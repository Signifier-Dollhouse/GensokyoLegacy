package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry.HandMatch;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import net.minecraft.server.level.ServerLevel;
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
	 * off hand (the mirror follows).
	 */
	protected void ensureMainHand(DollEntity doll, HandMatch match) {
		if (match.hand() != DollSlot.MAIN_HAND) {
			doll.swapHands();
		}
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

	protected void halt(DollEntity doll) {
		doll.getNavigation().stop();
	}

}
