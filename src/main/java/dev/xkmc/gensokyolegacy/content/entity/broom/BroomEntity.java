package dev.xkmc.gensokyolegacy.content.entity.broom;

import dev.xkmc.fastprojectileapi.entity.SimplifiedEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.mixin.LivingEntityJumpingAccessor;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A rideable magic broom: a free-flight vehicle steered by its rider.
 *
 * <p>The rider's yaw and pitch are mirrored onto the entity every tick, so
 * looking around aims the broom and nothing else rotates it. The vanilla
 * movement keys cover the flight axes — forward/back thrust along the view,
 * left/right strafe along the horizontal facing, jump/shift lift and drop.
 *
 * <p>The entity is only a stand-in for the item: it lives exactly as long as
 * some rider keeps a broom in either hand, and is discarded the tick that stops
 * being true (see {@link BroomItem}, which is what creates it).
 */
public class BroomEntity extends SimplifiedEntity implements GeoEntity {

	/**
	 * Seat offset in blocks: on top of the shaft (model y ~11) and a little ahead
	 * of the midpoint, so the bristle bundle trails behind the rider. Rotated by
	 * the entity yaw only — the rider stays upright while the broom pitches.
	 */
	private static final double SEAT_UP = 0.7;
	private static final double SEAT_FORWARD = 0.4;

	/**
	 * A Multi Fakkero in the rider's other hand adds thrust on top of the keys for a while.
	 * Held as a timer rather than a decaying force so the client, which owns the flight
	 * model, is the only side that counts it down and the two can never disagree about how
	 * much is left.
	 */
	@Nullable
	private Boost boost;

	private record Boost(float power, int ticks) {
	}

	/** Per-tick speed gain at full input, in blocks per tick squared. */
	private static final double THRUST = 0.05;
	private static final double STRAFE = 0.04;
	private static final double LIFT = 0.05;
	/**
	 * Multiplier on the whole input vector when it pushes against the motion it
	 * is applied to — holding back into a glide, or swinging the nose around to
	 * reverse. Braking that is no stronger than accelerating makes a broom coming
	 * at you take as long to turn around as it did to speed up from a standstill,
	 * which is where the "mushy" feel came from.
	 */
	private static final double BRAKE = 2;
	/** Speed caps; 1.0 b/t is 20 blocks per second, about a sprinting horse. */
	private static final double MAX_SPEED = 1.0;
	private static final double MAX_RISE = 0.8;
	/**
	 * Speed retained per tick once the rider lets go of the keys, per axis of the
	 * rider's horizontal facing — see {@link #coast()} for why it is split.
	 * Along the facing, friction is low so a released forward key glides instead of
	 * stopping dead; across it, friction is high so a facing that is never
	 * corrected bleeds off instead of sliding sideways forever.
	 */
	private static final double COAST_FWD = 0.95;
	private static final double COAST_SIDE = 0.85;

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("broom_idle");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public BroomEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public void tick() {
		baseTick();
		Player rider = getRider();
		if (rider == null || !holdsBroom(rider)) {
			discard();
			return;
		}
		// A rider is not falling. Vanilla charges fall damage off the distance
		// accumulated while moving downward without ground underfoot, and this thing
		// drops at up to MAX_RISE a tick — so a ride left a bill that came due the
		// moment the feet touched down, or the moment the broom was discarded for no
		// longer being held (BroomItem). Cleared on both sides: the server owns the
		// damage and the client owns the prediction, and neither should see a rider
		// banking one. Zeroing rather than clamping, so a broom that dives from height
		// dismounts clean.
		rider.resetFallDistance();
		// Position is client-authoritative, as on Boat/Minecart: the riding client
		// runs the flight model and reports the result in ServerboundMoveVehiclePacket,
		// and the server just applies it. Orientation is NOT handled here — the
		// renderer reads the rider's view directly (see BroomRenderer), because a
		// broom has no steering of its own the way a boat does, and copying the
		// rider's yaw into the entity made the server and the packet fight over it.
		if (!level().isClientSide || !isControlledByLocalInstance()) {
			setDeltaMovement(Vec3.ZERO);
			return;
		}
		// keep the entity's own facing in step anyway, so the hitbox and any vanilla
		// yaw-dependent logic (e.g. seat placement) still line up with the model
		setYRot(rider.getYRot());
		setXRot(rider.getXRot());
		setDeltaMovement(thrust(rider));
		move(MoverType.SELF, getDeltaMovement());
	}

	@Override
	public boolean isNoGravity() {
		// a broom holds altitude on its own; only LIFT moves it vertically
		return true;
	}

