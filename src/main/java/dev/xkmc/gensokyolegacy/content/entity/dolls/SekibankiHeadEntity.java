package dev.xkmc.gensokyolegacy.content.entity.dolls;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollNature;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.*;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * A disembodied Sekibanki head, bobbing along as the owner's first gadget: it pairs, follows and
 * itemizes like any doll, and stops there. No loadout, no commands, no attack — it extends
 * {@link BaseDollEntity} rather than {@link DollEntity} precisely so that none of those exist.
 * <p>
 * The one thing it does beyond that is let its owner look out through it: shift and right click
 * hands the camera over, and the mouse and the movement keys then fly the head instead of the body,
 * leashed to {@link #REACH} blocks from the player it left behind. The body keeps its own hands
 * to itself while the camera is away — it neither attacks nor uses items, and the head only ever
 * works a block with an empty hand.
 *
 * @see "doc/design/doll/gadget.md"
 */
public class SekibankiHeadEntity extends BaseDollEntity {

	/**
	 * The gadget kind's entity type, and the entity kind its item materializes.
	 */
	public static final ResourceLocation TYPE = GensokyoLegacy.loc("sekibanki_head");

	/**
	 * Furthest the head ever gets from the player driving it, in blocks.
	 */
	public static final double REACH = 48;

	/**
	 * Creative-flight speed while possessed, and the same doubled while sprinting.
	 */
	private static final double FLY_SPEED = 0.25;

	/**
	 * The most the head may travel in one frame, and so the rate it walks home at once it is past
	 * {@link #REACH}. Its own flight stays at {@link #MAX_SPEED} or under, so this never bites in
	 * play: it exists for the two frames that are not flight, a leash catching up and a client
	 * claiming more than it flew.
	 */
	private static final double MAX_FRAME = 1.0;

	/**
	 * How far the head's own crosshair reaches, matching the vanilla block interaction range.
	 */
	private static final double USE_RANGE = 4.5;

	/**
	 * How far off its player the head is left stranded once possession ends, in blocks.
	 */
	private static final double RETURN_DISTANCE = 8;

	/**
	 * How fast it comes back, in blocks per tick: between a mob's walk and its own flight.
	 */
	private static final double RETURN_SPEED = 0.75;

	private static final EntityDataAccessor<Optional<UUID>> POSSESSOR =
			SynchedEntityData.defineId(SekibankiHeadEntity.class, EntityDataSerializers.OPTIONAL_UUID);

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("sekibanki_idle");
	private static final RawAnimation FLY = RawAnimation.begin().thenLoop("sekibanki_fly");
	private static final RawAnimation BLINK = RawAnimation.begin().thenLoop("sekibanki_blink");
	private static final RawAnimation SHOUT = RawAnimation.begin().thenPlay("sekibanki_shout");

	/**
	 * The last movement frame the possessor's client produced, in the client's own input frame.
	 * Client-side only: the server takes the finished frame as a position, not as keys.
	 */
	private float moveStrafe;
	private float moveForward;
	private boolean moveUp;
	private boolean moveDown;
	private boolean moveSprint;

	public SekibankiHeadEntity(EntityType<? extends BaseDollEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public DollNature nature() {
		return DollNature.ITEM_GADGET;
	}

	/**
	 * Nothing can break this head. The vanilla gate answers before the doll pipeline writes to the
	 * ledger at all, so it is not a doll that heals back to full — it is one whose entry never
	 * loses health, never reaches zero, and so never parks as a broken item.
	 */
	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return true;
	}

	/**
	 * The same three controllers as the plain doll, pointed at this head's clips. The one-shot is a
	 * plain controller rather than {@code DollOneShotController}: that one flashes a lance particle
	 * at an impact point, and this head has neither a lance nor a way of marking one.
	 */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, WINK_CONTROLLER, 0, e -> e.setAndContinue(BLINK)));
		controllers.add(new AnimationController<>(this, ANIM_CONTROLLER, ANIM_TRANSITION,
				e -> e.setAndContinue(e.isMoving() ? FLY : IDLE)));
		controllers.add(new AnimationController<>(this, ONE_SHOT_CONTROLLER, ANIM_TRANSITION,
				this::oneShotAnimController).receiveTriggeredAnimations().triggerableAnim("shout", SHOUT));
	}

	/**
	 * Let the shout that fired run to its end, and otherwise write nothing at all.
	 */
	private PlayState oneShotAnimController(AnimationState<SekibankiHeadEntity> event) {
		return event.getController().isPlayingTriggeredAnimation() ? PlayState.CONTINUE : PlayState.STOP;
	}

	/**
	 * Both swing ids answer with the one clip this rig has. Nothing can order the head to swing —
	 * it is not a {@link DollEntity} — so this is a guard, not a live path: an id arriving anyway
	 * reaches a {@code sekibanki_*} clip instead of a {@code toy_*} one it does not have.
	 */
	@Override
	public void handleEntityEvent(byte id) {
		if (level().isClientSide() && (id == EVENT_ATTACK || id == EVENT_SLAP)) {
			triggerAnim(ONE_SHOT_CONTROLLER, "shout");
			return;
		}
		super.handleEntityEvent(id);
	}

	@Override
	public void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(POSSESSOR, Optional.empty());
	}

	// ---- possession: the owner's eyes, hands and movement keys, for a spell ----

	/**
	 * Whether this head is the one {@code player} is currently looking out of.
	 */
	public boolean isPossessedBy(Player player) {
		return player.getUUID().equals(getEntityData().get(POSSESSOR).orElse(null));
	}

	/**
	 * Hands the flight to whoever is looking out of this head, and to nobody else. This is what
	 * makes the head client-authoritative the way a boat is: the possessor's client then stops
	 * applying the server's position and rotation packets to it, so its copy of the mouse survives
	 * instead of being lerped back to whatever the server last echoed. Every other client keeps
	 * following normally and sees the head fly as an ordinary tracked entity.
	 */
	@Override
	public boolean isControlledByLocalInstance() {
		return localPossessor() != null || super.isControlledByLocalInstance();
	}

	/**
	 * Where this head looks, while this head is the camera. Deferred to the player outright rather
	 * than read off the head's own rotation, and this is the whole of the "the mouse does not turn
	 * the camera" bug: {@code Entity} answers with an interpolation between its last two ticks,
	 * while the head only records a heading once a tick, so the camera would advance in 20 steps a
	 * second behind a mouse that turns every frame. {@code LocalPlayer} overrides these two for
	 * exactly this reason, and every consumer funnels through them — {@code Camera#setup} for the
	 * view, and {@code getViewVector} (final, and built on these) for the crosshair.
	 */
	@Override
	public float getViewYRot(float partialTick) {
		Player local = localPossessor();
		return local != null ? local.getYRot() : super.getViewYRot(partialTick);
	}

	@Override
	public float getViewXRot(float partialTick) {
		Player local = localPossessor();
		return local != null ? local.getXRot() : super.getViewXRot(partialTick);
	}

	/**
	 * The player whose camera this is, when that player is the one running this code.
	 */
	@Nullable
	private Player localPossessor() {
		if (!level().isClientSide) return null;
		return possessorEntity() instanceof Player player && player.isLocalPlayer() ? player : null;
	}

	/**
	 * The player whose camera this is, resolved wherever it lives; null once it is gone. Read on
	 * both sides and deliberately not for the leash: the leash is the server's call alone.
	 */
	@Nullable
	private LivingEntity possessorEntity() {
		UUID id = getEntityData().get(POSSESSOR).orElse(null);
		if (id == null) return null;
		Entity found = level() instanceof ServerLevel sl ? sl.getEntity(id) : level().getPlayerByUUID(id);
		return found instanceof LivingEntity living ? living : null;
	}

	/**
	 * One possessor at a time: taking the eyes from a player gives them back first.
	 */
	public void possess(ServerPlayer player) {
		if (isPossessedBy(player)) return;
		release();
		getEntityData().set(POSSESSOR, Optional.of(player.getUUID()));
		player.connection.send(new ClientboundSetCameraPacket(this));
	}

	/**
	 * Puts the player back in their own head and stops answering for it.
	 */
	public void release() {
		if (getEntityData().get(POSSESSOR).isEmpty()) return;
		LivingEntity holder = possessorEntity();
		getEntityData().set(POSSESSOR, Optional.empty());
		if (holder instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetCameraPacket(sp));
	}

	/**
	 * The frame the owning client flew this tick. The client runs the model and reports where it
	 * ended up rather than which keys were held, so the two sides can never disagree about the
	 * flight itself; the leash below is the one correction either side is allowed to make.
	 */
	public void acceptMove(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
		if (!isPossessedBy(player)) return;
		LivingEntity holder = possessorEntity();
		Vec3 wanted = leash(holder == null ? position().add(x, y, z) : holder.position(), position(), new Vec3(x, y, z));
		setDeltaMovement(wanted.subtract(position()));
		absMoveTo(wanted.x, wanted.y, wanted.z, yaw, Mth.clamp(pitch, -90.0F, 90.0F));
	}

	/**
	 * Holds the head on the {@link #REACH} sphere around its player. Inside the limit an overshoot
	 * is trimmed back to the surface, so pushing at the leash slides along it rather than stopping
	 * dead; past it the head is walked home {@link #MAX_FRAME} at a time, which is what keeps a
	 * player who respawned across the map from dragging it through the world in a single tick.
	 * <p>
	 * Shared by both sides on purpose: the owning client runs it before it reports a frame and the
	 * server runs it again on the report, so the two agree on the leash, and the server's pass is
	 * also what refuses a frame no flight of this head could have produced.
	 */
	private static Vec3 leash(Vec3 holder, Vec3 from, Vec3 wanted) {
		Vec3 out = wanted.subtract(holder);
		double far = out.length();
		Vec3 home = far > REACH && far > 1.0E-4 ? holder.add(out.scale(REACH / far)) : wanted;
		Vec3 step = home.subtract(from);
		double len = step.length();
		return len > MAX_FRAME ? from.add(step.scale(MAX_FRAME / len)) : home;
	}

	/**
	 * Records the frame the owning client wants flown this tick, and its own idea of where the
	 * camera is pointing. Silent for a head nobody is driving, so a stale client that kept
	 * sending after being dropped costs nothing.
	 */
	public void applyMove(float yaw, float pitch, float strafe, float forward, boolean up, boolean down, boolean sprint) {
		if (getEntityData().get(POSSESSOR).isEmpty()) return;
		setYRot(yaw);
		setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
		moveStrafe = strafe;
		moveForward = forward;
		moveUp = up;
		moveDown = down;
		moveSprint = sprint;
	}

	/**
	 * Shift and right click is the toggle: the plain click still itemizes the head, and nothing
	 * else may steer it.
	 */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!player.isShiftKeyDown()) return super.mobInteract(player, hand);
		if (!isOwner(player) && !player.getAbilities().instabuild) return super.mobInteract(player, hand);
		if (level().isClientSide()) return InteractionResult.SUCCESS;
		if (isPossessedBy(player)) release();
		else possess((ServerPlayer) player);
		return InteractionResult.CONSUME;
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		release();
		super.remove(reason);
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide && getEntityData().get(POSSESSOR).isPresent()
				&& (isRemoved() || possessorEntity() == null)) {
			release();
		}
	}

	/**
	 * The goals give up the head twice: while a player is driving it, whose input owns its heading
	 * instead, and while it is flying itself home (§5.5). The second test has to be kept alongside
	 * the first rather than folded into {@link #flyingHome}, which is false precisely when the head
	 * is possessed.
	 */
	@Override
	public boolean followsOwner() {
		return getEntityData().get(POSSESSOR).isEmpty() && !flyingHome();
	}

	/**
	 * Creative flight for the owning client, in place of the follow path: the input vector is
	 * taken whole rather than nudged, so stopping is stopping and there is no ground friction to
	 * fight. Only that client runs it — {@link #isControlledByLocalInstance} stops the server's
	 * copies of this head from reaching the other copies at all — so the frame it produces is
	 * reported back whole, leash included, by {@link #acceptMove}. Once possession is over the
	 * head is an ordinary tracked entity again and flies home to its player instead (§5.5).
	 */
	@Override
	public void travel(Vec3 travelVector) {
		if (getEntityData().get(POSSESSOR).isPresent()) {
			if (localPossessor() == null) return;
			Vec3 input = new Vec3(moveStrafe, (moveUp ? 1 : 0) - (moveDown ? 1 : 0), moveForward);
			Vec3 motion = heading(input, moveSprint ? MAX_SPEED : FLY_SPEED);
			LivingEntity holder = possessorEntity();
			// the leash is expressed as a destination, the same way acceptMove reads it, so the
			// frame this produces is already the frame the server will accept
			if (holder != null)
				motion = leash(holder.position(), position(), position().add(motion)).subtract(position());
			move(MoverType.SELF, motion);
			setDeltaMovement(motion);
			return;
		}
		if (!level().isClientSide && flyingHome()) {
			flyHome();
			return;
		}
		super.travel(travelVector);
	}

	/**
	 * Whether the head is far enough from its player, with a clear way back, that it flies itself
	 * home instead of leaving that to the follow goal (gadget.md §5.5). A possessed head may be
	 * {@link #REACH} blocks out; the goal would path it back, but only at the speed a mob has, and
	 * that is not a pace a possessed head reads as coming home. The line of sight test is what
	 * keeps this to the open case: with terrain in the way the goal is left to path around it,
	 * because a straight line at a wall would only jam the head against it.
	 */
	private boolean flyingHome() {
		if (getEntityData().get(POSSESSOR).isPresent()) return false;
		LivingEntity owner = getOwner();
		return owner != null && owner.distanceToSqr(this) > RETURN_DISTANCE * RETURN_DISTANCE
				&& hasLineOfSight(owner);
	}

	/**
	 * Straight at the player, at a clip between a walk and the flight itself. Server-side only: the
	 * head is an ordinary tracked entity again the moment possession ends, so putting this on both
	 * sides would have them pull it apart.
	 */
	private void flyHome() {
		LivingEntity owner = getOwner();
		if (owner == null) return;
		Vec3 to = owner.position().subtract(position());
		double len = to.length();
		if (len < 1.0E-4) return;
		setYRot((float) (-Mth.atan2(to.x, to.z) * 180.0 / Math.PI));
		Vec3 step = leash(owner.position(), position(),
				position().add(to.scale(Math.min(RETURN_SPEED, len) / len))).subtract(position());
		move(MoverType.SELF, step);
		setDeltaMovement(step);
	}

	/**
	 * Turns an input vector into world motion the way {@code moveRelative} does, diagonal and all,
	 * but returns the frame instead of folding it into a running total a flight has no use for.
	 */
	private Vec3 heading(Vec3 input, double speed) {
		Vec3 scaled = (input.lengthSqr() > 1.0 ? input.normalize() : input).scale(speed);
		double yaw = Math.toRadians(getYRot());
		double sin = Math.sin(yaw);
		double cos = Math.cos(yaw);
		return new Vec3(scaled.x * cos - scaled.z * sin, scaled.y, scaled.z * cos + scaled.x * sin);
	}

	/**
	 * The head's own right click on a block. Deliberately the empty hand half of a block's
	 * interaction: a door opens and a chest fills without the body behind it swinging whatever it
	 * happens to be holding. The crosshair is re-traced here because the client sent a position,
	 * not a click, and the head may have moved since it drew one.
	 */
	public void useBlock(ServerPlayer player, BlockHitResult hit) {
		if (!isPossessedBy(player)) return;
		Level level = level();
		BlockPos pos = hit.getBlockPos();
		if (level.getBlockState(pos).isAir() || !level.isLoaded(pos)) return;
		Vec3 eye = getEyePosition();
		if (eye.distanceToSqr(Vec3.atCenterOf(pos)) > USE_RANGE * USE_RANGE) return;
		if (!level.clip(new ClipContext(eye, Vec3.atCenterOf(pos), ClipContext.Block.OUTLINE,
				ClipContext.Fluid.NONE, this)).getBlockPos().equals(pos)) return;
		if (!level.mayInteract(player, pos)) return;
		BlockState state = level.getBlockState(pos);
		InteractionResult result = state.useWithoutItem(level, player, hit);
		if (result.shouldSwing()) player.swing(InteractionHand.MAIN_HAND, true);
		player.connection.send(new ClientboundBlockUpdatePacket(level, pos));
		player.connection.send(new ClientboundBlockUpdatePacket(level, pos.relative(hit.getDirection())));
	}

}