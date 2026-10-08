package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeSearchUtil;
import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.party.core.PartyFoodValue;
import dev.xkmc.gensokyolegacy.content.party.core.PartyGift;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A guest's whole visit: claim a seat, walk to the food, eat until satisfied,
 * leave a gift, go home.
 * <p>
 * Follows the walk-then-act-on-a-deadline shape the other house tasks use. The
 * only state here is which position she is heading at and how long she has left
 * to get there; the eaten tally and the party's served total live on
 * {@link PartyModule} and the party's attachment.
 * <p>
 * There is no eating animation in the codebase — {@code YoukaiAnim} has no EAT
 * slot and nothing calls {@code startUsingItem} on a character — so eating is a
 * main-hand swing plus the vanilla eating sound, the same treatment
 * {@code MarisaForageTask} gives foraging.
 */
public class YoukaiPartyTask extends Behavior<SmartYoukaiEntity> {

	/** Ticks spent eating one dish. */
	private static final int EAT_TICKS = 40;
	/** How long one leg of the walk may take before she is written off. */
	private static final int WALK_TIMEOUT = 200;
	/** How often a missing seat is re-checked, in ticks. */
	private static final int SEAT_RETRY = 40;
	private static final double ARRIVE_RANGE = 3.5;
	private static final double EAT_RANGE = 2.5;

	private enum Phase { SEEK_SEAT, SEEK_FOOD, EATING }

