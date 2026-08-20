package dev.xkmc.gensokyolegacy.content.entity.dolls.goals;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class FollowOwnerGoal extends Goal {
    private final BaseDollEntity doll;
    private final double speedModifier;
    private final float stopDistance;
    private final float emergencyStopDistance;
    private Player owner;

    public FollowOwnerGoal(BaseDollEntity doll, double speedModifier, float stopDistance, float emergencyStopDistance, Player owner) {
        this.doll = doll;
        this.speedModifier = speedModifier;
        this.stopDistance = stopDistance;
        this.emergencyStopDistance = emergencyStopDistance;
        this.owner = owner;
    }

    @Override
    public boolean canUse() {
        this.owner = this.doll.getOwner();
        if (this.owner == null) return false;
        double distanceSqr = this.doll.distanceToSqr(this.owner);
        if (distanceSqr <= this.stopDistance * this.stopDistance) {
            if (distanceSqr <= this.emergencyStopDistance * this.emergencyStopDistance) {
                this.doll.setDeltaMovement(0, 0, 0);
            }
            return false;
        }
        return true;
    }

    @Override
    public void tick() {
        if (this.owner == null) return;
        double dx = this.owner.getX() - this.doll.getX();
        double dy = this.owner.getY() + 0.5 - this.doll.getY();
        double dz = this.owner.getZ() - this.doll.getZ();
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq == 0) return;
        double distance = Math.sqrt(distanceSq);
        this.doll.setDeltaMovement(
                (dx / distance) * this.speedModifier,
                (dy / distance) * this.speedModifier,
                (dz / distance) * this.speedModifier
        );
        this.doll.lookAt(this.owner, 30.0F, 30.0F);
    }
}
