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
 * Goal-like doll behavior: one per {@link DollActionType}, driven by the single
 * delegating {@code DollCommandGoal}. Lifecycle mirrors {@code Goal}
 * ({@code canUse / canContinueToUse / start / stop / tick}) but runs against an
 * explicit doll instead of goal-selector state, so behaviors stay plain objects
 * with per-doll instances. Capability is re-resolved every check — the doll never
 * caches or switches hands on its own.
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
	 * Whether this behavior keeps running after it let its ticket go. The charge's
	 * return flight is the case: it releases the ticket on impact so a volley can
	 * hand off, and only the flight itself is left. The delegating goal asks this
	 * once the ticket is gone, and only for as long as it holds — a doll that takes
	 * a new order mid-flight leaves the flight behind.
	 */
	public boolean selfDriven(DollEntity doll) {
		return false;
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
	 * The doll acts with its main hand: sticky-swap the ledger when the match is
	 * off hand (the mirror follows), and lock both hands for {@link DollHandLock#HOLD}
	 * ticks.
	 * <p>
	 * The lock is why this is also where the spend is recorded. Every behavior calls
	 * this immediately before using the item, and several complete their ticket on
	 * that same tick, so without it a host could re-arm the doll on the next tick
	 * and the client would watch the item blink out of the hand mid-animation.
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
	 * Turn the doll's body onto a horizontal direction. {@link #faceTarget} only
	 * steers the head — and the look-at-owner goal owns that anyway — so a behavior
	 * that flies on plain velocity, with no {@code DollMoveControl} to orient it,
	 * has to set the body itself the same way the move control does.
	 */
	protected void faceDir(DollEntity doll, Vec3 dir) {
		doll.setYRot(-(float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG));
		doll.yBodyRot = doll.getYRot();
	}

	protected void halt(DollEntity doll) {
		doll.getNavigation().stop();
	}

}
