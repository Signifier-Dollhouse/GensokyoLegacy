package dev.xkmc.gensokyolegacy.content.entity.misc.anchor;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorItem;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorThrow;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One bounded anchor: it flies out, comes home, or hangs and drags its owner. It is a plain
 * projectile, not a danmaku, so the server owns its position and it can stop flush on a block face.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public class AnchorHookEntity extends ThrowableProjectile implements ItemSupplier, IEntityAdditionalSpawnData {

	public enum Phase {
		FLY, RETURN, GRAPPLE, DRAG, HANG
	}

	private static final EntityDataAccessor<Byte> DATA_TYPE = SynchedEntityData.defineId(
			AnchorHookEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(
			AnchorHookEntity.class, EntityDataSerializers.BYTE);

	private static final double FLY_SPEED = 1.5;
	private static final double RETURN_SPEED = 1.5;
	private static final double RETURN_DIST = 1.5;
	private static final int RETURN_TIMEOUT = 60;
	private static final int GRAPPLE_TIME = 40;
	private static final int DRAG_TIME = 40;
	private static final double GRAPPLE_DIST = 2;
	private static final double GRAPPLE_SPEED = 1;
	private static final double HANG_DIST = 4;
	private static final double HANG_HOLD = 0.12;
	private static final double REGRAPPLE_DIST = 3;
	private static final double MAX_OWNER_DIST = 24;
	private static final double DRAG_DIST = 2;
	private static final double DRAG_SPEED = 1;
	private static final int LIFE = 200;
	private static final int CHAIN_SOUND_INTERVAL = 5;
	private static final ItemStack DIAMOND_PICKAXE = new ItemStack(Items.DIAMOND_PICKAXE);

	private float damage = 0;
	private int radius = 0;
	private Vec3 origin = Vec3.ZERO;
	private Vec3 flyDir = Vec3.ZERO;
	private int grapple = 0;
	private int returnTicks = 0;
	private boolean anchored = false;
	private UUID grabbedId;
	private LivingEntity grabbed;

	public AnchorHookEntity(EntityType<? extends AnchorHookEntity> type, Level level) {
		super(type, level);
	}

	public AnchorHookEntity(EntityType<? extends AnchorHookEntity> type, LivingEntity shooter, Level level) {
		super(type, shooter, level);
	}

	public void launch(AnchorThrow type, float damage, Vec3 origin, Vec3 dir) {
		this.damage = damage;
		this.origin = origin;
		this.flyDir = dir.normalize();
		entityData.set(DATA_TYPE, (byte) type.ordinal());
		setPhase(Phase.FLY);
		setPos(origin);
		shoot(flyDir.x, flyDir.y, flyDir.z, (float) FLY_SPEED, 0);
	}

	/**
	 * The blast radius of a charged destroy throw; only read by {@link AnchorThrow#DESTROY_CHARGED}.
	 */
	public void setRadius(int radius) {
		this.radius = Math.max(radius, 0);
	}

	public AnchorThrow throwType() {
		return AnchorThrow.values()[entityData.get(DATA_TYPE)];
	}

	public Phase phase() {
		return Phase.values()[entityData.get(DATA_PHASE)];
	}

	private void setPhase(Phase phase) {
		entityData.set(DATA_PHASE, (byte) phase.ordinal());
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(DATA_TYPE, (byte) 0);
		entityData.define(DATA_PHASE, (byte) 0);
	}

	@Override
	protected float getGravity() {
		return 0;
	}

	/**
	 * The plain iron anchor: a hook is never a storm throw, so it has no wet twin.
	 */
	@Override
	public ItemStack getItem() {
		return GLItems.MINAMITSU_ANCHOR.get().getDefaultInstance();
	}

	@Override
	public void tick() {
		steer();
		super.tick();
		if (level().isClientSide || isRemoved()) return;
		tickPhase();
	}

	private void steer() {
		switch (phase()) {
			case FLY -> setDeltaMovement(flyDir.scale(FLY_SPEED));
			case RETURN -> {
				Entity owner = getOwner();
				Vec3 diff = owner == null ? Vec3.ZERO : owner.getEyePosition().subtract(position());
				setDeltaMovement(diff.lengthSqr() < 1e-6 ? Vec3.ZERO : diff.normalize().scale(RETURN_SPEED));
			}
			case GRAPPLE, DRAG, HANG -> setDeltaMovement(Vec3.ZERO);
		}
	}

	/**
	 * The anchor keeps the heading it was thrown with. {@code Projectile.updateRotation} would turn
	 * it around on the return leg and while it drags, making the model look like it aims backwards.
	 */
	@Override
	protected void updateRotation() {
	}

	@Override
	protected boolean canHitEntity(Entity target) {
		return phase() == Phase.FLY && throwType() != AnchorThrow.PULL && super.canHitEntity(target);
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		if (level().isClientSide || phase() != Phase.FLY) return;
		setPos(hit.getLocation());
		setDeltaMovement(Vec3.ZERO);
		if (throwType().dealsDamage()) AnchorStorm.smash(level(), hit.getBlockPos());
		switch (throwType()) {
			case DESTROY -> toReturn();
			case DESTROY_CHARGED -> {
				destroyImpact(position());
				toReturn();
			}
			case PULL -> {
				anchored = true;
				enterGrapple();
			}
			case GRAB -> toReturn();
			default -> {
			}
		}
	}

	@Override
	public void onHitEntity(EntityHitResult result) {
		if (level().isClientSide) return;
		Entity target = unwrap(result.getEntity());
		if (throwType().dealsDamage()) AnchorStorm.smashAt(level(), target.position());
		switch (throwType()) {
			case DESTROY -> {
				hurtTarget(target);
				toReturn();
			}
			case DESTROY_CHARGED -> {
				destroyImpact(position());
				toReturn();
			}
			case GRAB -> {
				if (target instanceof LivingEntity le) enterDrag(le);
				else toReturn();
			}
			default -> {
			}
		}
	}

	private void tickPhase() {
		if (ownerGone() || ownerLost()) {
			discard();
			return;
		}
		if (tickCount > LIFE && phase() != Phase.HANG) {
			discard();
			return;
		}
		tickChainSound();
		switch (phase()) {
			case FLY -> {
				if (position().distanceTo(origin) > throwType().reach()) endFlight();
			}
			case RETURN -> tickReturn();
			case GRAPPLE -> tickGrapple();
			case DRAG -> tickDrag();
			case HANG -> tickHang();
		}
	}

	private boolean ownerGone() {
		return !(getOwner() instanceof LivingEntity owner) || owner.isRemoved() || owner.isDeadOrDying();
	}

	/**
	 * The link on the held anchor is the leash: too far away, another dimension, or a held anchor
	 * that does not carry this hook's UUID lets it go. Leaving the anchor in the inventory is fine.
	 */
	private boolean ownerLost() {
		if (!(getOwner() instanceof Player player)) return false;
		if (player.level() != level()) return true;
		if (player.distanceToSqr(this) > MAX_OWNER_DIST * MAX_OWNER_DIST) return true;
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		if (!(main.getItem() instanceof AnchorItem) && !(off.getItem() instanceof AnchorItem)) return false;
		return !linkMatches(main) && !linkMatches(off);
	}

	private boolean linkMatches(ItemStack stack) {
		return stack.getItem() instanceof AnchorItem && getUUID().equals(AnchorItem.getLink(stack));
	}

	/**
	 * The hook never follows its owner through a portal; {@link #ownerLost()} discards it instead.
	 */
	@Override
	public boolean canChangeDimensions() {
		return false;
	}

	@Nullable
	@Override
	public Entity changeDimension(ServerLevel level) {
		return null;
	}

	private void endFlight() {
		if (throwType() == AnchorThrow.PULL) enterGrapple();
		else toReturn();
	}

	private void toReturn() {
		setPhase(Phase.RETURN);
		returnTicks = 0;
		grabbedId = null;
		grabbed = null;
	}

	/**
	 * A pull that is over leaves the hook where it stopped instead of reeling it in: the player
	 * retrieves it with a right click, and it lives until then rather than hitting the lifetime cap.
	 */
	private void toHang() {
		setPhase(Phase.HANG);
		setDeltaMovement(Vec3.ZERO);
	}

	/**
	 * A right click on the linked stack: out of reach of a stuck anchor it reels the owner back in,
	 * otherwise it sends the hook home.
	 */
	public void onRetrieve(Player player) {
		if (phase() == Phase.HANG && distanceToSqr(player) > REGRAPPLE_DIST * REGRAPPLE_DIST) {
			enterGrapple();
		} else if (phase() != Phase.RETURN) {
			toReturn();
		}
	}

	private void enterGrapple() {
		setPhase(Phase.GRAPPLE);
		grapple = GRAPPLE_TIME;
		setDeltaMovement(Vec3.ZERO);
	}

	private void enterDrag(LivingEntity target) {
		setPhase(Phase.DRAG);
		grapple = DRAG_TIME;
		grabbed = target;
		grabbedId = target.getUUID();
		setDeltaMovement(Vec3.ZERO);
	}

	private void tickReturn() {
		returnTicks++;
		Entity owner = getOwner();
		if (owner == null || returnTicks > RETURN_TIMEOUT ||
				position().distanceTo(owner.getEyePosition()) < RETURN_DIST) {
			discard();
		}
	}

	private void tickGrapple() {
		grapple--;
		if (!(getOwner() instanceof Player player) || grapple <= 0 || player.isShiftKeyDown()) {
			endPull();
			return;
		}
		Vec3 diff = position().subtract(player.position());
		if (diff.lengthSqr() < GRAPPLE_DIST * GRAPPLE_DIST) {
			endPull();
			return;
		}
		player.setDeltaMovement(diff.normalize().scale(GRAPPLE_SPEED));
		player.hurtMarked = true;
		player.fallDistance = 0;
	}

	/**
	 * Only a block hit leaves the anchor behind; a throw that ran out of reach in open air returns
	 * as it always did.
	 */
	private void endPull() {
		if (anchored) toHang();
		else toReturn();
	}

	/**
	 * Holds the owner at the stuck anchor: a spring toward it counters the drop without ever taking
	 * gravity away, so a lost hook can only drop the player, never strand them.
	 */
	private void tickHang() {
		if (!(getOwner() instanceof Player player) || player.isShiftKeyDown()) return;
		if (player.onGround() || player.getAbilities().flying || player.isPassenger()) return;
		Vec3 diff = position().subtract(player.position());
		if (diff.lengthSqr() > HANG_DIST * HANG_DIST) return;
		player.setDeltaMovement(diff.scale(HANG_HOLD));
		player.hurtMarked = true;
		player.fallDistance = 0;
	}

	/**
	 * The grabbed entity is dragged every tick, not nudged once, and the anchor rides along on it so
	 * the chain stays attached for the whole pull.
	 */
	private void tickDrag() {
		grapple--;
		LivingEntity target = grabbedTarget();
		Entity owner = getOwner();
		if (target == null || !target.isAlive() || owner == null) {
			toReturn();
			return;
		}
		setPos(target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ());
		Vec3 diff = owner.position().subtract(target.position());
		if (diff.lengthSqr() < DRAG_DIST * DRAG_DIST || grapple <= 0) {
			toReturn();
			return;
		}
		target.setDeltaMovement(diff.normalize().scale(DRAG_SPEED));
		target.hurtMarked = true;
		target.fallDistance = 0;
	}

	private LivingEntity grabbedTarget() {
		if (grabbed != null && grabbed.isAlive()) return grabbed;
		if (grabbedId == null) return null;
		if (level() instanceof ServerLevel sl && sl.getEntity(grabbedId) instanceof LivingEntity le) {
			grabbed = le;
			return le;
		}
		return null;
	}

	private static Entity unwrap(Entity entity) {
		while (entity instanceof PartEntity<?> part) entity = part.getParent();
		return entity;
	}

	private void hurtTarget(Entity target) {
		Entity owner = getOwner();
		if (!(target instanceof LivingEntity le)) return;
		if (!IDanmakuEntity.canHurt(owner, le)) return;
		DamageSource source = damageSources().thrown(this, owner);
		if (le.hurt(source, damage)) {
			Vec3 dir = le.position().subtract(position());
			if (dir.lengthSqr() > 1e-6) {
				dir = dir.normalize();
				le.knockback(0.3, dir.x, dir.z);
			}
		}
	}

	/**
	 * The charged destroy throw: one blast that both excavates and damages around the impact.
	 */
	private void destroyImpact(Vec3 at) {
		int r = Math.max(radius, 1);
		breakBlocks(BlockPos.containing(at), r);
		AnchorStorm.areaDamage(this, getOwner(), at, r, damage);
	}

	private void breakBlocks(BlockPos center, int radius) {
		Entity owner = getOwner();
		Player player = owner instanceof Player p ? p : null;
		boolean drop = player == null || !player.isCreative();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
				center.offset(radius, radius, radius))) {
			if (radius > 1 && center.distSqr(pos) > radius * radius) continue;
			BlockState state = level().getBlockState(pos);
			if (state.isAir()) continue;
			if (state.getDestroySpeed(level(), pos) < 0) continue;
			if (state.requiresCorrectToolForDrops() && !DIAMOND_PICKAXE.isCorrectToolForDrops(state)) continue;
			if (player != null) {
				if (!level().mayInteract(player, pos)) continue;
				if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level(), pos, state, player)))
					continue;
			}
			level().destroyBlock(pos, drop, owner);
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putByte("AnchorType", (byte) throwType().ordinal());
		tag.putByte("AnchorPhase", (byte) phase().ordinal());
		tag.putFloat("AnchorDamage", damage);
		tag.putInt("AnchorRadius", radius);
		tag.putInt("AnchorGrapple", grapple);
		tag.putInt("AnchorReturn", returnTicks);
		tag.putBoolean("AnchorAnchored", anchored);
		tag.putDouble("AnchorOriginX", origin.x);
		tag.putDouble("AnchorOriginY", origin.y);
		tag.putDouble("AnchorOriginZ", origin.z);
		tag.putDouble("AnchorFlyX", flyDir.x);
		tag.putDouble("AnchorFlyY", flyDir.y);
		tag.putDouble("AnchorFlyZ", flyDir.z);
		if (grabbedId != null) tag.putUUID("AnchorGrab", grabbedId);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(DATA_TYPE, tag.getByte("AnchorType"));
		entityData.set(DATA_PHASE, tag.getByte("AnchorPhase"));
		damage = tag.getFloat("AnchorDamage");
		radius = tag.getInt("AnchorRadius");
		grapple = tag.getInt("AnchorGrapple");
		returnTicks = tag.getInt("AnchorReturn");
		anchored = tag.getBoolean("AnchorAnchored");
		origin = new Vec3(tag.getDouble("AnchorOriginX"), tag.getDouble("AnchorOriginY"),
				tag.getDouble("AnchorOriginZ"));
		flyDir = new Vec3(tag.getDouble("AnchorFlyX"), tag.getDouble("AnchorFlyY"),
				tag.getDouble("AnchorFlyZ"));
		grabbedId = tag.hasUUID("AnchorGrab") ? tag.getUUID("AnchorGrab") : null;
	}

	/**
	 * The client never receives the spawn velocity, and {@code Projectile} stores yaw as
	 * {@code atan2(dx, dz)}, so the launch vector has to travel separately or the hook drives
	 * itself in the wrong heading.
	 */
	@Override
	public void writeSpawnData(FriendlyByteBuf buffer) {
		buffer.writeDouble(flyDir.x);
		buffer.writeDouble(flyDir.y);
		buffer.writeDouble(flyDir.z);
	}

	@Override
	public void readSpawnData(FriendlyByteBuf buffer) {
		flyDir = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
	}

	/**
	 * The chain rattles while it pays out, reels in or pulls the owner; a five-tick interval keeps
	 * it audible without turning the flight into a buzz.
	 */
	private void tickChainSound() {
		if (tickCount % CHAIN_SOUND_INTERVAL != 0) return;
		switch (phase()) {
			case FLY, RETURN, GRAPPLE -> playSound(SoundEvents.CHAIN_PLACE, 0.6F);
			default -> {
			}
		}
	}

	private void playSound(SoundEvent sound, float volume) {
		level().playSound(null, this, sound, SoundSource.PLAYERS, volume, 1F);
	}

}
