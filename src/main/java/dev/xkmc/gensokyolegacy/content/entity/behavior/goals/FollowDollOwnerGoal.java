package dev.xkmc.gensokyolegacy.content.entity.behavior.goals;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class FollowDollOwnerGoal extends Goal {
    private final BaseDollEntity doll;
    private Player owner;
    private int timeToRecalcPath;

    public FollowDollOwnerGoal(BaseDollEntity doll) {
        this.doll = doll;
    }

    private Vec3 getPosBehindOwner() {
        Vec3 base = this.owner.getEyePosition().add(this.owner.getLookAngle().scale(3.0));
        double minY = this.owner.getY() + 0.8;
        return new Vec3(base.x, Math.max(base.y, minY), base.z);
    }

    private boolean teleportToOwnerPos(Vec3 destination) {
        if (!this.doll.level().dimension().equals(this.owner.level().dimension())) {
            ServerLevel targetLevel = this.doll.level().getServer().getLevel(this.owner.level().dimension());
            if (targetLevel != null){
                this.doll.teleportTo(targetLevel, destination.x, destination.y, destination.z, java.util.Set.of(), this.doll.getYRot(), this.doll.getXRot());
            }
            return true;
        }
        double dx = destination.x - this.doll.getX();
        double dy = destination.y - this.doll.getY();
        double dz = destination.z - this.doll.getZ();
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq > 144) {
            this.doll.teleportTo(destination.x, destination.y, destination.z);
            return true;
        }
        return false;
    }

    @Override
    public boolean canUse() {
        this.owner = this.doll.getOwnerPlayerObject();
        if (this.owner == null) return false;
        return this.doll.distanceToSqr(this.getPosBehindOwner()) > this.doll.stopDistance * this.doll.stopDistance;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        Vec3 targetPos = this.getPosBehindOwner();
        this.doll.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, this.doll.speedModifier);
    }

    @Override
    public void tick() {
        if (this.doll.level().isClientSide) return;
        Vec3 targetPos = this.getPosBehindOwner();
        if (this.teleportToOwnerPos(targetPos)) {
            return;
        }
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            this.doll.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, this.doll.speedModifier);
        }
    }

    @Override
    public void stop() {
        this.doll.getNavigation().stop();
    }
}
