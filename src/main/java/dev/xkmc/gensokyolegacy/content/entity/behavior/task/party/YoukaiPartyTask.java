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
 * through the spread, and go when the party ends or the food does. Sitting means riding a
 * {@code ChairEntity}, so every departure from a chair owes a return.
 *
 * <p>
 * One {@code Behavior} drives all of it and must never return {@code false} from
 * {@link #canStillUse()} to change phase: {@code false} stops the instance and wipes the
 * phase field. Every phase change keeps returning {@code true}.
 *
 * @see doc/design/party/guest.md
 */
public class YoukaiPartyTask extends Behavior<SmartYoukaiEntity> {

	/** Ticks spent eating one dish. */
	private static final int EAT_TICKS = 40;

	/** Length of the {@code 使用主手物品} clip — the same number FairyCakeTask times against. */
	private static final int PICKUP_TICKS = 6;

	/** Random per bite, so a table of guests does not eat in lockstep. */
	private static final int BITE_DELAY = 40;
	private static final int BITE_DELAY_MIN = 10;

	/**
	 * Refreshed at every seek <em>and</em> on sitting down to rest, so it covers a whole
	 * round trip — a guest eating from her chair never walks and would never refresh it.
	 */
	private static final int LEG_TIMEOUT = 200;

	/** How often a missing chair is re-checked. */
	private static final int SEAT_RETRY = 60;

	/** Three failures apart hold the table out of the feast for about nine seconds. */
	private static final int SEAT_FAILURES = 3;

	/** Staggered per guest so the table does not empty inside one tick. */
	private static final int UNSEAT_DELAY = 60;

	private static final int WALK_OUT_SPREAD = 32;
	private static final int WALK_OUT_TRIES = 8;

	/** A guest who cannot reach her destination is not stuck holding a chair for ever. */
	private static final int LEAVE_TIMEOUT = 200;

	/**
	 * Reach slack, not arm length — {@link PartySend#standingNear} has already put her
	 * beside the dish. All three are measured to the block's <b>box</b>, not its centre, so
	 * standing on the block beside a dish is zero blocks from it.
	 *
	 * <p>
	 * The chair's is wider than the walk tolerance on purpose: arriving and sitting are
	 * two steps, the second being {@code setSitting} finding a {@code ChairEntity}.
	 */
	private static final double REACH_SEATED = 3;
	private static final double REACH_STANDING = 2;
	private static final double SEAT_REACH = 3;

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
		/** Holding the dish she just drew: its two ends do different things, so it is its own phase. */
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

	/** The standable block she is walking to, which is <b>not</b> {@link #target}: arrival is tested against this, the action reach against that. */
	@Nullable
	private BlockPos standing;
	/** The chair list's version when she last checked her own chair was still on it. */
	private int layoutVersion = -1;

	/** The host is told on the first failure, she leaves on the third — two different messages. */
	private int seatFailures = 0;
	private boolean seatWarned = false;

	/** Paces the attempts; {@link #seatDeadline} is the backstop for the whole episode. */
	private long seatCheckAt = 0;

	/** Tracked as a flag, not inferred from the phase, so {@link #stop()} can empty her hand in any phase. */
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
		// owns them, because it must bank a serve taken off a dish and never credited.
		// A restart is not a new visit — she keeps her chair and her tally, and only loses
		// her place in the cycle, which is recovered from what she is physically doing.
		var party = entity.getModule(PartyModule.class).orElseThrow();
		seatDeadline = gameTime + SEAT_FAILURES * SEAT_RETRY;
		seatCheckAt = gameTime;
		// The exit is asked for, never inferred: a party that closed while this task was
		// stopped is still one she has to leave.
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

	/** The wait is drawn here, so a restart mid-departure does not re-roll it. */
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
		// from: a guest who returned true here without stepping would stand up and stand there.
		boolean leaving = party.leaving() || attachment.phase() == PartyAttachment.Phase.CLOSING;
		if (leaving) beginLeaving(level, entity, gameTime);
		// the deadline does not apply on the way out: stepLeaving owns her own clock
		if (!leaving && gameTime > deadline) {
			// A repair, not a shortcut: the pathfinder has already failed, and a guest
			// standing in a doorway for the rest of a party is worse than one that blinks in.
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
				// she has arrived, wherever that turned out to be — otherwise the arrival test never
				// passes and she is teleported again on every timeout.
				standing = BlockPos.containing(spot);
				deadline = gameTime + LEG_TIMEOUT;
			} else {
				dismiss(entity);
				return false;
			}
		}
		// her chair was taken away under her: back to looking for one on a first-time guest's
		// terms. Never while leaving, which would send her back to looking instead.
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
		// Keeps running across a phase change: returning false would stop() the task, wipe
		// the phase field, and restart her cycle from the chair.
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
				// The only place a guest leaves while food remains. Rolled per satisfied
				// sit, not on a timer, so the two per-character fields mean something together.
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

	/** Shared by the two ways a bite starts: a dish in reach is taken where she stands, one further out is walked to. */
	private void reach(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment) {
		if (dishInReach(entity, target)) {
			takeServing(level, entity, party);
			return;
		}
		// Still airborne: no order is given, so she falls instead of flying, and the leg clock
		// waits with her.
		if (!canWalk(entity)) {
			deadline = level.getGameTime() + LEG_TIMEOUT;
			return;
		}
		away = true;
		// a passenger does not walk, and the chair she is riding will not go for her
		if (entity.isPassenger()) entity.stopRiding();
		// Told once, not once a tick: re-issuing that often restarts the pathfinding under
		// her own feet, which is how a walk that would have succeeded ends up timing out.
		if (BrainUtils.getMemory(entity, MemoryModuleType.WALK_TARGET) == null) {
			var ground = PartySend.standingNear(level, target);
			if (ground == null) {
				// nothing to stand on beside it: walk at it and let the leg timeout decide,
				// because standing still for ever is worse than reaching badly
				walkTo(entity, target.getCenter(), target);
				return;
			}
			walkTo(entity, ground.getCenter(), target);
		}
	}

	/**
	 * Unseat and walk are separate beats: a table that empties inside one tick reads as a
	 * cutscene. Everything after the walk is {@link PartyModule}'s — only she knows where
	 * she came from.
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
			// airborne: no order, and the leg clock held off, or a guest who fell from
			// above the table would be written off before she landed
			if (!canWalk(entity)) {
				deadline = gameTime + LEG_TIMEOUT;
				return;
			}
			target = dest;
			walkTo(entity, dest.getCenter(), dest);
			return;
		}
		// arriving is enough: she is walking away, not picking anything up, so the exact
		// stand-on-it rule for a dish or a chair does not apply to a departure
		if (standing == null || within(entity, standing, DEPART_REACH) || gameTime > leaveDeadline) {
			finishLeaving(level, entity);
		}
	}

	/**
	 * A summoned clone walks off and is gone; a character who came as herself goes back.
	 * A visitor whose window already closed gets no walk — she is leaving a party, not
	 * going home.
	 *
	 * @return a destination, or null to finish leaving here and now
	 */
	@Nullable
	private BlockPos destination(ServerLevel level, SmartYoukaiEntity entity, PartyModule party) {
		var table = party.table();
		if (table == null) return null;
		if (party.origin() == PartyModule.Origin.SUMMONED) return walkOut(level, entity, table);
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

	/**
	 * Somewhere a summoned guest walks off to before she is put away. The band starts past
	 * the guest radius and is checked twice — on the rolled spot and on the ground found
	 * for it — because an outward search from a too-close roll can return a neighbouring
	 * block and undo the point.
	 *
	 * @return somewhere to walk to, or null when nothing that far out is walkable
	 */
	@Nullable
	private BlockPos walkOut(ServerLevel level, SmartYoukaiEntity entity, BlockPos table) {
		int min = GLModConfig.SERVER.partyGuestRadius.get() + 12;
		int max = min + WALK_OUT_SPREAD;
		var random = entity.getRandom();
		for (int attempt = 0; attempt < WALK_OUT_TRIES; attempt++) {
			var spot = new BlockPos(table.getX() + random.nextInt(-max, max + 1),
					table.getY(), table.getZ() + random.nextInt(-max, max + 1));
			if (!farEnough(spot, table, min)) continue;
			var away = PartySend.freeSpaceNear(level, spot, 4);
			if (away == null) continue;
			var dest = BlockPos.containing(away);
			if (farEnough(dest, table, min)) return dest;
		}
		return null;
	}

	private static boolean farEnough(BlockPos dest, BlockPos table, int min) {
		long dx = (long) dest.getX() - table.getX();
		long dz = (long) dest.getZ() - table.getZ();
		return dx * dx + dz * dz >= (long) min * min;
	}

	/** Let the module finish the exit: come off the books, then restore or discard. */
	private void finishLeaving(ServerLevel level, SmartYoukaiEntity entity) {
		clearWalk(entity);
		dropServing(entity);
		entity.getModule(PartyModule.class).ifPresent(m -> m.completeLeave(level));
	}

	/**
	 * Choose what to walk to next, or dismiss her if there is nothing left to do. Records
	 * it in {@link #target}, so the next tick checks arrival instead of seeking again.
	 */
	private void seek(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment, long gameTime) {
		switch (phase) {
			case SEEK_SEAT -> {
				var seat = attachment.claimSeat(level, entity.getUUID(), party.seat());
				if (seat == null) {
					// Once per window, not once per tick: seek runs every tick while
					// there is no destination
					if (gameTime < seatCheckAt) {
						deadline = gameTime + LEG_TIMEOUT;
						return;
					}
					seatCheckAt = gameTime + SEAT_RETRY;
					// told once, on the first failure: the message is about a shortage
					// the host can still fix
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
					// no chair in reach yet: keep looking, and push the deadline out
					// with it or the leg timeout writes her off between checks
					deadline = gameTime + LEG_TIMEOUT;
					return;
				}
				seatFailures = 0;
				seatWarned = false;
				// airborne: she waits, and so does the leg clock
				if (canWalk(entity)) walkToSeat(level, entity, seat);
				else deadline = gameTime + LEG_TIMEOUT;
			}
			case SEEK_FOOD -> {
				var dish = drawDish(level, entity, party, attachment);
				if (dish == null) {
					// nothing of the spread left she can reach: settle up and go
					PartyGift.give(level, entity, party);
					beginLeaving(level, entity, gameTime);
					return;
				}
				target = dish;
				// a dish within reach is eaten from where she sits, without standing up.
				// The deadline is set before this branch, not after it: a guest who never
				// only standing up refreshes it anywhere else
				deadline = gameTime + LEG_TIMEOUT;
				// then she hesitates, so a table of guests does not eat in lockstep
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
				if (canWalk(entity)) walkToSeat(level, entity, seat);
			}
			default -> target = null;
		}
		deadline = gameTime + LEG_TIMEOUT;
	}

	/**
	 * Walk to somewhere she can stand <b>beside</b> {@code seat}, and look at it — never to
	 * the block above, which is inside the chair's own collision box and is the one place a
	 * party must never put anyone. A chair walled in on every side has no answer, and the
	 * old target is then still better than not arriving.
	 */
	private void walkToSeat(ServerLevel level, SmartYoukaiEntity entity, BlockPos seat) {
		var beside = PartySend.standingNear(level, seat);
		walkTo(entity, beside == null ? seat.above().getCenter() : beside.getCenter(), seat);
	}

	/**
	 * Send her to {@code to}, and remember {@code look} as the thing being walked to. Set
	 * here rather than at the call sites because {@code step()} reads it to choose between
	 * seeking and arriving: a seek that did not record its destination would re-issue the
	 * walk every tick and never sit down.
	 */
	private void walkTo(SmartYoukaiEntity entity, Vec3 to, BlockPos look) {
		target = look.immutable();
		standing = BlockPos.containing(to);
		BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET, new WalkTarget(to, 1, 1));
		BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET, new BlockPosTracker(look));
	}

	/**
	 * Whether she is in a state to start walking at all — <b>not while she is off the
	 * ground</b>, because a pathfinding order issued mid-air is answered by the fly control
	 * and she <i>flies</i> to the party. Tested with {@code isCloseToGround()} rather than
	 * {@code onGround()}: a walk node wants solid ground, so {@code onGround} is exactly the
	 * condition that leaves a hovering character unable to path anywhere.
	 */
	private static boolean canWalk(SmartYoukaiEntity entity) {
		return entity.onGround() || entity.navCtrl.isCloseToGround();
	}

	private void arrive(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment) {
		switch (phase) {
			case SEEK_FOOD -> takeServing(level, entity, party);
			case SEEK_SEAT, RETURNING -> sit(level, entity, party, attachment);
			default -> target = null;
		}
	}

	/**
	 * Sit down on {@link #target}, if she is close enough to. Out of reach she does not
	 * claim it and tries again next tick: {@code setSitting} is a silent no-op on a block
	 * that is not one of ours, and a claim on a chair she is not on holds the party in
	 * GATHERING for ever.
	 */
	private void sit(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment) {
		if (!within(entity, target, SEAT_REACH)) return;
		// a no-op unless the block is one of ours; a third-party chair in party_seat
		// leaves her standing beside it, and the claim is still recorded so the party is
		// not held in GATHERING by a guest who will never be seated
		HomeSearchUtil.setSitting(level, target, entity);
		party.claimSeat(target);
		away = false;
		target = null;
		until = 0;
		clearWalk(entity);
		if (eatingAfterSeat) {
			// she was walking because she picked the dish up away from the table: the
			// chew happens here, not standing over the plate
			eatingAfterSeat = false;
			startEating(level, entity);
			return;
		}
		phase = attachment.phase() == PartyAttachment.Phase.FEASTING
				? Phase.SEEK_FOOD : Phase.WAIT_TABLE;
	}

	/**
	 * The pickup beat: take one serving off {@link #target} and put it in her hand. The
	 * serve comes off the block here, not when she eats, so two guests converging on one
	 * cake cannot both be counted for it.
	 */
	private void takeServing(ServerLevel level, SmartYoukaiEntity entity, PartyModule party) {
		var dish = target;
		if (dish == null) {
			beginRest(level, entity, party);
			return;
		}
		// re-validate: another guest may have taken this while she walked over, and a
		// dish counts only if the table or chair it was put out on is
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
	 * The first valid entry, so a container is handed whichever of its foods the guest
	 * already picked.
	 */
	private static ItemStack servingFor(ServerLevel level, BlockPos pos, BlockState state) {
		for (var result : PartyFoodValue.results(level, pos, state)) {
			if (!result.valid()) continue;
			return result.servingCopy();
		}
		return ItemStack.EMPTY;
	}

	/** The eat beat: seated, chewing, with the serving leaving her hand as she goes. */
	private void startEating(ServerLevel level, SmartYoukaiEntity entity) {
		phase = Phase.EATING;
		until = level.getGameTime() + EAT_TICKS;
		// emptied part-way through, so the food visibly does not survive the whole chew
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
	 * Go back to the chair if she left it, then sit for a while — longer once she has eaten
	 * her fill. The deadline goes out with it: a satisfied sit outlasts the gap between
	 * two seeks.
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
	 * One dish, drawn at random with probability proportional to its appeal per serve — so a
	 * guest works across the spread rather than clearing the nearest plate first.
	 * <p>
	 * The total is accumulated in {@code long}: this sums a whole box of dishes, and an
	 * overflow would skew every draw after it.
	 */
	@Nullable
	private BlockPos drawDish(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment) {
		var access = level.registryAccess();
		var dishes = new ArrayList<BlockPos>();
		var weights = new ArrayList<Integer>();
		long total = 0;
		// the party's own list, not a sweep per guest: this runs once per dish for the
		// whole visit, and the party already has a fresher answer
		for (var pos : attachment.food()) {
			var state = level.getBlockState(pos);
			// re-checked, not trusted: the cache is two seconds old, and in those two
			// seconds another guest may have emptied this
			if (!PartyArea.isLaidOut(level, pos, state)) continue;
			if (!PartyFoodValue.isFood(level, pos, state)) continue;
			int appeal = PartyFoodValue.appealPerServe(access, level, pos, state);
			if (appeal <= 0) continue;
			dishes.add(pos.immutable());
			weights.add(appeal);
			total += appeal;
		}
		if (total <= 0) return null;
		// nextDouble() is [0,1), so the roll lands in [0,total)
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
	 * Whether she is standing on the block she was sent to, with no tolerance at all.
	 * {@link #standing}, not {@link #target}: she walks to the floor beside the thing she
	 * acts on, and testing arrival against the chair or the dish lets her act on a dish
	 * from across the table.
	 */
	private boolean arrived(SmartYoukaiEntity entity) {
		return standing != null && within(entity, standing, ARRIVAL_REACH);
	}

	/**
	 * Whether she can take {@code dish} from where she is. Three blocks from the chair and
	 * two from the floor: a dish within three of her seat is taken from where she is sitting,
	 * and only a dish further out costs a dismount and a walk.
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
		// A dish taken when eating started but never credited still counts: the block is
		// already gone, so dropping the tally would silently lose that food.
		if (pendingHunger > 0 || pendingAppeal > 0) {
			var module = entity.getModule(PartyModule.class);
			if (module.isPresent()) {
				var attachment = module.get().party(level);
				if (attachment != null) module.get().eat(pendingHunger, pendingAppeal, attachment);
			}
		}
		clearWalk(entity);
		// otherwise a task stopping mid-bite leaves a slice of cake in her hand for good
		dropServing(entity);
		target = null;
		pendingHunger = 0;
		pendingAppeal = 0;
		holding = false;
		eatingAfterSeat = false;
		until = 0;
	}

}