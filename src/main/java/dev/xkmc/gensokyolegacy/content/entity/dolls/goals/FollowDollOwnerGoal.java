package dev.xkmc.gensokyolegacy.content.entity.dolls.goals;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

public class FollowDollOwnerGoal extends Goal {
    /**
     * Height above the owner's feet of the arc's center line: the middle slot
     * hovers the highest and the arc's tips sink back down to this line.
     */
    private static final double FORMATION_HEIGHT = 1.6;
    /** Cap on the vertical reach of the arc above its center line. */
    private static final double FORMATION_ARC_MAX = 3.0;
    private static final double FORMATION_MIN_RADIUS = 2.0;
    private static final double FORMATION_MAX_RADIUS = 6.0;

    private final BaseDollEntity doll;
    private LivingEntity owner;
    private int timeToRecalcPath;

    public FollowDollOwnerGoal(BaseDollEntity doll) {
        this.doll = doll;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Vertical semicircle behind the owner, standing in the plane perpendicular
     * to the latched formation yaw: slot 0..n-1 swing from one flank, up across
     * the top and down to the other flank, the middle slot directly above the
     * owner as the arc's peak. The diameter runs left-right, so the arc reads
     * like a rainbow standing upright face-on to the owner's travel direction.
     */
    private static Vec3 formationPos(Vec3 ownerPos, double feetY, float yawDeg, int index, int total) {
        int n = Math.max(1, total);
        int i = Math.max(0, Math.min(index, n - 1));
        double radius = n <= 1 ? FORMATION_MIN_RADIUS :
                Math.min(FORMATION_MAX_RADIUS, Math.max(FORMATION_MIN_RADIUS, 0.9 * (n - 1) / Math.PI));
        double arc = Math.min(radius, FORMATION_ARC_MAX);
        double depth = radius * 0.6;
        double rad = Math.toRadians(yawDeg);
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        double sx = Math.cos(rad), sz = Math.sin(rad);
        double t = n <= 1 ? Math.PI / 2 : (double) i / (n - 1) * Math.PI;
        double c = Math.cos(t), s = Math.sin(t);
        double ox = (sx * c * radius - fx * depth);
        double oz = (sz * c * radius - fz * depth);
        double oy = feetY + FORMATION_HEIGHT + s * arc;
        return new Vec3(ownerPos.x + ox, oy, ownerPos.z + oz);
    }

    /**
     * This doll's slot in the shared follow semicircle. Slot and count come
     * from the cached fields the roster rebuild stamps each tick — no ledger
     * scan here. The target never tracks the owner's live look vector: the
     * formation yaw only re-anchors when the owner moves, so looking around
     * while standing still leaves every doll where it is. Strays have left
     * the ledger (no slot), so they simply hover over the owner instead of
     * holding a formation slot. Null means "don't follow" (block-hosted
     * dolls have no owner to follow).
     */
    @Nullable
    private Vec3 getTargetPos() {
        LivingEntity owner = this.doll.getOwner();
        if (owner == null) return null;
        if (this.doll.isStray()) return owner.position().add(0, FORMATION_HEIGHT, 0);
        DollHost host = this.doll.getHost();
        if (!(host instanceof DollAttachment att)) return null;
        DollData data = att.findSummoned(this.doll.getUUID());
        if (data == null) return null;
        return formationPos(owner.position(), owner.getY(), att.getFormationYaw(), data.formationIndex, data.formationTotal);
    }

    private boolean teleportToOwnerPos(Vec3 destination) {
        if (!this.doll.level().dimension().equals(this.owner.level().dimension())) {
            ServerLevel targetLevel = this.doll.level().getServer().getLevel(this.owner.level().dimension());
            if (targetLevel != null){
                this.doll.teleportTo(targetLevel, destination.x, destination.y, destination.z, Set.of(), this.doll.getYRot(), this.doll.getXRot());
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
        this.owner = this.doll.getOwner();
        if (this.owner == null) return false;
        Vec3 target = this.getTargetPos();
        if (target == null) return false;
        return this.doll.distanceToSqr(target) > this.doll.stopDistance * this.doll.stopDistance;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        Vec3 targetPos = this.getTargetPos();
        if (targetPos == null) return;
        this.doll.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, this.doll.speedModifier);
    }

    @Override
    public void tick() {
        // No client guard needed: goals only ever tick server-side
        // (Mob.serverAiStep is the sole goalSelector.tick caller and runs
        // only when isEffectiveAi, i.e. never on the client).
        Vec3 targetPos = this.getTargetPos();
        if (targetPos == null) return;
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
