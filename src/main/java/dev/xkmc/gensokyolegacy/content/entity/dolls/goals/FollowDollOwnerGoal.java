package dev.xkmc.gensokyolegacy.content.entity.dolls.goals;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

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

    /** Catch-up range, squared: past 5 blocks from the slot the doll is lost. */
    private static final double LOST_RANGE_SQ = 25;

    /**
     * How long the owner must stay out of sight before the doll treats itself as lost and
     * blinks back — the grace period Alice's escort gets too (host.md §5.1).
     */
    private static final int BLIND_TICKS = 20;

    private final BaseDollEntity doll;
    private LivingEntity owner;
    private int timeToRecalcPath;

    /** Game time the owner was first lost from sight, -1 while visible; a goal that does not override
     * {@code requiresUpdateEveryTick} only ticks every other pass, so this is game time, not a tick counter. */
    private long unseenSince = -1;

    public FollowDollOwnerGoal(BaseDollEntity doll) {
        this.doll = doll;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Vertical semicircle behind the owner, perpendicular to the latched formation yaw: slots
     * swing from one flank up across the top to the other, the middle slot peaking over the owner.
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
     * This doll's slot in the shared follow semicircle, from the roster-rebuilt cached fields. The
     * yaw only re-anchors when the owner moves, never on their live look vector. Strays hover
     * overhead instead, and null means "don't follow".
     */
    @Nullable
    private Vec3 getTargetPos() {
        LivingEntity owner = this.doll.getOwner();
        if (owner == null) return null;
        if (this.doll.isStray()) return owner.position().add(0, FORMATION_HEIGHT, 0);
        DollHost host = this.doll.getHost();
        if (host == null) return null;
        DollData data = host.findSummoned(this.doll.getUUID());
        if (data == null) return null;
        return formationPos(owner.position(), owner.getY(), host.getFormationYaw(), data.formationIndex, data.formationTotal);
    }

    /**
     * Snap to the formation slot when the doll is lost: same level, no command, further than
     * {@link #LOST_RANGE_SQ}, blind for {@link #BLIND_TICKS} and standable ({@link #landingSafe}).
     * Teleporting is not velocity, so this does not violate the movement cap.
     */
    private boolean teleportToOwnerPos(Vec3 destination) {
        if (this.owner == null || this.doll.level() != this.owner.level()) return false;
        // pending tickets count as commands too: a doll still winding up to attack
        // is being placed deliberately as well
        if (this.doll instanceof DollEntity dollEntity && dollEntity.actions.isActive()) return false;
        if (this.doll.distanceToSqr(destination) <= LOST_RANGE_SQ) return false;
        long gameTime = this.doll.level().getGameTime();
        if (this.unseenSince < 0 || gameTime - this.unseenSince < BLIND_TICKS) return false;
        // the fallback is deliberately not re-tested: the owner is standing there,
        // and a collision query at that point would hit its own hitbox every time
        Vec3 landing = landingSafe(destination) ? destination : this.owner.position();
        this.doll.teleportTo(landing.x, landing.y, landing.z);
        // the path that led here is meaningless from the landing point
        this.doll.getNavigation().stop();
        this.timeToRecalcPath = 0;
        return true;
    }

    /**
     * Whether the doll's own bounding box fits at {@code target}. The target is within a few blocks
     * of the owner, hence inside loaded chunks, so this never forces a chunk load.
     */
    private boolean landingSafe(Vec3 target) {
        AABB box = this.doll.getBoundingBox().move(target.subtract(this.doll.position()));
        return this.doll.level().noCollision(this.doll, box);
    }

    /**
     * Restarts the blindness clock: a fresh follow run owes the owner a full grace period, so a doll
     * that just came into range is never blinked back on a stale window.
     */
    private void trackSight() {
        if (this.doll.hasLineOfSight(this.owner)) {
            this.unseenSince = -1;
            return;
        }
        long gameTime = this.doll.level().getGameTime();
        if (this.unseenSince < 0) this.unseenSince = gameTime;
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
        this.unseenSince = -1;
        Vec3 targetPos = this.getTargetPos();
        if (targetPos == null) return;
        this.doll.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, this.doll.speedModifier);
    }

    @Override
    public void tick() {
        // no client guard needed: goals only ever tick server-side
        Vec3 targetPos = this.getTargetPos();
        if (targetPos == null) return;
        this.trackSight();
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
