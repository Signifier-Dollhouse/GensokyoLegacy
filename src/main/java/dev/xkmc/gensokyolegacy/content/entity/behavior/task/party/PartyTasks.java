package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.behavior.brain.TaskBoard;

import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import java.util.List;

/**
 * The party machine: the ordered list of its tasks, and the numbers they share.
 *
 * <p>
 * <b>The order below <i>is</b> the priority.</b> {@code TaskBoard.fetch} collects the
 * exclusive tasks registered under one activity into a single
 * {@code GateBehavior(ORDERED, RUN_ONE)}, which tries them in insertion order and runs the
 * first whose memory signature matches. So a task lower down may be given a signature as
 * loose as "nothing", as long as everything more specific above it has already been ruled
 * out — that is what makes the cascade total rather than a list of cases.
 *
 * <p>
 * Read as a decision tree:
 *
 * <pre>
 *   leave armed?                          → Leave
 *   has serve  + chewing?                 → Chew
 *   has serve  + seated?                  → Eat
 *   has serve                             → GoToSeat
 *   has dish  + hesitating?               → Delay
 *   has dish  + grabbing?                 → Pickup
 *   has dish                              → GoToFood
 *   resting?                              → Rest
 *   seated, party feasting?               → FindFood
 *   seated, party not feasting?           → WaitTable
 *   otherwise                             → FindSeat
 * </pre>
 *
 * <p>
 * Everything in that tree is derived from seven memories, so there is no state to lose:
 * a guest reloaded from disk lands on the same line of the tree she was on.
 */
public final class PartyTasks {

	/**
	 * How long she may go without getting any closer to what she is walking to before the
	 * director puts her there instead. Three seconds.
	 *
	 * <p>
	 * Measured as lost ground rather than elapsed time, because a guest crossing a long
	 * room is slow and fine, while one who has not gained a block in three seconds is not
	 * going to.
	 */
	public static final int WALK_STALL = 60;

	/** How often a missing chair is re-checked, in ticks. Three seconds. */
	public static final int SEAT_RETRY = 60;

	/**
	 * How many times she may fail to find a chair before she gives the party up. About nine
	 * seconds: the escape from a table that will never eat, since one guest with nowhere to
	 * sit holds the whole party in GATHERING.
	 */
	public static final int SEAT_FAILURES = 3;

	/** Ticks spent chewing one serving. */
	public static final int EAT_TICKS = 40;

	/**
	 * How long the pickup gesture runs before the plate is in her hand. The
	 * {@code 使用主手物品} clip is 0.31s and {@code FairyCakeTask} times a gesture against
	 * exactly this number, so the two agree rather than each guessing.
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

	/**
	 * The whole machine, in priority order. See the class comment for why the order is the
	 * thing to read.
	 */
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
	 * Register the party machine on a board.
	 *
	 * <p>
	 * The director is an <i>always</i> task and so runs alongside whichever of the eleven is
	 * running; the eleven are exclusive, so exactly one of them is.
	 */
	/** The whole machine, as one line for an entity class. */
	public static void register(TaskBoard board) {
		board.addAlways(new PartyDirectorTask(), GLBrains.PARTY.get());
		for (var task : ALL) board.addExclusive(0, task, GLBrains.PARTY.get());
	}

}