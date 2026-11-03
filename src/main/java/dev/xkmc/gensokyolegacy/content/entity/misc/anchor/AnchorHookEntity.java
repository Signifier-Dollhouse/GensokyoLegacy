package dev.xkmc.gensokyolegacy.content.entity.misc.anchor;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorThrow;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * One bounded anchor: it flies out, comes home, or hangs and drags its owner. It is a plain
 * projectile, not a danmaku, so the server owns its position and it can stop flush on a block face.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public class AnchorHookEntity extends ThrowableProjectile implements ItemSupplier {

	public enum Phase {
		FLY, RETURN, GRAPPLE
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
	private static final double GRAPPLE_DIST = 2;
	private static final double GRAPPLE_SPEED = 1;
	private static final int LIFE = 200;
	private static final ItemStack DIAMOND_PICKAXE = new ItemStack(Items.DIAMOND_PICKAXE);

	private float damage = 0;
	private Vec3 origin = Vec3.ZERO;
	private int grapple = 0;
	private int returnTicks = 0;
	private int trail = 0;

	public AnchorHookEntity(EntityType<? extends AnchorHookEntity> type, Level level) {
		super(type, level);
	}

	public AnchorHookEntity(EntityType<? extends AnchorHookEntity> type, LivingEntity shooter, Level level) {
		super(type, shooter, level);
	}

	public void launch(AnchorThrow type, float damage, Vec3 origin, Vec3 dir) {
		this.damage = damage;
		this.origin = origin;
		entityData.set(DATA_TYPE, (byte) type.ordinal());
		setPhase(Phase.FLY);
		setPos(origin);
		shoot(dir.x, dir.y, dir.z, (float) FLY_SPEED, 0);
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
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_TYPE, (byte) 0);
		builder.define(DATA_PHASE, (byte) 0);
	}

	@Override
	protected double getDefaultGravity() {
		return 0;
	}

	/**
	 * The water-anchor twin while a storm throw is wet; the stack is synthesized, never stored.
	 */
	@Override
	public ItemStack getItem() {
		return throwType().isStorm() && isWet() ? GLItems.ANCHOR_WET.get().getDefaultInstance()
				: GLItems.MINAMITSU_ANCHOR.get().getDefaultInstance();
	}

	public boolean isWet() {
		return AnchorStorm.isWet(this);
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
			case FLY -> setDeltaMovement(Vec3.directionFromRotation(getXRot(), getYRot()).scale(FLY_SPEED));
			case RETURN -> {
				Entity owner = getOwner();
				Vec3 diff = owner == null ? Vec3.ZERO : owner.getEyePosition().subtract(position());
				setDeltaMovement(diff.lengthSqr() < 1e-6 ? Vec3.ZERO : diff.normalize().scale(RETURN_SPEED));
			}
			case GRAPPLE -> setDeltaMovement(Vec3.ZERO);
		}
	}

	@Override
	protected void updateRotation() {
		if (phase() == Phase.GRAPPLE) return;
		super.updateRotation();
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
		playImpact(SoundEvents.TRIDENT_HIT_GROUND);
		switch (throwType()) {
			case DESTROY -> {
				breakArea(hit.getBlockPos());
				toReturn();
			}
			case PULL -> enterGrapple();
			case STORM -> {
				AnchorStorm.impact(this, getOwner(), isWet());
				toReturn();
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
		switch (throwType()) {
			case DESTROY -> {
				playImpact(SoundEvents.TRIDENT_HIT);
				hurtTarget(target);
				toReturn();
			}
			case GRAB -> {
				playImpact(SoundEvents.TRIDENT_HIT);
				yank(target);
				toReturn();
			}
			case STORM -> {
				playImpact(SoundEvents.TRIDENT_HIT);
				AnchorStorm.impact(this, getOwner(), isWet());
				toReturn();
			}
			default -> {
			}
		}
	}

	private void tickPhase() {
		if (tickCount > LIFE || ownerGone()) {
			discard();
			return;
		}
		switch (phase()) {
			case FLY -> {
				if (throwType().isStorm()) trail = AnchorStorm.shedTrail(this, getOwner(), isWet(), trail);
				if (position().distanceTo(origin) > throwType().reach()) endFlight();
			}
			case RETURN -> tickReturn();
			case GRAPPLE -> tickGrapple();
		}
	}

	private boolean ownerGone() {
		return !(getOwner() instanceof LivingEntity owner) || owner.isRemoved() || owner.isDeadOrDying();
	}

	private void endFlight() {
		if (throwType() == AnchorThrow.PULL) enterGrapple();
		else toReturn();
	}

	private void toReturn() {
		setPhase(Phase.RETURN);
		returnTicks = 0;
	}

	private void enterGrapple() {
		setPhase(Phase.GRAPPLE);
		grapple = GRAPPLE_TIME;
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
			toReturn();
			return;
		}
		Vec3 diff = position().subtract(player.position());
		if (diff.lengthSqr() < GRAPPLE_DIST * GRAPPLE_DIST) {
			toReturn();
			return;
		}
		player.setDeltaMovement(diff.normalize().scale(GRAPPLE_SPEED));
		player.hurtMarked = true;
		player.fallDistance = 0;
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

	private void yank(Entity target) {
		Entity owner = getOwner();
		if (owner == null || !(target instanceof LivingEntity le)) return;
		Vec3 diff = owner.position().subtract(le.position());
		if (diff.lengthSqr() < 1e-6) return;
		double power = Math.min(1.5, diff.length() * 0.15);
		le.setDeltaMovement(le.getDeltaMovement().add(diff.normalize().scale(power)));
		le.hurtMarked = true;
	}

	private void breakArea(BlockPos center) {
		Entity owner = getOwner();
		Player player = owner instanceof Player p ? p : null;
		boolean drop = player == null || !player.isCreative();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
			BlockState state = level().getBlockState(pos);
			if (state.isAir()) continue;
			if (state.getDestroySpeed(level(), pos) < 0) continue;
			if (!DIAMOND_PICKAXE.isCorrectToolForDrops(state)) continue;
			if (player != null) {
				if (!level().mayInteract(player, pos)) continue;
				if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level(), pos, state, player)).isCanceled())
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
		tag.putInt("AnchorGrapple", grapple);
		tag.putInt("AnchorReturn", returnTicks);
		tag.putInt("AnchorTrail", trail);
		tag.putDouble("AnchorOriginX", origin.x);
		tag.putDouble("AnchorOriginY", origin.y);
		tag.putDouble("AnchorOriginZ", origin.z);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(DATA_TYPE, tag.getByte("AnchorType"));
		entityData.set(DATA_PHASE, tag.getByte("AnchorPhase"));
		damage = tag.getFloat("AnchorDamage");
		grapple = tag.getInt("AnchorGrapple");
		returnTicks = tag.getInt("AnchorReturn");
		trail = tag.getInt("AnchorTrail");
		origin = new Vec3(tag.getDouble("AnchorOriginX"), tag.getDouble("AnchorOriginY"),
				tag.getDouble("AnchorOriginZ"));
	}

	private void playImpact(SoundEvent sound) {
		level().playSound(null, this, sound, SoundSource.PLAYERS, 1F, 1F);
	}

}