	private Phase phase = Phase.SEEK_SEAT;
	@Nullable
	private BlockPos target;
	private long deadline;
	private long eatEnd;
	/** Appeal of the dish currently being eaten, read before it was removed. */
	private int pendingAppeal = 0;

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
		phase = Phase.SEEK_SEAT;
		target = null;
		deadline = gameTime + PartyModule.ARRIVE_TIMEOUT;
		eatEnd = 0;
		pendingAppeal = 0;
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
		if (phase == Phase.EATING) {
			if (eatEnd != gameTime) return true;
			// the dish was removed when we started eating; credit it now so the
			// eaten tally and the party's served total move together
			party.eat(pendingAppeal, attachment);
			pendingAppeal = 0;
			eatEnd = 0;
			target = null;
			if (party.satisfied()) {
				PartyGift.give(level, entity, party);
				dismiss(entity);
				return false;
			}
			phase = Phase.SEEK_FOOD;
			clearWalk(entity);
			// true, not false: see the note on the arrival branch below
			return true;
		}
		if (target != null && !level.isLoaded(target)) target = null;
		if (target == null) return seek(level, entity, party, attachment, gameTime);
		if (entity.distanceToSqr(target.getCenter()) < rangeSqr()) {
			reach(level, entity, phase == Phase.SEEK_SEAT);
			// Deliberately keeps the task running across a phase change instead of
			// returning false for a tick. One Behavior instance drives seat -> food
			// -> eat, so the phase lives in this field; returning false stops the
			// task, stop() wipes the phase, and the brain re-runs it next tick with
			// start() resetting to SEEK_SEAT. The guest would sit down and then
			// immediately start hunting for another seat, forever, never reaching
			// the food. MarisaForageTask and the house tasks can return false here
			// because each is single-action and holds no state between runs.
			return true;
		}
		return true;
	}

	/** How close counts as arrival, squared. Seats are entered from above, food from beside. */
	private double rangeSqr() {
		double r = phase == Phase.SEEK_SEAT ? ARRIVE_RANGE : EAT_RANGE;
		return r * r;
	}

	private boolean seek(ServerLevel level, SmartYoukaiEntity entity, PartyModule party,
			PartyAttachment attachment, long gameTime) {
		if (phase == Phase.SEEK_SEAT) {
			var seat = nearest(level, entity, party.table(), GLTagGen.PARTY_SEAT);
			if (seat == null) {
				// no reachable seat yet: the player may still be placing chairs, so
				// keep the guest alive and look again shortly
				if (gameTime % SEAT_RETRY == 0) deadline = gameTime + PartyModule.ARRIVE_TIMEOUT;
				return false;
			}
			target = seat;
			BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET, new WalkTarget(seat.above(), 1, 1));
			BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET, new BlockPosTracker(seat));
			deadline = gameTime + WALK_TIMEOUT;
			return true;
		}
		var food = nearest(level, entity, party.table(), GLTagGen.PARTY_FOOD);
		if (food == null) {
			// nothing left to serve; she leaves, satisfied or not
			dismiss(entity);
			return false;
		}
		target = food;
		BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET, new WalkTarget(food.getCenter(), 1, 1));
		BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET, new BlockPosTracker(food));
		deadline = gameTime + WALK_TIMEOUT;
		return true;
	}

	private void reach(ServerLevel level, SmartYoukaiEntity entity, boolean seating) {
		if (seating) {
			// a no-op unless the block is one of ours; a third-party chair in
			// party_seat simply leaves her standing beside it
			HomeSearchUtil.setSitting(level, target, entity);
			phase = Phase.SEEK_FOOD;
			target = null;
			clearWalk(entity);
			return;
		}
		// re-validate: another guest may have taken this while she walked over
		var state = level.getBlockState(target);
		if (!state.is(GLTagGen.PARTY_FOOD) || !level.getBlockState(target.below()).is(GLTagGen.PARTY_TABLE)) {
			target = null;
			return;
		}
		// appeal of the serving she is about to take, read from the state BEFORE
		// consuming - one slice of a cake, one serving of a feast
		pendingAppeal = PartyFoodValue.perServe(level.registryAccess(), state);
		phase = Phase.EATING;
		eatEnd = level.getGameTime() + EAT_TICKS;
		entity.swing(InteractionHand.MAIN_HAND);
		YoukaiAnim.USE_MAINHAND.play(entity);
		level.playSound(null, target, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 1.0F);
		// one serve only: a cake loses a slice and stays on the table, so the rest
		// of the party can still reach it. false, not true: food eaten at a party
		// is not dropped back onto it.
		PartyFoodValue.consume(level, target, state);
	}

	private static void dismiss(SmartYoukaiEntity entity) {
		clearWalk(entity);
		entity.getModule(PartyModule.class).ifPresent(PartyModule::dismiss);
	}

	private static void clearWalk(SmartYoukaiEntity entity) {
		BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
		BrainUtils.clearMemory(entity, MemoryModuleType.LOOK_TARGET);
	}

	/** The nearest block in the tag, searching outward from the table. */
	@Nullable
	private static BlockPos nearest(ServerLevel level, SmartYoukaiEntity entity, @Nullable BlockPos table,
			TagKey<Block> tag) {
		if (table == null) return null;
		BlockPos from = entity.blockPosition();
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		int radius = GLModConfig.SERVER.partyScanRadius.get();
		for (var pos : BlockPos.betweenClosed(table, table.offset(radius, radius, radius))) {
			var state = level.getBlockState(pos);
			if (!state.is(tag)) continue;
			// tagged as food, but worth nothing to eat: walking over and eating it
			// would cost this guest the whole visit for no fulfillment at all
			if (!PartyFoodValue.result(state).valid()) continue;
			double dist = pos.distSqr(from);
			if (dist >= bestDist) continue;
			bestDist = dist;
			best = pos.immutable();
		}
		return best;
	}

	@Override
	protected void stop(ServerLevel level, SmartYoukaiEntity entity, long gameTime) {
		// A dish destroyed when we started eating but never credited - the party
		// ended, or the chunk unloaded, mid-bite - still counts. The block is already
		// gone, so dropping the tally would silently lose that food.
		if (pendingAppeal > 0) {
			var module = entity.getModule(PartyModule.class);
			if (module.isPresent()) {
				var attachment = module.get().party(level);
				if (attachment != null) module.get().eat(pendingAppeal, attachment);
			}
		}
		clearWalk(entity);
		target = null;
		phase = Phase.SEEK_SEAT;
		eatEnd = 0;
		pendingAppeal = 0;
	}

}
