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

	/** Per-tick speed gain at full input, in blocks per tick squared. */
	private static final double THRUST = 0.05;
	private static final double STRAFE = 0.04;
	private static final double LIFT = 0.05;
	/** Speed caps; 1.0 b/t is 20 blocks per second, about a sprinting horse. */
	private static final double MAX_SPEED = 1.0;
	private static final double MAX_RISE = 0.8;
	/** Speed retained per tick once the rider lets go of the keys. */
	private static final double COAST = 0.85;

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
		// Authority split as on Boat/Minecart: the riding client owns the flight
		// model and reports the result in ServerboundMoveVehiclePacket, so the
		// server must not simulate or it fights that packet. Other clients only
		// receive position updates; base Entity#lerpTo snaps, as it does for boats.
		if (!isControlledByLocalInstance()) return;
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
	 * With no keys held the broom coasts down instead of stopping dead.
	 */
	private Vec3 thrust(Player rider) {
		// vanilla riding input: zza is forward/back, xxa is left/right, and jump and
		// shift are still live for a passenger. Sneak normally dismounts, which
		// PlayerMixin opts this vehicle out of.
		double ahead = rider.zza;
		double aside = rider.xxa;
		double up = (isJumping(rider) ? 1 : 0) - (rider.isShiftKeyDown() ? 1 : 0);
		if (ahead == 0 && aside == 0 && up == 0) return getDeltaMovement().scale(COAST);
		Vec3 motion = getDeltaMovement()
				.add(getLookAngle().scale(ahead * THRUST))
				.add(Vec3.directionFromRotation(0, getYRot() - 90).scale(aside * STRAFE))
				.add(0, up * LIFT, 0);
		double flat = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
		if (flat > MAX_SPEED) {
			double shrink = MAX_SPEED / flat;
			motion = new Vec3(motion.x * shrink, motion.y, motion.z * shrink);
		}
		return motion.with(Direction.Axis.Y, Mth.clamp(motion.y, -MAX_RISE, MAX_RISE));
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
	 * the resulting position in {@code ServerboundMoveVehiclePacket}.
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