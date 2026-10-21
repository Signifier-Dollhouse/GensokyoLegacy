package dev.xkmc.gensokyolegacy.content.entity.dolls.goals;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionMode;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehavior;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * The single combat goal: resolves the commanded behavior from the held items and delegates every lifecycle call;
 * instances are per-execution and dropped on stop, so per-run state can never go stale. Gates apply before start via
 * the cached pending candidate, and a wait whose target is dead, unreachable or unstartable aborts at once.
 */
public class DollCommandGoal extends Goal {

	/**
	 * Accepted-but-unstarted iterative regular: hand ahead after this long.
	 */
	private static final int HAND_AHEAD_TICKS = 20;

	/**
	 * Accepted-but-unstarted iterative regular: abort after this long.
	 */
	private static final int GIVE_UP_TICKS = 60;

	/**
	 * Accepted-but-unstarted anything else: abort after this long.
	 */
	private static final int GIVE_UP_OTHER_TICKS = 40;

	private final DollEntity doll;

	@Nullable
	private DollBehavior pending;

	@Nullable
	private DollBehavior active;

	@Nullable
	private DollAction activeAction;

	public DollCommandGoal(DollEntity doll) {
		this.doll = doll;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	/**
	 * Whether a behavior is currently executing (started, not just pending):
	 * drives the attacking-vs-preparing split in the synced sidebar status.
	 */
	public boolean isExecuting() {
		return active != null;
	}

	@Override
	public boolean canUse() {
		if (!(doll.level() instanceof ServerLevel)) return false;
		DollAction action = doll.actions.getCurrent();
		if (action == null) {
			pending = null;
			return false;
		}
		checkStartTimeouts(action);
		action = doll.actions.getCurrent();
		if (action == null) {
			pending = null;
			return false;
		}
		if (pending == null || pending.type() != action.type()) pending = null;
		if (pending == null) pending = DollBehaviorRegistry.createFor(doll, action).orElse(null);
		return pending != null && pending.canUse(doll);
	}

	/**
	 * True when the action names a target that is no longer a live entity in
	 * this level (dead, removed, or left). A null target never counts as gone.
	 */
	private boolean isTargetGone(DollAction action) {
		return action.target() != null && liveTarget(action) == null;
	}

	/**
	 * The action's target as a live entity of this level, or null when it names
	 * none or names something that is gone.
	 */
	@Nullable
	private LivingEntity liveTarget(DollAction action) {
		if (action.target() == null || !(doll.level() instanceof ServerLevel level)) return null;
		Entity entity = level.getEntity(action.target());
		return entity instanceof LivingEntity target && target.isAlive() ? target : null;
	}

	private void checkStartTimeouts(DollAction action) {
		if (isTargetGone(action)) {
			doll.actions.complete(doll);
			pending = null;
			return;
		}
		DollBehavior behavior = DollBehaviorRegistry.createFor(doll, action).orElse(null);
		// capability lost while waiting (ammo consumed, loadout edited): release at once
		// instead of idling yellow. A cooldown-gated wait keeps waiting — capability still resolves.
		if (behavior == null) {
			doll.actions.complete(doll);
			pending = null;
			return;
		}
		// out of the behavior's reach, and no amount of waiting changes that: the
		// ticket goes now rather than burning the start timeout and its hand-ahead
		LivingEntity target = liveTarget(action);
		if (target != null && !behavior.canReach(doll, target)) {
			doll.actions.complete(doll);
			pending = null;
			return;
		}
		long waited = doll.level().getGameTime() - doll.actions.acceptedAt();
		if (action.mode() == DollActionMode.ITERATIVE && action.type() == DollActionType.REGULAR_ATTACK) {
			if (waited >= GIVE_UP_TICKS) {
				doll.actions.complete(doll);
				pending = null;
			} else if (waited >= HAND_AHEAD_TICKS && !doll.actions.handAheadSent()) {
				doll.actions.markHandAheadSent();
				DollHost host = doll.getHost();
				if (host != null) host.handAhead(doll, action);
			}
		} else if (waited >= GIVE_UP_OTHER_TICKS) {
			doll.actions.complete(doll);
			pending = null;
		}
	}

	@Override
	public boolean canContinueToUse() {
		if (active == null) return false;
		DollAction action = doll.actions.getCurrent();
		// the ticket is gone: either a behavior released it on its own — only that
		// behavior then decides whether it is still doing anything — or something stopped us
		if (action == null) return active.selfDriven(doll);
		if (active.type() != action.type()) return false;
		return active.canContinueToUse(doll);
	}

	@Override
	public void start() {
		DollAction action = doll.actions.getCurrent();
		if (action == null || pending == null || pending.type() != action.type() ||
				!pending.canUse(doll)) {
			pending = action == null ? null :
					DollBehaviorRegistry.createFor(doll, action).filter(b -> b.canUse(doll)).orElse(null);
		}
		active = pending;
		pending = null;
		activeAction = action;
		if (active != null) {
			active.start(doll);
		}
	}

	@Override
	public void stop() {
		if (active != null) {
			active.stop(doll);
			active = null;
		}
		// stopping with the ticket still held (target lost mid-execution, gate failure,
		// preempted wait) completes it, so the run never wedges the doll on yellow. Identity-guarded.
		if (activeAction != null && doll.actions.holds(activeAction)) {
			doll.actions.complete(doll);
		}
		activeAction = null;
		pending = null;
	}

	@Override
	public void tick() {
		if (active == null) return;
		DollAction action = doll.actions.getCurrent();
		if (action == null) {
			if (!active.selfDriven(doll)) return;
		} else if (active.type() != action.type()) {
			return;
		}
		active.tick(doll);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

}
