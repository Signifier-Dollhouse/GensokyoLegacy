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

    private Vec3 getPosBehindOwner() {
        return this.owner.getEyePosition().add(this.owner.getLookAngle().scale(-3.0));
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
        return this.doll.distanceToSqr(this.getPosBehindOwner()) > this.stopDistance * this.stopDistance;
    }

    @Override
    public void start() {
        Vec3 targetPos = this.getPosBehindOwner();
        this.doll.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, this.speedModifier);
    }

    @Override
    public void tick() {
        if (this.doll.level().isClientSide) return;
        Vec3 targetPos = this.getPosBehindOwner();
        if (this.teleportToOwnerPos(targetPos)) {
            return;
        }
        this.doll.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, this.speedModifier);
        this.doll.getLookControl().setLookAt(this.owner, 30.0F, 30.0F);
    }

    @Override
    public void stop() {
        this.doll.getNavigation().stop();
        System.out.println("stopping goal");
    }
}
