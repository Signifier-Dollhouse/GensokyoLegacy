package dev.xkmc.gensokyolegacy.content.entity.dolls.goals;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class FollowOwnerGoal extends Goal {
    private final DollEntity mob;
    private final double speedModifier;
    private final float stopDistance;
    private final float emergencyStopDistance;
    private Player owner;

    public FollowOwnerGoal(DollEntity mob, double speedModifier, float stopDistance, float emergencyStopDistance, Player owner) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.stopDistance = stopDistance;
        this.emergencyStopDistance = emergencyStopDistance;
        this.owner = owner;
    }

    @Override
    public boolean canUse() {
        this.owner = this.mob.getOwner();
        if (this.owner == null) return false;
        double distanceSqr = this.mob.distanceToSqr(this.owner);
        if (distanceSqr <= this.stopDistance * this.stopDistance) {
            if (distanceSqr <= this.emergencyStopDistance * this.emergencyStopDistance) {
                this.mob.setDeltaMovement(0, 0, 0);
            }
            return false;
        }
        return true;
    }

    @Override
    public void tick() {
        if (this.owner == null) return;
        double dx = this.owner.getX() - this.mob.getX();
        double dy = this.owner.getY() + 0.5 - this.mob.getY();
        double dz = this.owner.getZ() - this.mob.getZ();

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        this.mob.setDeltaMovement(
                (dx / distance) * this.speedModifier,
                (dy / distance) * this.speedModifier,
                (dz / distance) * this.speedModifier
        );
        this.mob.lookAt(this.owner, 30.0F, 30.0F);
    }
}