	/**
	 * Accelerates along the rider's view, then caps the result per axis group.
	 * Input aimed against the current motion is boosted by {@link #BRAKE}. With no
	 * keys held the broom coasts down instead of stopping dead.
	 */
	private Vec3 thrust(Player rider) {
		// vanilla riding input: zza is forward/back, xxa is left/right, and jump and
		// shift are still live for a passenger. Sneak normally dismounts, which
		// PlayerMixin opts this vehicle out of.
		double ahead = rider.zza;
		double aside = rider.xxa;
		double up = (isJumping(rider) ? 1 : 0) - (rider.isShiftKeyDown() ? 1 : 0);
		// A boost acts as if the forward key were held, so it still needs to be cancelled
		// by actually pushing back — otherwise a rider could never slow down while it ran.
		if (boost != null && ahead < 0) boost = null;
		float power = boostPower();
		if (power > 0) ahead = Math.max(ahead, 0) + power;
		if (ahead == 0 && aside == 0 && up == 0) return coast();
		Vec3 motion = getDeltaMovement();
		Vec3 push = getLookAngle().scale(ahead * THRUST)
				.add(Vec3.directionFromRotation(0, getYRot() - 90).scale(aside * STRAFE))
				.add(0, up * LIFT, 0);
		// pushing into the current motion, i.e. braking or reversing: bite harder,
		// so turning the broom around does not take as long as winding it up
		if (push.dot(motion) < 0) push = push.scale(BRAKE);
		motion = motion.add(push);
		double flat = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
		// a boosted broom is allowed past the normal cap, or the extra thrust would only
		// buy it the first few ticks before friction pinned it at the usual ceiling
		double cap = MAX_SPEED * Math.max(1.0F, power);
		if (flat > cap) {
			double shrink = cap / flat;
			motion = new Vec3(motion.x * shrink, motion.y, motion.z * shrink);
		}
		return motion.with(Direction.Axis.Y, Mth.clamp(motion.y, -MAX_RISE, MAX_RISE));
	}

	/** The boost's share of the forward input, or 0 when there is none left. */
	private float boostPower() {
		if (boost == null) return 0;
		if (boost.ticks <= 1) {
			boost = null;
			return 0;
		}
		boost = new Boost(boost.power, boost.ticks - 1);
		return boost.power;
	}

	/**
	 * Adds thrust for {@code ticks} ticks. Sent by the server on the hakkero's behalf and
	 * applied here on the client, which is where the flight model actually runs.
	 */
	public void boost(float power, int ticks) {
		Boost next = new Boost(power, ticks);
		// a fresh, stronger kick replaces a weaker one still running
		boost = boost == null || boost.power <= power ? next : boost;
	}

	/**
	 * Idle decay, split along and across the rider's horizontal facing: the
	 * motion is projected onto the facing, the parallel part is scaled by
	 * {@link #COAST_FWD} and everything left over — the perpendicular part, which
	 * is also where pure climb and drop lands, since the facing is level — by
	 * {@link #COAST_SIDE}. Scaling the whole vector uniformly instead is what made
	 * the broom feel wrong to steer: momentum could never be spent, because the
	 * speed the thrust built was thrown away the instant the key came up, and a
	 * sideways shove kept the broom travelling sideways long after the rider had
	 * turned to face where they were going.
	 */
	private Vec3 coast() {
		Vec3 facing = Vec3.directionFromRotation(0, getYRot());
		Vec3 motion = getDeltaMovement();
		double ahead = motion.dot(facing);
		Vec3 across = motion.subtract(facing.scale(ahead));
		return facing.scale(ahead * COAST_FWD).add(across.scale(COAST_SIDE));
	}

	/** Jump key, read through the mixin: {@code jumping} has no public getter. */
	private static boolean isJumping(Player rider) {
		return ((LivingEntityJumpingAccessor) rider).gensokyolegacy$isJumping();
	}

	@Nullable
	public Player getRider() {
		return getFirstPassenger() instanceof Player player ? player : null;
	}

	/**
	 * Hands the controls to whoever is sitting on the front of the shaft. This is
	 * what makes the vehicle client-authoritative the same way a boat is: the
	 * riding client ticks it via {@link #isControlledByLocalInstance()} and reports
	 * the resulting position in {@code ServerboundMoveVehiclePacket}, which
	 * {@code handleMoveVehicle} accepts only when this returns that player.
	 */
	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return getRider();
	}

	@Override
	public Vec3 getPassengerRidingPosition(Entity passenger) {
		return position().add(Vec3.directionFromRotation(0, getYRot()).scale(SEAT_FORWARD)).add(0, SEAT_UP, 0);
	}

	/**
	 * Right-click the broom itself to get off. Sneak is taken by the descend
	 * key, so it cannot double as the dismount.
	 */
	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (!level().isClientSide && getRider() == player) player.stopRiding();
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	/** A broom is a tool, not a creature: it has no health and cannot be hit. */
	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return true;
	}

	private static boolean holdsBroom(Player player) {
		return player.getMainHandItem().is(GLItems.BROOM.get()) || player.getOffhandItem().is(GLItems.BROOM.get());
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "all", 0, state -> state.setAndContinue(IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}

}