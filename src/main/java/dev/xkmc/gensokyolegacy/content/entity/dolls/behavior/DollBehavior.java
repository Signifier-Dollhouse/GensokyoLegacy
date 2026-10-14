package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry.HandMatch;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollHandLock;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Goal-like doll behavior: one per {@link DollActionType}, driven by the single delegating
 * {@code DollCommandGoal}. Capability is re-resolved every check — the doll never caches or
 * switches hands on its own.
 *
 * @see "doc/design/doll/control.md"
 */
public abstract class DollBehavior {

	/** Acting dolls never leave this radius around their owner (kamikaze exempt). */
	public static final double LEASH_RADIUS = 10.0;

	public abstract DollActionType type();

	public abstract boolean canUse(DollEntity doll);

	public abstract boolean canContinueToUse(DollEntity doll);

	public abstract void start(DollEntity doll);

	public abstract void stop(DollEntity doll);

	public abstract void tick(DollEntity doll);

	/**
	 * Whether this behavior keeps running after it let its ticket go — the charge's return flight
	 * releases on impact so a volley can hand off. The delegating goal asks this only once the
	 * ticket is gone.
	 */
	public boolean selfDriven(DollEntity doll) {
		return false;
	}

	/**
	 * Whether this behavior could ever act on the given target with what the doll holds — the
	 * ticket-free form of {@link #canUse}, asked before an order is issued so a ticket that can
	 * never run is given up instead of idling through the start timeout.
	 */
	public boolean canReach(DollEntity doll, LivingEntity target) {
		return true;
	}

	/** My action, iff it is the current command. */
	@Nullable
	protected DollAction current(DollEntity doll) {
		DollAction cur = doll.actions.getCurrent();
		return cur != null && cur.type() == type() ? cur : null;
	}

	protected boolean isServer(DollEntity doll) {
		return doll.level() instanceof ServerLevel;
	}

	@Nullable
	protected LivingEntity resolveTarget(DollEntity doll, DollAction action) {
		if (action.target() == null || !(doll.level() instanceof ServerLevel level)) return null;
		Entity entity = level.getEntity(action.target());
		return entity instanceof LivingEntity target && target.isAlive() ? target : null;
	}

	protected Vec3 targetCenter(LivingEntity target) {
		return target.position().add(0, target.getBbHeight() / 2, 0);
	}

	/** Capability still holds. The stack is a copy. */
	protected Optional<HandMatch> hand(DollEntity doll) {
		return DollBehaviorRegistry.findHand(doll, type());
	}

	/**
	 * The doll acts with its main hand: sticky-swap the ledger when the match is off hand, and
	 * lock both hands for {@link DollHandLock#HOLD} ticks. Behaviors call it right before using
	 * the item, so without the lock an arming host would take the item away mid-animation.
	 */
	protected void ensureMainHand(DollEntity doll, HandMatch match) {
		if (match.hand() != DollSlot.MAIN_HAND) {
			doll.swapHands();
		}
		doll.handLock.stamp(doll.level().getGameTime());
	}

	@Nullable
	protected LivingEntity owner(DollEntity doll) {
		return doll.getOwner();
	}

	protected boolean destWithinLeash(DollEntity doll, Vec3 dest) {
		LivingEntity owner = owner(doll);
		if (owner == null) return true;
		return dest.distanceToSqr(owner.position()) <= LEASH_RADIUS * LEASH_RADIUS;
	}

	protected boolean inRange(DollEntity doll, LivingEntity target, double range) {
		return doll.distanceTo(target) <= range;
	}

	protected void faceTarget(DollEntity doll, LivingEntity target) {
		doll.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
	}

	/**
	 * Turn the doll's body onto a horizontal direction; {@link #faceTarget} only steers the head
	 * — and the look-at-owner goal owns that anyway.
	 */
	protected void faceDir(DollEntity doll, Vec3 dir) {
		doll.setYRot(-(float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG));
		doll.yBodyRot = doll.getYRot();
	}

	protected void halt(DollEntity doll) {
		doll.getNavigation().stop();
	}

}
