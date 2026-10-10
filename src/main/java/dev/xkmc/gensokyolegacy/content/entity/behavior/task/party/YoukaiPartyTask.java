package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeSearchUtil;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyArea;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodValue;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyGift;
import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A guest's whole visit: find a chair, wait for the table to fill, then eat her way
 * through the spread until there is none of it left within reach.
 * <p>
 * The shape is a cycle, not walk-then-finish. Each round she draws a dish weighted by its
 * appeal, eats one serving of it, goes back to her chair and sits for a while before the
 * next. How long that sit is depends on whether she has eaten her fill:
 * {@link #HUNGRY_REST} while she still needs the food, {@link #FULL_REST} once she has
 * had enough and is only grazing. She never leaves on being satisfied — the visit ends
 * when the host's food does.
 * <p>
 * Most rounds she does not move at all. A dish within {@link #REACH} of where she sits is
 * eaten from her chair, which is the common case at a laid table and the reason the reach
 * exists. Only a dish further out than that sends her across the room and back.
 * <p>
 * Sitting means <em>riding</em> a {@code ChairEntity} (see {@code HomeSearchUtil.setSitting}),
 * and a passenger does not walk. So leaving a chair is an explicit dismount, and every
 * departure owes a return — otherwise the second trip out is measured from wherever the
 * first one left her.
 * <p>
 * There is no eating animation in the codebase — {@code YoukaiAnim} has no EAT slot and
 * nothing calls {@code startUsingItem} on a character — so eating is a main-hand swing plus
 * the vanilla eating sound, the same treatment {@code MarisaForageTask} gives foraging.
 */
public class YoukaiPartyTask extends Behavior<SmartYoukaiEntity> {

	/** Ticks spent eating one dish. */
	private static final int EAT_TICKS = 40;
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
	/** How often a missing chair is re-checked, in ticks. */
	private static final int SEAT_RETRY = 40;

	/** How close to a dish counts as reaching it, squared. Three blocks. */
	private static final double REACH_SQR = 9.0;

	/** Sitting time between dishes while she is still hungry: one second. */
	private static final int HUNGRY_REST = 20;
	/** Sitting time between dishes once she is satisfied: five seconds. */
	private static final int FULL_REST = 100;

	/**
	 * How far around the table she looks for a chair, and for a dish to draw.
	 * <p>
	 * Fixed, and much smaller than the party scan distance. The scan walks a whole room
	 * once, for the party; this is walked per guest, once per dish, for the length of the
	 * visit — and a guest is not going to walk tens of blocks for another helping anyway,
	 * she eats what is at her table and takes what she can reach.
	 */
	private static final int SEARCH_RADIUS = 6;

	private enum Phase {
		/** Walking to a chair. */
		SEEK_SEAT,
		/** Seated, waiting for the rest of the table to be seated. */
		WAIT_TABLE,
		/** Drawing the next dish. */
		SEEK_FOOD,
		/** Eating it. */
		EATING,
		/** Walking back to her chair, having eaten away from it. */
		RETURNING,
		/** Sitting it out before the next dish. */
		RESTING
	}

	private Phase phase = Phase.SEEK_SEAT;
	@Nullable
	private BlockPos target;
	private long deadline;
	/** Deadline of the current wait, for EATING and RESTING. */
	private long until;
	/** Hunger in the dish being eaten, read before it was removed. */
	private int pendingHunger = 0;
	/** Appeal in the same dish; her gift is sized on the running total of these. */
	private int pendingAppeal = 0;
	/** Whether she is away from her chair and owes it a return. */
	private boolean away = false;

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
		if (party.seat() == null) {
			phase = Phase.SEEK_SEAT;
		} else if (entity.isPassenger()) {
			phase = Phase.WAIT_TABLE;
		} else {
			phase = Phase.RETURNING;   // stopped part-way through walking back
			away = true;
		}
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
		if (gameTime > deadline) {
			dismiss(entity);
			return false;
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
				phase = Phase.SEEK_FOOD;
			}
			case WAIT_TABLE -> {
				// nobody eats until every guest has a chair
				if (attachment.phase() == PartyAttachment.Phase.FEASTING) phase = Phase.SEEK_FOOD;
				// standing in GATHERING is not her failing to arrive, so the visit timeout
				// is held off while the table fills
				else deadline = gameTime + PartyModule.ARRIVE_TIMEOUT;
			}
			default -> {
				if (target != null && !level.isLoaded(target)) target = null;
				if (target == null) seek(level, entity, party, gameTime);
				else if (inReach(entity, target)) arrive(level, entity, party, attachment);
			}
		}
	}

	/**
	 * Choose what to walk to next, or dismiss her if there is nothing left to do.
	 * <p>
	 * Records the destination in {@link #target}, so the next tick {@code step()} checks
	 * arrival instead of seeking again.
	 */
	private void seek(ServerLevel level, SmartYoukaiEntity entity, PartyModule party, long gameTime) {
		switch (phase) {
			case SEEK_SEAT -> {
				var seat = nearestSeat(level, entity, party.table());
				if (seat == null) {
					// no chair in reach yet: the player may still be placing them, so keep
					// the guest alive and look again shortly
					if (gameTime % SEAT_RETRY == 0) deadline = gameTime + PartyModule.ARRIVE_TIMEOUT;
					return;
				}
				walkTo(entity, seat.above().getCenter(), seat);
			}
			case SEEK_FOOD -> {
				var dish = drawDish(level, entity, party);
				if (dish == null) {
					// nothing of the spread left she can reach, so she settles up and goes
					PartyGift.give(level, entity, party);
					dismiss(entity);
					return;
				}
				target = dish;
				// a dish within reach is eaten from where she sits, without standing up.
				// The deadline is set before this branch, not after it: a guest who never
				// stands up never refreshes it anywhere else, and would be written off
				// part-way through the meal.
				deadline = gameTime + LEG_TIMEOUT;
				if (inReach(entity, dish)) {
					beginEating(level, entity);
					return;
				}
				away = true;
				// a passenger does not walk, and the chair she is riding will not go for her
				if (entity.isPassenger()) entity.stopRiding();
				walkTo(entity, dish.getCenter(), dish);
			}
			case RETURNING -> {
				var seat = party.seat();
				if (seat == null) {
					dismiss(entity);
					return;
				}
				walkTo(entity, seat.above().getCenter(), seat);
			}
			default -> target = null;
		}
		deadline = gameTime + LEG_TIMEOUT;
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
		BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET, new WalkTarget(to, 1, 1));
		BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET, new BlockPosTracker(look));
	}

	private void arrive(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment) {
		switch (phase) {
			case SEEK_FOOD -> beginEating(level, entity);
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
		phase = attachment.phase() == PartyAttachment.Phase.FEASTING
				? Phase.SEEK_FOOD : Phase.WAIT_TABLE;
	}

	/**
	 * Take one serving off {@link #target} and start eating it.
	 * <p>
	 * Requires {@link #target} to be set: both call sites are inside a branch that has
	 * already drawn or arrived at a dish.
	 */
	private void beginEating(ServerLevel level, SmartYoukaiEntity entity) {
		var dish = target;
		// re-validate: another guest may have taken this while she walked over, and a
		// dish is only part of the spread if the table or chair it was put out on is
		// still there
		var state = level.getBlockState(dish);
		if (!PartyFoodValue.isFood(state) || !PartyArea.isLaidOut(level, dish)) {
			// someone else got there first, or the host broke it up; take the next turn
			target = null;
			beginRest(level, entity, entity.getModule(PartyModule.class).orElseThrow());
			return;
		}
		// both readings are taken from the state BEFORE consuming — one slice of a cake,
		// one serving of a feast
		pendingHunger = PartyFoodValue.hungerPerServe(state);
		pendingAppeal = PartyFoodValue.appealPerServe(level.registryAccess(), state);
		phase = Phase.EATING;
		until = level.getGameTime() + EAT_TICKS;
		entity.swing(InteractionHand.MAIN_HAND);
		YoukaiAnim.USE_MAINHAND.play(entity);
		level.playSound(null, dish, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 1.0F);
		// one serve only: a cake loses a slice and stays on the table, so the rest of the
		// party can still reach it. false, not true: food eaten at a party is not dropped
		// back onto it
		PartyFoodValue.consume(level, dish, state);
	}

	/**
	 * Go back to the chair if she left it, then sit for a while.
	 * <p>
	 * The wait is longer once she has eaten her fill: a hungry guest is back for the next
	 * course, a satisfied one is only grazing and takes her time over it.
	 * <p>
	 * The deadline is pushed out here too, because {@link #FULL_REST} is longer than the
	 * gap between two seeks once a dish has been walked to and back from.
	 */
	private void beginRest(ServerLevel level, SmartYoukaiEntity entity, PartyModule party) {
		deadline = level.getGameTime() + LEG_TIMEOUT;
		if (away) {
			phase = Phase.RETURNING;
			return;
		}
		phase = Phase.RESTING;
		until = level.getGameTime() + (party.satisfied() ? FULL_REST : HUNGRY_REST);
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
	private BlockPos drawDish(ServerLevel level, SmartYoukaiEntity entity, PartyModule party) {
		var table = party.table();
		if (table == null) return null;
		var access = level.registryAccess();
		var dishes = new ArrayList<BlockPos>();
		var weights = new ArrayList<Integer>();
		long total = 0;
		for (var pos : BlockPos.betweenClosed(
				table.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS),
				table.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS))) {
			if (!PartyArea.isLaidOut(level, pos)) continue;
			var state = level.getBlockState(pos);
			// worthless food is skipped: crossing the room and eating it would cost her a
			// whole cycle for no fulfillment at all
			if (!PartyFoodValue.isFood(state) || !PartyFoodValue.result(state).valid()) continue;
			int appeal = PartyFoodValue.appealPerServe(access, state);
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

	/** Whether she can act on {@code pos} from where she is standing or sitting. */
	private static boolean inReach(SmartYoukaiEntity entity, BlockPos pos) {
		return entity.distanceToSqr(pos.getCenter()) <= REACH_SQR;
	}

	/** The nearest chair in the tag, searching outward from the table. */
	@Nullable
	private static BlockPos nearestSeat(ServerLevel level, SmartYoukaiEntity entity, @Nullable BlockPos table) {
		if (table == null) return null;
		BlockPos from = entity.blockPosition();
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (var pos : BlockPos.betweenClosed(
				table.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS),
				table.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS))) {
			if (!level.getBlockState(pos).is(GLTagGen.PARTY_SEAT)) continue;
			double dist = pos.distSqr(from);
			if (dist >= bestDist) continue;
			bestDist = dist;
			best = pos.immutable();
		}
		return best;
	}

	private static void dismiss(SmartYoukaiEntity entity) {
		clearWalk(entity);
		entity.getModule(PartyModule.class).ifPresent(PartyModule::dismiss);
	}

	private static void clearWalk(SmartYoukaiEntity entity) {
		BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
		BrainUtils.clearMemory(entity, MemoryModuleType.LOOK_TARGET);
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
		target = null;
		pendingHunger = 0;
		pendingAppeal = 0;
		until = 0;
	}

}