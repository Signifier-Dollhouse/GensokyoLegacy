package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;

import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import java.util.List;

/**
 * The party machine: the ordered list of its tasks, and the numbers they share.
 *
 * <p>
 * <b>The order of {@link #ALL} is the priority.</b> {@code TaskBoard.fetch} collects the
 * exclusive tasks under one activity into a single {@code GateBehavior(ORDERED, RUN_ONE)},
 * which runs the first whose signature matches — so a task lower down may be given a
 * signature as loose as "nothing" as long as everything above it has been ruled out.
 *
 * <p>
 * Which task that amounts to is decided by the fields on the party module, so a guest
 * reloaded from disk lands on the same line of the tree she was on.
 *
 * @see doc/design/party/guest.md
 */
public final class PartyTasks {

	/**
	 * How long she may go without getting any closer to what she is walking to before the
	 * director puts her there instead. Three seconds. Measured as lost ground rather than
	 * elapsed time: a guest crossing a long room is slow and fine.
	 */
	public static final int WALK_STALL = 60;

	/** How often a missing chair is re-checked, in ticks. Three seconds. */
	public static final int SEAT_RETRY = 60;

	/**
	 * How many times she may fail to find a chair before she gives the party up, about nine
	 * seconds: one guest with nowhere to sit holds the whole party in GATHERING.
	 */
	public static final int SEAT_FAILURES = 3;

	/** Ticks spent chewing one serving. */
	public static final int EAT_TICKS = 40;

	/**
	 * How long the pickup gesture runs before the plate is in her hand. The
	 * {@code 使用主手物品} clip is 0.31s, the same figure {@code FairyCakeTask} times against.
	 */
	public static final int PICKUP_TICKS = 6;

	/** Longest a guest hesitates over a dish before reaching for it. Two seconds. */
	public static final int BITE_DELAY = 40;

	/** Shortest such hesitation — the least that still looks like a choice. */
	public static final int BITE_DELAY_MIN = 10;

	/** Longest a guest takes to stand up when the party ends. */
	public static final int UNSEAT_DELAY = 60;

	/** How long she may spend walking away before she is let go regardless. */
	public static final int LEAVE_TIMEOUT = 200;

	/** How far past the guest floor a departing guest is sent, as a band on top of it. */
	public static final int WALK_OUT_SPREAD = 32;

	/** How many places are rolled before giving up on somewhere to walk off to. */
	public static final int WALK_OUT_TRIES = 8;

	/** Looser than a dish or a chair: she is walking away, not picking anything up. */
	public static final double DEPART_REACH = 2;

	private PartyTasks() {
	}

	/** The whole machine, in priority order. */
	public static final List<PartyTask> ALL = List.of(
			new LeaveTask(),
			new ChewTask(),
			new EatTask(),
			new GoToSeatTask(),
			new DelayTask(),
			new PickupTask(),
			new GoToFoodTask(),
			new RestTask(),
			new FindFoodTask(),
			new WaitTableTask(),
			new FindSeatTask());

	/**
	 * Register the party machine on a board. The director is an <i>always</i> task and so runs
	 * alongside whichever of the eleven exclusive tasks is going; exactly one of those is.
	 */
	public static void register(TaskBoard board) {
		board.addAlways(new PartyDirectorTask(), GLBrains.PARTY.get());
		for (var task : ALL) board.addExclusive(0, task, GLBrains.PARTY.get());
	}

}