package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ally-aware firing lanes for ranged behaviors. Allies are the owner plus fellow
 * summoned dolls of the same ledger (never the target itself). A lane is blocked
 * when an ally body comes within hit margin of the doll → target segment; the
 * behavior then strafes instead of firing into its own team.
 */
public final class DollFriendlyFire {

	private static final double LANE_MARGIN = 0.6;
	private static final double STRAFE_STEP = 2.5;
	private static final double STRAFE_LEASH = 9.0;

	private DollFriendlyFire() {
	}

	public static List<LivingEntity> allies(DollEntity doll, @Nullable LivingEntity target) {
		List<LivingEntity> out = new ArrayList<>();
		LivingEntity owner = doll.getOwner();
		if (owner != null && owner.isAlive() && owner != target) out.add(owner);
		DollHost host = doll.getHost();
		if (host instanceof DollAttachment att) {
			for (DollEntity ally : att.commands.summonedAllies(doll)) {
				if (ally.isAlive() && ally != target) out.add(ally);
			}
		}
		return out;
	}

	public static Optional<LivingEntity> findBlocker(DollEntity doll, Vec3 targetPos,
													 @Nullable LivingEntity target) {
		Vec3 from = center(doll);
		for (LivingEntity ally : allies(doll, target)) {
			Vec3 p = center(ally);
			if (distPointSegment(p, from, targetPos) < ally.getBbWidth() / 2 + LANE_MARGIN) {
				return Optional.of(ally);
			}
		}
		return Optional.empty();
	}

	public static boolean blockedByAlly(DollEntity doll, Vec3 targetPos, @Nullable LivingEntity target) {
		return findBlocker(doll, targetPos, target).isPresent();
	}

	/**
	 * Sidestep away from the blocker, holding altitude and staying inside the
	 * leash. Null when neither side has room.
	 */
	@Nullable
	public static Vec3 strafeDest(DollEntity doll, Vec3 from, Vec3 to, LivingEntity blocker) {
		Vec3 dir = to.subtract(from);
		dir = new Vec3(dir.x, 0, dir.z);
		if (dir.lengthSqr() < 1e-6) return null;
		dir = dir.normalize();
		Vec3 perp = new Vec3(-dir.z, 0, dir.x);
		Vec3 away = blocker.position().subtract(from);
		away = new Vec3(away.x, 0, away.z);
		if (away.lengthSqr() > 1e-6 && perp.dot(away) < 0) perp = perp.scale(-1);
		Vec3 dest = new Vec3(from.x + perp.x * STRAFE_STEP, from.y, from.z + perp.z * STRAFE_STEP);
		dest = clampLeash(doll, dest);
		if (dest == null || dest.distanceToSqr(from) < 0.25) {
			Vec3 other = new Vec3(from.x - perp.x * STRAFE_STEP, from.y, from.z - perp.z * STRAFE_STEP);
			other = clampLeash(doll, other);
			if (other == null || other.distanceToSqr(from) < 0.25) return null;
			return other;
		}
		return dest;
	}

	@Nullable
	private static Vec3 clampLeash(DollEntity doll, Vec3 dest) {
		LivingEntity owner = doll.getOwner();
		if (owner == null) return dest;
		Vec3 op = owner.position();
		Vec3 off = new Vec3(dest.x - op.x, 0, dest.z - op.z);
		if (off.lengthSqr() <= STRAFE_LEASH * STRAFE_LEASH) return dest;
		if (off.lengthSqr() < 1e-6) return null;
		Vec3 clamped = off.normalize().scale(STRAFE_LEASH);
		return new Vec3(op.x + clamped.x, dest.y, op.z + clamped.z);
	}

	private static Vec3 center(LivingEntity entity) {
		return entity.position().add(0, entity.getBbHeight() / 2, 0);
	}

	private static double distPointSegment(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double lenSq = ab.lengthSqr();
		if (lenSq < 1e-9) return p.distanceTo(a);
		double t = Math.clamp(p.subtract(a).dot(ab) / lenSq, 0, 1);
		return p.distanceTo(a.add(ab.scale(t)));
	}

}
