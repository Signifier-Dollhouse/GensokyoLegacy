package dev.xkmc.gensokyolegacy.content.entity.misc;

import dev.xkmc.gensokyolegacy.content.attachment.index.IndexStorage;
import dev.xkmc.gensokyolegacy.content.attachment.index.MissingTowerManager;
import dev.xkmc.gensokyolegacy.content.attachment.index.MissingTowerState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * The missing tower as a world object: it follows its carrier or hovers at the record's position.
 *
 * @see "doc/design/quest/missing_tower.md"
 */
public class MissingTowerEntity extends Entity {

	private static final double RENDER_RANGE = 128;

	@Nullable
	private UUID owner;

	public MissingTowerEntity(EntityType<? extends MissingTowerEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	@Nullable
	public UUID getOwnerId() {
		return owner;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide || !(level() instanceof ServerLevel level)) return;
		IndexStorage storage = IndexStorage.get(level);
		MissingTowerState state = owner == null ? null : storage.getTower(owner);
		if (state == null || !Objects.equals(state.entity(), getUUID())
				|| !state.dim().equals(level.dimension().location())) {
			discard();
			return;
		}
		if (state.carried()) {
			ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
			if (player == null || player.isDeadOrDying() || player.level() != level
					|| player.distanceToSqr(this) > MissingTowerManager.FOLLOW_RANGE * MissingTowerManager.FOLLOW_RANGE) {
				MissingTowerManager.setGround(state, level,
						Vec3.atBottomCenterOf(player == null ? blockPosition() : player.blockPosition()));
				if (player != null) MissingTowerManager.syncProgress(player);
				return;
			}
			follow(player);
			if (tickCount % 10 == 0) MissingTowerManager.syncPos(state, blockPosition());
			return;
		}
		Vec3 at = Vec3.atBottomCenterOf(state.pos()).add(0, 0.35, 0);
		setDeltaMovement(Vec3.ZERO);
		if (position().distanceToSqr(at) > 1e-4) setPos(at.x, at.y, at.z);
	}

	/**
	 * The target is 0.55 behind the player at shoulder height, but the tower keeps a 5-block
	 * standoff; it steers with a capped, blended velocity so it glides instead of snapping.
	 */
	private void follow(Player player) {
		Vec3 look = player.getLookAngle();
		Vec3 facing = new Vec3(look.x, 0, look.z);
		if (facing.lengthSqr() < 1e-4) facing = Vec3.directionFromRotation(0, player.getYRot());
		facing = facing.normalize();
		Vec3 target = player.position()
				.subtract(facing.scale(0.55))
				.add(0, player.getEyeHeight() * 0.8, 0);
		Vec3 toTarget = target.subtract(position());
		double dist = toTarget.length();
		double speed = Math.min(dist * MissingTowerManager.FOLLOW_GAIN, MissingTowerManager.FOLLOW_SPEED);
		Vec3 wish = dist < 1e-4 ? Vec3.ZERO : toTarget.scale(speed / dist);
		Vec3 vel = getDeltaMovement().lerp(wish, MissingTowerManager.FOLLOW_ACCEL);
		Vec3 off = horizontal(position().subtract(player.position()));
		double ring = off.length();
		if (ring > 1e-4) {
			// cap the inward radial speed at the distance left to the 5-block ring
			Vec3 out = off.scale(1 / ring);
			double inward = -vel.dot(out);
			double maxInward = Math.max(0, ring - MissingTowerManager.FOLLOW_DISTANCE);
			if (inward > maxInward) vel = vel.add(out.scale(inward - maxInward));
		}
		setDeltaMovement(vel);
		move(MoverType.SELF, vel);
	}

	private static Vec3 horizontal(Vec3 vec) {
		return new Vec3(vec.x, 0, vec.z);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (level().isClientSide) return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer sp) || !(level() instanceof ServerLevel level))
			return InteractionResult.PASS;
		return MissingTowerManager.pickup(sp, level, this)
				? InteractionResult.CONSUME : InteractionResult.FAIL;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return true;
	}

	@Override
	public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < RENDER_RANGE * RENDER_RANGE;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		if (owner != null) tag.putUUID("Owner", owner);
	}

}
