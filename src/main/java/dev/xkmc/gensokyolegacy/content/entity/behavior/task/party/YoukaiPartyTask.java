package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeSearchUtil;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.IHomeHolder;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyArea;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodValue;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyGift;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartySend;
import dev.xkmc.gensokyolegacy.content.attachment.party.item.PartyMessage;
import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.module.VisitModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A guest's whole visit: find a chair, wait for the table to fill, then eat her way
 * through the spread until there is none of it left within reach, and go when the party
 * ends or her host's food does.
 * <p>
 * The shape is a cycle, not walk-then-finish. Each round she draws a dish weighted by its
 * appeal, hesitates, picks the serving up, carries it back to her chair if she had to
 * leave it, eats it there, and sits for a while before the next. How long that sit is
 * depends on whether she has eaten her fill: her {@code hungrySeat} while she still needs
 * the food and her {@code fullSeat} once she has had enough and is only grazing — both per
 * character, not constants. She never leaves on being satisfied; the visit ends when her
 * {@code leaveChance} comes up, or when the party's food does, or when the party closes.
 * <p>
 * Most rounds she does not move at all. A dish within {@link #REACH_SEATED} of where she
 * sits is taken from her chair, which is the common case at a laid table and the reason
 * that reach is longer than the one she has on her feet. Only a dish further out than
 * that sends her across the room and back.
 *
 * <p>
 * She never walks to the chair or the dish herself: both walks go to a standable block
 * next to them ({@link PartySend#standingNear}), and she has to be standing <i>on</i> it
 * before she acts. A chair is furniture you sit down from beside, and a dish on a table
 * has air beside it rather than floor, so in both cases "the block she walks to" and "the
 * thing she is there for" are different blocks — and conflating them is what turns a
 * reach into a fudge factor.
 *
 * <p>
 * Neither a chair nor a dish is walked to directly — both walks go to a standable block
 * next to them, and she must be standing <i>on</i> it before she acts. See
 * {@link #REACH} and {@link PartySend#standingNear}. Neither detail is cosmetic: a guest
 * who cannot sit in a chair she is standing next to, or who is asked to path into a
 * chair's own collision box, looks broken in the most visible way this feature has.
 * <p>
 * Sitting means <em>riding</em> a {@code ChairEntity} (see {@code HomeSearchUtil.setSitting}),
 * and a passenger does not walk. So leaving a chair is an explicit dismount, and every
 * departure owes a return — otherwise the second trip out is measured from wherever the
 * first one left her. The same ride is why the unseat on the way out is its own beat.
 * <p>
 * The eating itself is one rig's art at present: {@code YoukaiAnim} has no EAT slot and
 * only a plain fairy maps a chew, so {@link #startEating} plays a no-op for everybody else
 * and the beat is carried by the sound, the held item and the wait.
 */
public class YoukaiPartyTask extends Behavior<SmartYoukaiEntity> {

	/** Ticks spent eating one dish. */
	private static final int EAT_TICKS = 40;

	/**
	 * How long the pickup gesture runs before the item is in her hand.
	 * <p>
	 * The {@code 使用主手物品} clip is 0.31s; {@link FairyCakeTask} times a gesture against
	 * exactly this number, so the two agree rather than each guessing.
	 */
	private static final int PICKUP_TICKS = 6;

	/**
	 * How long she waits after drawing a dish before she reaches for it.
	 * <p>
	 * Random every time, and drawn per bite rather than once per guest. Without it a table
	 * of guests draws, stands and eats in lockstep, which reads as one organism rather
	 * than as people. The floor matters as much as the ceiling: an instant draw reads as
	 * the character reacting to the player rather than deciding, and half a second is the
	 * shortest delay that still looks like a choice. Forty ticks is two seconds — long
	 * enough to see, short enough that {@code hungrySeat} still sets the pace.
	 */
	private static final int BITE_DELAY = 40;
	/** The floor on that delay: half a second, so a draw is never acted on instantly. */
	private static final int BITE_DELAY_MIN = 10;

	/**
	 * How long one engaged stretch may last before she is written off.
	 * <p>
	 * Refreshed at every seek <em>and</em> on sitting down to rest, so it has to cover a
	 * whole round trip to a dish — walk out, eat, walk back, sit — and not merely the walk
	 * itself. A guest eating from her chair never walks at all, so nothing would refresh
	 * it for her, and she would be dismissed part-way through the fourth or fifth dish.
	 * {@link PartyModule#ARRIVE_TIMEOUT} covers the single trip to her chair instead, and
	 * {@code PARTY_TIMEOUT} bounds the visit as a whole.
	 */
	private static final int LEG_TIMEOUT = 200;

	/**
	 * How often a missing chair is re-checked, in ticks. Three seconds.
	 * <p>
	 * Long enough that the host has put out the chair rather than has not, short enough
	 * that three of them plus a walk is still under the ten seconds the old single timeout
	 * allowed — so the party is not held open any longer than it used to be, it is only
	 * told about sooner.
	 */
	private static final int SEAT_RETRY = 60;

	/**
	 * How many times she may fail to find a chair before she gives the party up.
	 * <p>
	 * Three failures three seconds apart is about nine seconds. The escape from a table
	 * that will never eat: without it one guest with nowhere to sit holds the whole party
	 * in GATHERING — the party-wide feast gate waits for everyone — and the host is told
	 * nothing until the ten-minute timeout calls the party off.
	 */
	private static final int SEAT_FAILURES = 3;

	/**
	 * How long she waits before standing up when the party ends, at most. Staggered per
	 * guest so the table does not empty inside one tick.
	 */
	private static final int UNSEAT_DELAY = 60;

	/**
	 * How long she may spend walking away or home before she is let go of the party
	 * regardless. A guest who cannot reach her destination is not stuck holding a chair
	 * for ever.
	 */
	private static final int LEAVE_TIMEOUT = 200;

	/**
	 * How close she must be to a dish once she has reached the block she walked to.
	 *
	 * <p>
	 * <b>Two numbers, because she is holding a plate out over the table.</b> From her
	 * chair she reaches three blocks, which is what makes most rounds of a laid banquet
	 * happen without her standing up at all; from her feet it is two, so that standing up
	 * and crossing to a dish three blocks away is not possible when she could have eaten
	 * it from where she was sitting.
	 *
	 * <p>
	 * Neither is the size of her arm — {@link PartySend#standingNear} has already put her
	 * standing next to the dish she came for — so they are slack for being sent to the
	 * wrong side of a long table, not reach proper.
	 *
	 * <p>
	 * There is no third number for the chair, and that is the point of arriving on a block
	 * <b>exactly</b>: the block she is sent to is adjacent to the chair by construction, so
	 * "she got there" and "she is next to her seat" are the same statement and need no
	 * separate tolerance to agree.
	 *
	 * <p>
	 * Measured to the <b>block's box</b> rather than its centre, which is what makes these
	 * numbers mean what they look like: standing on the block beside a dish is zero blocks
	 * from it.
	 */
	private static final double REACH_SEATED = 3;
	private static final double REACH_STANDING = 2;

	/**
	 * How close she must get to the block she is walking <i>to</i>. Zero: she has to be
	 * standing on it.
	 *
	 * <p>
	 * Exact, and the reason the walk target is a standable block rather than the thing
	 * itself. There is nothing to be "near" about a floor tile — she either reached it or
	 * she did not, and a tolerance here would have her act on a dish from the far end of
	 * the table, which is exactly the case the reach above exists to keep small.
	 */
	private static final double ARRIVAL_REACH = 0;

	/**
	 * How close she must get to where she is walking when she is <b>leaving</b>.
	 *
	 * <p>
	 * Loose, and the one place the exact rule does not apply: she is on her way out, not
	 * picking anything up, so the last two steps of a departure are not worth the leave
	 * timeout that would follow them.
	 */
	private static final double DEPART_REACH = 2;

	private enum Phase {
		/** Walking to a chair. */
		SEEK_SEAT,
		/** Seated, waiting for the rest of the table to be seated. */
		WAIT_TABLE,
		/** Drawing the next dish. */
		SEEK_FOOD,
		/**
		 * Holding the dish she just drew before she reaches for it.
		 * <p>
		 * A separate phase rather than a delay inside {@link #SEEK_FOOD}, because the two
		 * ends of it do different things: a dish in reach is picked up on the spot, and one
		 * that is not is walked to first. Folding the wait into the seek would put a delay
		 * between drawing and walking, which is not what a guest does.
		 */
		DELAY,
		/** Picking the serving up: the use animation, and then it is in her hand. */
		PICKUP,
		/** Eating it, seated, with the item leaving her hand as she goes. */
		EATING,
		/** Walking back to her chair, having picked a dish up away from it. */
		RETURNING,
		/** Sitting it out before the next dish. */
		RESTING,
		/** The party is over: stand up, then go. */
		LEAVING
	}

	private Phase phase = Phase.SEEK_SEAT;
	@Nullable
	private BlockPos target;
	private long deadline;
	/** Deadline of the current wait, for EATING, RESTING, DELAY and PICKUP. */
	private long until;
	/** When she stops looking for a chair and leaves the party. */
	private long seatDeadline;
	/** Whether the sit in progress is a satisfied one, and so may roll for leaving. */
	private boolean restingFull = false;
	/** Hunger in the dish being eaten, read before it was removed. */
	private int pendingHunger = 0;
	/** Appeal in the same dish; her gift is sized on the running total of these. */
	private int pendingAppeal = 0;
	/** Whether she is away from her chair and owes it a return. */
	private boolean away = false;

	/**
	 * The standable block she is walking to, which is <b>not</b> {@link #target}.
	 *
	 * <p>
	 * The two are different things and conflating them is what made the old reach a
	 * fudge: a chair and a dish are both things she acts on <i>from somewhere else</i>, so
	 * the walk goes to the ground beside them while {@code target} stays the chair or the
	 * dish. Arrival is therefore tested against this, exactly, and the action reach is
	 * tested against {@code target}.
	 */
	@Nullable
	private BlockPos standing;
	/** The chair list's version when she last checked her own chair was still on it. */
	private int layoutVersion = -1;

	/**
	 * Consecutive failures to find a chair, and whether the host has been told.
	 * <p>
	 * Two fields rather than one deadline because the host is told on the <i>first</i>
	 * failure and she leaves on the <i>third</i>. One counter and one flag, because the
	 * message means something different each time: "she is looking for a chair" is
	 * actionable while it is happening, "she gave up" is only true at the end.
	 */
	private int seatFailures = 0;
	private boolean seatWarned = false;

	/**
	 * When the next "is there a chair?" check counts.
	 * <p>
	 * A separate clock from {@link #seatDeadline} because the two answer different
	 * questions: this one paces the attempts, that one is the backstop for the whole
	 * episode. Seeking runs every tick while she has nowhere to be, so without it the
	 * failure counter would fill in a single frame.
	 */
	private long seatCheckAt = 0;

	/**
	 * Whether a serving is in her hand, and when it leaves it.
	 * <p>
	 * The item is emptied part-way through the chew rather than at the end, so the food
	 * visibly does not survive the whole animation. Tracked as a flag rather than assumed
	 * from the phase because {@link #stop()} has to be able to empty her hand whatever
	 * phase she was interrupted in.
	 */
	private boolean holding = false;
	private long untilItemDrop = 0;

	/** Whether the return from picking a dish up is owed, and is to end in the chew. */
	private boolean eatingAfterSeat = false;

	/** When she stands up, and when she is let go of the party regardless. */
	private long leaveAt = 0;
	private long leaveDeadline = 0;
	/** Whether she has actually stood up yet. */
	private boolean leftSeat = false;

	public YoukaiPartyTask() {
		super(Map.of(
				MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
				GLBrains.MEM_PARTY.get(), MemoryStatus.VALUE_PRESENT,
				MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
		));
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, SmartYoukaiEntity entity) {
		var module = entity.getModule(PartyModule.class);
		if (module.isEmpty() || !module.get().partying()) return false;
		return module.get().party(level) != null;
	}

	@Override
	protected void start(ServerLevel level, SmartYoukaiEntity entity, long gameTime) {
		deadline = gameTime + PartyModule.ARRIVE_TIMEOUT;
		target = null;
		until = 0;
		away = false;
		// pendingHunger / pendingAppeal are deliberately NOT cleared here: {@link #stop()}
		// owns them, because it is the one that has to bank a serve that was taken off a
		// dish and never credited. Clearing them in both places would mean whichever ran
		// first won.
		//
		// A task restart is not a new visit. She keeps her chair and everything she has
		// eaten, and only loses her place in the cycle — which is recovered from what she
		// is physically doing, not guessed at. Chunk unload stops this task and the brain
		// starts a fresh one, so guessing would restart every guest at the chair and send
		// them all out for another walk.
		var party = entity.getModule(PartyModule.class).orElseThrow();
		seatDeadline = gameTime + SEAT_FAILURES * SEAT_RETRY;
		seatCheckAt = gameTime;
		// A party that closed while this task was stopped is still a party she has to
		// leave, and the exit is the one thing that must not be re-derived from what she
		// happens to be doing: it is asked for, not inferred.
		if (party.leaving() || party.party(level) == null
				|| party.party(level).phase() == PartyAttachment.Phase.CLOSING) {
			beginLeaving(level, entity, gameTime);
			return;
		}
		if (party.seat() == null) {
			phase = Phase.SEEK_SEAT;
		} else if (entity.isPassenger()) {
			phase = Phase.WAIT_TABLE;
		} else {
			phase = Phase.RETURNING;   // stopped part-way through walking back
			away = true;
		}
	}

	/**
	 * Start the exit: a random wait, then up and away.
	 * <p>
	 * The wait is per guest and drawn here rather than in the task's caller, so a task
	 * restart mid-departure does not re-roll it and send her back to her chair for another
	 * two seconds.
	 */
	private void beginLeaving(ServerLevel level, SmartYoukaiEntity entity, long gameTime) {
		entity.getModule(PartyModule.class).ifPresent(PartyModule::beginLeave);
		if (phase == Phase.LEAVING) return;
		phase = Phase.LEAVING;
		target = null;
		away = false;
		eatingAfterSeat = false;
		leftSeat = false;
		until = 0;
		leaveAt = gameTime + entity.getRandom().nextInt(UNSEAT_DELAY + 1);
		leaveDeadline = gameTime + LEAVE_TIMEOUT;
		deadline = leaveAt + LEAVE_TIMEOUT;
	}

	@Override
	protected boolean canStillUse(ServerLevel level, SmartYoukaiEntity entity, long gameTime) {
		var module = entity.getModule(PartyModule.class);
		if (module.isEmpty()) return false;
		var party = module.get();
		var attachment = party.party(level);
		// the party ended, or its chunk unloaded out from under us
		if (attachment == null) {
			dismiss(entity);
			return false;
		}
		// the party closed: she leaves, whichever phase she happened to be in. Not returned
		// from: the exit is a phase like any other and is stepped like any other, and a
		// guest who returned true here without stepping would stand up and then stand
		// there for ever.
		boolean leaving = party.leaving() || attachment.phase() == PartyAttachment.Phase.CLOSING;
		if (leaving) beginLeaving(level, entity, gameTime);
		// the deadline does not apply to a guest who is on her way out: stepLeaving owns
		// her own clock, and the leg timeout here would write her off mid-walk
		if (!leaving && gameTime > deadline) {
			// She could not walk where she was going. Moved rather than written off: a
			// guest standing in a doorway for the rest of a party is worse than one that
			// blinks into place, and this only fires once the pathfinder has already
			// failed, so the teleport is a repair and not a shortcut.
			if ((phase == Phase.SEEK_SEAT || phase == Phase.RETURNING) && target != null) {
				var spot = PartySend.freeSpaceNear(level, target, 2);
				if (spot == null) {
					dismiss(entity);
					return false;
				}
				if (entity.isPassenger()) entity.stopRiding();
				entity.teleportTo(spot.x, spot.y, spot.z);
				entity.getNavigation().stop();
				entity.navCtrl.stopMoving();
				deadline = gameTime + LEG_TIMEOUT;
			} else {
				dismiss(entity);
				return false;
			}
		}
		// her chair was taken away under her: the layout rescan noticed, and she is
		// standing with nothing to sit on. Back to looking for one, on the same terms a
		// first-time guest gets - if there is no other chair she leaves. Never while she is
		// on her way out: that would put a guest who was asked to go back into looking for
		// a chair instead.
		if (!leaving && layoutVersion >= 0 && layoutVersion != attachment.layoutVersion()) {
			layoutVersion = attachment.layoutVersion();
			var seat = party.seat();
			if (seat != null && !attachment.seats().contains(seat)) {
				party.dropSeat(level);
				if (entity.isPassenger()) entity.stopRiding();
				phase = Phase.SEEK_SEAT;
				target = null;
				away = false;
				seatFailures = 0;
				seatWarned = false;
				seatDeadline = gameTime + SEAT_FAILURES * SEAT_RETRY;
				seatCheckAt = gameTime;
			}
		}
		step(level, entity, party, attachment, gameTime);
		// Deliberately keeps the task running across a phase change instead of returning
		// false for a tick. One Behaviour instance drives the whole visit, so the phase
		// lives in this field; returning false stops the task, stop() wipes the state, and
		// the brain re-runs it next tick — restarting the cycle from the chair every time
		// she changed phase.
		return true;
	}

	private void step(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment, long gameTime) {
		switch (phase) {
			case EATING -> {
				// the serving leaves her hand part-way through the chew, not at the end
				if (holding && gameTime >= untilItemDrop) dropServing(entity);
				if (gameTime < until) return;
				// the dish was removed when eating started; credit it now so her tally and
				// the party's served total move together
				party.eat(pendingHunger, pendingAppeal, attachment);
				pendingHunger = 0;
				pendingAppeal = 0;
				until = 0;
				target = null;
				// back to the chair either way; if she never left it she is already there
				beginRest(level, entity, party);
			}
			case RESTING -> {
				if (gameTime < until) return;
				until = 0;
				// The only place a guest leaves while there is still food. Rolled once
				// per satisfied sit and not on a timer, so a character who sits longer
				// also dawdles about leaving - which is what a slow eater should do, and
				// what makes the two per-character fields mean something together.
				if (restingFull && entity.getRandom().nextDouble() < party.leaveChance()) {
					PartyGift.give(level, entity, party);
					beginLeaving(level, entity, gameTime);
					return;
				}
				restingFull = false;
				phase = Phase.SEEK_FOOD;
			}
			case WAIT_TABLE -> {
				// nobody eats until every guest has a chair
				if (attachment.phase() == PartyAttachment.Phase.FEASTING) phase = Phase.SEEK_FOOD;
				// standing in GATHERING is not her failing to arrive, so the visit timeout
				// is held off while the table fills
				else deadline = gameTime + PartyModule.ARRIVE_TIMEOUT;
			}
			case DELAY -> {
				if (gameTime < until) return;
				until = 0;
				if (target == null || !level.isLoaded(target)) {
					phase = Phase.SEEK_FOOD;   // the dish went under us while she hesitated
					return;
				}
				reach(level, entity, party, attachment);
			}
			case PICKUP -> {
				if (gameTime < until) return;
				until = 0;
				// she is holding it now: back to the chair if she left it to get it, and
				// the chew happens there rather than standing over the plate
				if (away) {
					eatingAfterSeat = true;
					target = null;
					phase = Phase.RETURNING;
				} else {
					startEating(level, entity);
				}
			}
			case LEAVING -> stepLeaving(level, entity, party, gameTime);
			default -> {
				// both halves go together: a destination whose subject has unloaded is no
				// longer a destination, and arriving at a stale floor tile would have her
				// act on a dish on the other side of the room
				if (target != null && !level.isLoaded(target)) {
					target = null;
					standing = null;
				}
				if (target == null) seek(level, entity, party, attachment, gameTime);
				else if (arrived(entity)) arrive(level, entity, party, attachment);
			}
		}
	}

	/**
	 * She is holding the serving: go where she is going to eat it.
	 * <p>
	 * Shared by the two ways a bite starts, because the decision is the same either way —
	 * a dish within reach is taken from where she is standing, and anything further out
	 * is walked to. The difference is entirely upstream, in {@link #DELAY}.
	 */
	private void reach(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment) {
		if (dishInReach(entity, target)) {
			takeServing(level, entity, party);
			return;
		}
		away = true;
		// a passenger does not walk, and the chair she is riding will not go for her
		if (entity.isPassenger()) entity.stopRiding();
		// told once, not once a tick. This case is stepped every tick until she arrives,
		// and re-issuing the walk target that often restarts the pathfinding under her own
		// feet, which is how a walk that would have succeeded ends up timing out.
		if (BrainUtils.getMemory(entity, MemoryModuleType.WALK_TARGET) == null) {
			var ground = PartySend.standingNear(level, target);
			if (ground == null) {
				// nothing to stand on beside this dish, so walk at it and let the leg
				// timeout decide. Reaching for a dish from mid-air is worse than not
				// reaching, but standing still for ever is worse than both.
				walkTo(entity, target.getCenter(), target);
				return;
			}
			walkTo(entity, ground.getCenter(), target);
		}
	}

	/**
	 * The exit: stand up, then go — away from the party, or back to where she came from.
	 * <p>
	 * The unseat and the walk are separate beats because a table that empties inside one
	 * tick reads as a cutscene. Everything after the walk is {@link PartyModule}'s: what
	 * happens to her depends on where she came from, and only she knows that.
	 */
	private void stepLeaving(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			long gameTime) {
		if (gameTime < leaveAt) return;
		if (!leftSeat) {
			leftSeat = true;
			if (entity.isPassenger()) entity.stopRiding();
			party.dropSeat(level);
			clearWalk(entity);
			dropServing(entity);
			deadline = leaveDeadline;
		}
		if (target == null) {
			var dest = destination(level, entity, party);
			if (dest == null) {
				finishLeaving(level, entity);
				return;
			}
			target = dest;
			walkTo(entity, dest.getCenter(), dest);
			return;
		}
		// arriving is enough: she is walking away, not picking anything up, so the exact
		// stand-on-it rule that governs a dish or a chair does not apply to the last few
		// steps of a departure
		if (standing == null || within(entity, standing, DEPART_REACH) || gameTime > leaveDeadline) {
			finishLeaving(level, entity);
		}
	}

	/**
	 * Where she goes when the party is over.
	 * <p>
	 * A summoned clone has nothing to go back to, so she walks off and is gone — anywhere
	 * a little way beyond the table is as good as anywhere else, since there is nothing to
	 * arrive at. A character who came as herself goes back: her home, or her visit.
	 * <p>
	 * A visitor whose window has already closed gets no walk at all. She is not going home
	 * from the party; she is leaving a party, and a long walk to a house that is no longer
	 * having her is a worse ending than being gone from the table in ten seconds.
	 *
	 * @return a destination, or null to finish leaving here and now
	 */
	@Nullable
	private BlockPos destination(ServerLevel level, SmartYoukaiEntity entity, PartyModule party) {
		var table = party.table();
		if (table == null) return null;
		if (party.origin() == PartyModule.Origin.SUMMONED) {
			// out past the guest radius, so she is off the party's books and not merely
			// standing at the edge of it
			var reach = GLModConfig.SERVER.partyGuestRadius.get() + 8;
			var random = entity.getRandom();
			var spot = new BlockPos(table.getX() + random.nextInt(-reach, reach + 1),
					table.getY(), table.getZ() + random.nextInt(-reach, reach + 1));
			var away = PartySend.freeSpaceNear(level, spot, 4);
			return away == null ? null : BlockPos.containing(away);
		}
		var key = party.originKey();
		if (key == null || !level.dimension().equals(key.getDim())) return null;
		if (party.origin() == PartyModule.Origin.VISITING) {
			var visit = entity.getModule(VisitModule.class).orElse(null);
			if (visit == null || visit.remaining(level) <= 0) return null;
		}
		var home = IHomeHolder.of(level, key);
		var center = home == null ? null : home.getWanderCenter();
		return center == null ? key.pos() : center;
	}

	/** Let the module finish the exit: come off the books, then restore or discard. */
	private void finishLeaving(ServerLevel level, SmartYoukaiEntity entity) {
		clearWalk(entity);
		dropServing(entity);
		entity.getModule(PartyModule.class).ifPresent(m -> m.completeLeave(level));
	}

	/**
	 * Choose what to walk to next, or dismiss her if there is nothing left to do.
	 * <p>
	 * Records the destination in {@link #target}, so the next tick {@code step()} checks
	 * arrival instead of seeking again.
	 */
	private void seek(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment, long gameTime) {
		switch (phase) {
			case SEEK_SEAT -> {
				var seat = attachment.claimSeat(level, entity.getUUID(), party.seat());
				if (seat == null) {
					// Counting once per window rather than once per tick: seek runs every
					// tick while there is no destination, and three failures a tick would
					// be three failures in no time at all.
					if (gameTime < seatCheckAt) {
						deadline = gameTime + LEG_TIMEOUT;
						return;
					}
					seatCheckAt = gameTime + SEAT_RETRY;
					// The host is told once, on the first failure, because the message is
					// about a shortage she can still fix: worth saying while it is
					// happening, not only at the end.
					if (!seatWarned) {
						seatWarned = true;
						seatFailures = 0;
						PartyMessage.reportLookingForSeat(level, attachment, entity);
					}
					seatFailures++;
					if (seatFailures >= SEAT_FAILURES || gameTime > seatDeadline) {
						expel(level, entity, party, attachment);
						return;
					}
					// no chair in reach yet: the host may still be putting them out, so
					// keep looking. The deadline goes with it, or the leg timeout would
					// write her off between checks.
					deadline = gameTime + LEG_TIMEOUT;
					return;
				}
				seatFailures = 0;
				seatWarned = false;
				walkToSeat(level, entity, seat);
			}
			case SEEK_FOOD -> {
				var dish = drawDish(level, entity, party, attachment);
				if (dish == null) {
					// nothing of the spread left she can reach, so she settles up and goes
					PartyGift.give(level, entity, party);
					beginLeaving(level, entity, gameTime);
					return;
				}
				target = dish;
				// a dish within reach is eaten from where she sits, without standing up.
				// The deadline is set before this branch, not after it: a guest who never
				// stands up never refreshes it anywhere else, and would be written off
				// part-way through the meal.
				deadline = gameTime + LEG_TIMEOUT;
				// then she hesitates before reaching for it, so a table of guests does
				// not draw, stand and eat in lockstep
				phase = Phase.DELAY;
				until = gameTime + BITE_DELAY_MIN
						+ entity.getRandom().nextInt(BITE_DELAY - BITE_DELAY_MIN + 1);
				return;
			}
			case RETURNING -> {
				var seat = party.seat();
				if (seat == null) {
					finishLeaving(level, entity);
					return;
				}
				walkToSeat(level, entity, seat);
			}
			default -> target = null;
		}
		deadline = gameTime + LEG_TIMEOUT;
	}

	/**
	 * Walk to somewhere she can stand <b>beside</b> {@code seat}, and look at it.
	 *
	 * <p>
	 * Not to the seat, and not to the block above it. The block above is where the rider
	 * ends up, which is inside the chair's own collision box: asking a pathfinder to reach
	 * it either fails outright or parks the guest standing on the seat, which is the one
	 * place a party must never put anyone. Standing on the floor next to the chair is both
	 * reachable and the position she actually sits down from — and arrival is then
	 * measured on that floor tile exactly, so "she got there" and "she is next to her
	 * chair" are the same statement.
	 *
	 * <p>
	 * {@link PartySend#standingNear} has the answer; a chair walled in on every side has
	 * none, and then the old target is still better than not arriving.
	 */
	private void walkToSeat(ServerLevel level, SmartYoukaiEntity entity, BlockPos seat) {
		var beside = PartySend.standingNear(level, seat);
		walkTo(entity, beside == null ? seat.above().getCenter() : beside.getCenter(), seat);
	}

	/**
	 * Send her to {@code to}, and remember {@code look} as the thing being walked to.
	 * <p>
	 * {@code target} is set here rather than at the call sites because {@code step()}
	 * reads it to decide between seeking and arriving: a seek that did not record its
	 * destination would re-issue the walk every tick forever, never arrive, and — for a
	 * chair — never sit down, which would hold the whole party in GATHERING.
	 */
	private void walkTo(SmartYoukaiEntity entity, Vec3 to, BlockPos look) {
		target = look.immutable();
		standing = BlockPos.containing(to);
		BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET, new WalkTarget(to, 1, 1));
		BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET, new BlockPosTracker(look));
	}

	private void arrive(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment) {
		switch (phase) {
			case SEEK_FOOD -> takeServing(level, entity, party);
			case SEEK_SEAT, RETURNING -> sit(level, entity, party, attachment);
			default -> target = null;
		}
	}

	/** Sit down on {@link #target}, which both callers have already reached. */
	private void sit(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment) {
		// a no-op unless the block is one of ours; a third-party chair in party_seat
		// simply leaves her standing beside it, and the claim is still recorded so the
		// party is not held in GATHERING by a guest who will never be seated
		HomeSearchUtil.setSitting(level, target, entity);
		party.claimSeat(target);
		away = false;
		target = null;
		until = 0;
		clearWalk(entity);
		if (eatingAfterSeat) {
			// she came back from picking a dish up away from the table, which is why she
			// was walking: the chew happens here, not standing over the plate
			eatingAfterSeat = false;
			startEating(level, entity);
			return;
		}
		phase = attachment.phase() == PartyAttachment.Phase.FEASTING
				? Phase.SEEK_FOOD : Phase.WAIT_TABLE;
	}

	/**
	 * The pickup beat: take one serving off {@link #target} and put it in her hand.
	 * <p>
	 * The gesture runs first and the item appears when it ends, so what she is holding is
	 * something she was seen reaching for. The serve comes off the block here rather than
	 * when she eventually eats it, so two guests converging on one cake cannot both be
	 * counted for it.
	 * <p>
	 * Requires {@link #target} to be set: every call site is inside a branch that has
	 * already drawn a dish or reached one.
	 */
	private void takeServing(ServerLevel level, SmartYoukaiEntity entity, PartyModule party) {
		var dish = target;
		if (dish == null) {
			beginRest(level, entity, party);
			return;
		}
		// re-validate: another guest may have taken this while she walked over, and a
		// dish is only part of the spread if the table or chair it was put out on is
		// still there
		var state = level.getBlockState(dish);
		if (!PartyFoodValue.isFood(level, dish, state) || !PartyArea.isLaidOut(level, dish, state)) {
			// someone else got there first, or the host broke it up; take the next turn
			target = null;
			beginRest(level, entity, party);
			return;
		}
		// both readings are taken BEFORE consuming — one slice of a cake, one serving of a
		// feast, or one item off a board or a rack where the answer is not in the block
		// state at all and has to be read out of the block entity
		pendingHunger = PartyFoodValue.hungerPerServe(level, dish, state);
		pendingAppeal = PartyFoodValue.appealPerServe(level.registryAccess(), level, dish, state);
		// the stack the handler says one serving is worth: a slice, a bowl, or the pair of
		// sushi a cutting board hands over whole. A copy, because a resolved result is one
		// shared object and the stack in her hand is hers to do what she likes with.
		var serving = servingFor(level, dish, state);
		entity.setItemInHand(InteractionHand.MAIN_HAND, serving);
		holding = !serving.isEmpty();
		entity.swing(InteractionHand.MAIN_HAND);
		YoukaiAnim.USE_MAINHAND.play(entity);
		phase = Phase.PICKUP;
		until = level.getGameTime() + PICKUP_TICKS;
		deadline = level.getGameTime() + LEG_TIMEOUT;
		// one serve only: a cake loses a slice and stays on the table, so the rest of the
		// party can still reach it. false, not true: food eaten at a party is not dropped
		// back onto it
		PartyFoodValue.consume(level, dish, state);
	}

	/**
	 * The stack one serving of this block is worth, or nothing.
	 * <p>
	 * Asked of the handler rather than rebuilt here, which is the point of the food
	 * refactor: a cutting board's serving is two sushi because the board hands over
	 * whole plates, and no amount of reading an id would have told anybody that.
	 * <p>
	 * The first entry that is valid, so a container is handed whichever of its foods the
	 * guest has already picked — the same one whose serve was drawn and consumed.
	 */
	private static ItemStack servingFor(ServerLevel level, BlockPos pos, BlockState state) {
		for (var result : PartyFoodValue.results(level, pos, state)) {
			if (!result.valid()) continue;
			return result.servingCopy();
		}
		return ItemStack.EMPTY;
	}

	/**
	 * The eat beat: seated, chewing, with the serving leaving her hand as she goes.
	 * <p>
	 * The sound belongs here rather than at the pickup, because this is the moment she
	 * eats rather than the moment she picks the plate up.
	 */
	private void startEating(ServerLevel level, SmartYoukaiEntity entity) {
		phase = Phase.EATING;
		until = level.getGameTime() + EAT_TICKS;
		// emptied part-way through rather than at the end, so the food visibly does not
		// survive the whole chew
		untilItemDrop = level.getGameTime() + EAT_TICKS / 2;
		entity.broadcastEat();
		level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EAT,
				SoundSource.PLAYERS, 0.8F, 1.0F);
	}

	/** Put down whatever she is carrying, if anything. */
	private static void dropServing(SmartYoukaiEntity entity) {
		if (!entity.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
			entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
	}

	/**
	 * Go back to the chair if she left it, then sit for a while.
	 * <p>
	 * The wait is longer once she has eaten her fill: a hungry guest is back for the next
	 * course, a satisfied one is only grazing and takes her time over it.
	 * <p>
	 * The deadline is pushed out here too, because a satisfied sit is longer than the gap
	 * between two seeks once a dish has been walked to and back from.
	 */
	private void beginRest(ServerLevel level, SmartYoukaiEntity entity, PartyModule party) {
		deadline = level.getGameTime() + LEG_TIMEOUT;
		restingFull = party.satisfied();
		if (away) {
			phase = Phase.RETURNING;
			return;
		}
		phase = Phase.RESTING;
		until = level.getGameTime() + (restingFull ? party.fullSeat() : party.hungrySeat());
	}

	/**
	 * One dish, drawn at random with probability proportional to its appeal per serve.
	 * <p>
	 * Random rather than nearest, and weighted rather than uniform, so a guest works her
	 * way across the spread instead of clearing the nearest plate first and working
	 * outwards — and so the tasty things get eaten before the plain ones.
	 * <p>
	 * The appeal total is accumulated in {@code long}: one dish is capped by
	 * {@code MAX_APPEAL}, but this sums a whole search box of them, and an overflow would
	 * wrap into a bound of the wrong sign and skew every draw after it.
	 */
	@Nullable
	private BlockPos drawDish(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment) {
		var access = level.registryAccess();
		var dishes = new ArrayList<BlockPos>();
		var weights = new ArrayList<Integer>();
		long total = 0;
		// the party's own list, rebuilt every couple of seconds rather than swept per
		// guest: this runs once per dish, for the length of the visit, and the party
		// already has a fresher answer than a sweep here could produce
		for (var pos : attachment.food()) {
			var state = level.getBlockState(pos);
			// re-checked, not trusted: the cache is two seconds old, and in those two
			// seconds another guest may have emptied this and the host may have moved it
			if (!PartyArea.isLaidOut(level, pos, state)) continue;
			if (!PartyFoodValue.isFood(level, pos, state)) continue;
			int appeal = PartyFoodValue.appealPerServe(access, level, pos, state);
			if (appeal <= 0) continue;
			dishes.add(pos.immutable());
			weights.add(appeal);
			total += appeal;
		}
		if (total <= 0) return null;
		// RandomSource has no bounded nextLong, and casting a double keeps the bound off
		// the int range entirely: nextDouble() is [0,1), so the roll lands in [0,total).
		long roll = (long) (entity.getRandom().nextDouble() * total);
		for (int i = 0; i < dishes.size(); i++) {
			roll -= weights.get(i);
			if (roll < 0) return dishes.get(i);
		}
		return dishes.getLast();
	}

	/** Whether {@code pos} is within {@code blocks} of her, measured to the block's box. */
	private static boolean within(SmartYoukaiEntity entity, BlockPos pos, double blocks) {
		return new AABB(pos).distanceToSqr(entity.position()) <= blocks * blocks;
	}

	/**
	 * Whether she has got where she is going: standing on the block she was sent to, with
	 * no tolerance at all.
	 * <p>
	 * {@link #standing} rather than {@link #target}, because she never walks to the thing
	 * she is going to act on — she walks to the floor beside it. Testing arrival against
	 * the chair or the dish instead is how a guest ends up acting on a dish from across
	 * the table and calling it "close enough".
	 */
	private boolean arrived(SmartYoukaiEntity entity) {
		return standing != null && within(entity, standing, ARRIVAL_REACH);
	}

	/**
	 * Whether she can take {@code dish} from where she is right now.
	 *
	 * <p>
	 * Three blocks from the chair and two from the floor, and the difference is the whole
	 * reason a laid banquet plays out without anyone standing up: a dish within three
	 * blocks of her seat is taken from where she is sitting, and only a dish further out
	 * than that costs a dismount and a walk.
	 */
	private static boolean dishInReach(SmartYoukaiEntity entity, BlockPos dish) {
		return within(entity, dish, entity.isPassenger() ? REACH_SEATED : REACH_STANDING);
	}

	/**
	 * Give up on the party because there is nowhere to sit.
	 * <p>
	 * No gift: she never ate anything, and a guest carried in on the roster who found no
	 * chair is not one the host owes anything to. Saying so is the point, though - the
	 * host put out chairs and they were not enough, or one too many guests were invited,
	 * and without a word the only visible sign is a table with fewer people at it than
	 * the letter promised.
	 */
	private static void expel(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment) {
		PartyMessage.reportEvicted(level, attachment, entity);
		dismiss(entity);
	}

	private static void dismiss(SmartYoukaiEntity entity) {
		BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
		BrainUtils.clearMemory(entity, MemoryModuleType.LOOK_TARGET);
		dropServing(entity);
		entity.getModule(PartyModule.class).ifPresent(PartyModule::dismiss);
	}

	private void clearWalk(SmartYoukaiEntity entity) {
		BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
		BrainUtils.clearMemory(entity, MemoryModuleType.LOOK_TARGET);
		standing = null;
	}

	@Override
	protected void stop(ServerLevel level, SmartYoukaiEntity entity, long gameTime) {
		// A dish destroyed when eating started but never credited — the party ended, or
		// the chunk unloaded, mid-bite — still counts. The block is already gone, so
		// dropping the tally would silently lose that food.
		if (pendingHunger > 0 || pendingAppeal > 0) {
			var module = entity.getModule(PartyModule.class);
			if (module.isPresent()) {
				var attachment = module.get().party(level);
				if (attachment != null) module.get().eat(pendingHunger, pendingAppeal, attachment);
			}
		}
		clearWalk(entity);
		// whatever she was carrying goes back where it came from: a task that stops
		// mid-bite would otherwise leave a slice of cake in her hand until she died
		dropServing(entity);
		target = null;
		pendingHunger = 0;
		pendingAppeal = 0;
		holding = false;
		eatingAfterSeat = false;
		until = 0;
	}

}