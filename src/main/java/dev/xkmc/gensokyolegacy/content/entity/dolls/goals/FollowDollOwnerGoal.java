package dev.xkmc.gensokyolegacy.content.entity.dolls.goals;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class FollowDollOwnerGoal extends Goal {
	private final BaseDollEntity doll;
	private final double speedModifier;
	private final float stopDistance;
	private Player owner;

	public FollowDollOwnerGoal(BaseDollEntity doll, double speedModifier, float stopDistance) {
		this.doll = doll;
		this.speedModifier = speedModifier;
		this.stopDistance = stopDistance;
	}

	@Override
	public boolean canUse() {
		this.owner = this.doll.getOwnerPlayerObject();
		if (this.owner == null) return false;
		Vec3 targetPos = this.owner.getEyePosition().add(this.owner.getLookAngle().scale(-1.0));
		double distanceSqr = this.doll.distanceToSqr(targetPos);
		return !(distanceSqr <= this.stopDistance * this.stopDistance);
	}

	@Override
	public void tick() {
		if (this.doll.level().isClientSide) return;
		if (this.owner == null) return;
		Vec3 targetPos = this.owner.getEyePosition().add(this.owner.getLookAngle().scale(-1.0));

		if (!this.doll.level().dimension().equals(this.owner.level().dimension())) {
			ServerLevel targetLevel = this.doll.level().getServer().getLevel(this.owner.level().dimension());
			if (targetLevel != null) {
				this.doll.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, java.util.Set.of(), this.doll.getYRot(), this.doll.getXRot());
			}
			return;
		}
		double dx = targetPos.x - this.doll.getX();
		double dy = targetPos.y - this.doll.getY();
		double dz = targetPos.z - this.doll.getZ();
		double distanceSq = dx * dx + dy * dy + dz * dz;
		if (distanceSq > 256) {
			this.doll.teleportTo(targetPos.x, targetPos.y, targetPos.z);
			return;
		}
		if (distanceSq == 0) return;
		double distance = Math.sqrt(distanceSq);
		this.doll.setDeltaMovement(
				(dx / distance) * this.speedModifier,
				(dy / distance) * this.speedModifier,
				(dz / distance) * this.speedModifier
		);
		this.doll.lookAt(this.owner, 30.0F, 30.0F);
	}

	@Override
	public void stop() {
		this.doll.setDeltaMovement(0, 0, 0);
	}
}
