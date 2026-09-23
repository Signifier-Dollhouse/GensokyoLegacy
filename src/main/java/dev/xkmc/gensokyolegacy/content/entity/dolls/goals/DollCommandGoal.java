package dev.xkmc.gensokyolegacy.content.entity.dolls.goals;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
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
 * The single combat goal on a doll: resolves the commanded behavior from the
 * held items and delegates every lifecycle call to it. Behaviors are created
 * per execution and dropped on stop, so per-run state can never go stale and
 * the doll stores none.
 * <p>
 * The validating candidate is cached while pending so behavior gates (range,
 * cooldown) apply before start, not just during execution. Unstarted waits are
 * timed out here: a stalled iterative regular hands ahead after 1 second and
 * aborts after 3, anything else aborts after 2. A wait whose target is already
 * dead or gone aborts immediately instead of burning the timeout, so a volley
 * at a dead target drains instantly instead of marching yellow down the roster.
 * Stopping with the ticket still held completes it (done-set + handoff), so
 * aborted runs never strand chains — only an already-released ticket (natural
 * completion, glove stop, a preempting new order) skips completion.
 */
public class DollCommandGoal extends Goal {

	/** Accepted-but-unstarted iterative regular: hand ahead after this long. */
	private static final int HAND_AHEAD_TICKS = 20;

	/** Accepted-but-unstarted iterative regular: abort after this long. */
	private static final int GIVE_UP_TICKS = 60;

	/** Accepted-but-unstarted anything else: abort after this long. */
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
		if (action.target() == null) return false;
		if (!(doll.level() instanceof ServerLevel level)) return false;
		Entity entity = level.getEntity(action.target());
		return !(entity instanceof LivingEntity target && target.isAlive());
	}

	private void checkStartTimeouts(DollAction action) {
		if (isTargetGone(action)) {
			doll.actions.complete(doll);
			pending = null;
			return;
		}
		// Capability lost while waiting (ammo consumed, loadout edited): the
		// ticket can never start, so release it at once instead of idling
		// yellow through the timeout. Cooldown-gated waits keep waiting —
		// capability still resolves, only the behavior gate is closed.
		if (DollBehaviorRegistry.createFor(doll, action).isEmpty()) {
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
				if (host instanceof DollAttachment att) att.commands.handAhead(doll, action);
			}
		} else if (waited >= GIVE_UP_OTHER_TICKS) {
			doll.actions.complete(doll);
			pending = null;
		}
	}

	@Override
	public boolean canContinueToUse() {
		DollAction action = doll.actions.getCurrent();
		if (action == null || active == null || active.type() != action.type()) return false;
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
		// Stopping with the ticket still held (target lost mid-execution, gate
		// failure, preempted wait) completes it through the normal path —
		// done-set + handoff for iterative — so the run never wedges the doll
		// on yellow and chains keep moving. Identity-guarded: a ticket that was
		// already released (natural completion, glove stop) or replaced by a
		// preempting order is a different instance (or null) and is skipped.
		if (activeAction != null && doll.actions.holds(activeAction)) {
			doll.actions.complete(doll);
		}
		activeAction = null;
		pending = null;
	}

	@Override
	public void tick() {
		DollAction action = doll.actions.getCurrent();
		if (action == null || active == null || active.type() != action.type()) return;
		active.tick(doll);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

}
