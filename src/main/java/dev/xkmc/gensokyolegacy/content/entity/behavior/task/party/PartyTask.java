package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One task of the party machine.
 *
 * <p>
 * A task does not know what it is a state <i>of</i> — it only knows which memories must be
 * set for it to start, and it keeps running exactly as long as that signature is still the
 * live one. Which task that amounts to is decided by the brain's gate, so the machine has
 * no dispatcher and no phase enum.
 *
 * <p>
 * <b>Walking is not a task.</b> Every party task carries {@code WALK_TARGET:
 * VALUE_ABSENT}, and {@link #canStillUse} yields the moment a walk order is outstanding:
 * {@code YoukaiMoveTask} does the walking and clears the memory on arrival, at which point
 * the gate re-picks this same task and it finds itself arrived. That is also why reach is
 * not a memory — the walk order already stands in for it.
 *
 * <p>
 * <b>{@link #stillMine} must go false when a task has done its job.</b> A running task
 * keeps the gate — the brain only re-picks when nothing is running — so a task whose
 * signature stays true after it acts will simply keep acting: redrawing the dish every
 * tick, or re-arming its own chew. Every task that sets a memory therefore also tests for
 * that memory here, and that is the whole handoff.
 *
 * @see PartyTasks for the order, which is the priority
 * @see PartyContext for the moves
 */
public abstract class PartyTask extends Behavior<SmartYoukaiEntity> {

	/** Every party task starts on these, plus whatever the task itself names. */
	private static Map<MemoryModuleType<?>, MemoryStatus> gate(Map<MemoryModuleType<?>, MemoryStatus> own) {
		var map = new LinkedHashMap<MemoryModuleType<?>, MemoryStatus>();
		map.put(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT);
		map.put(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT);
		map.put(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED);
		map.putAll(own);
		return map;
	}

	/** No opinion on any memory. The fallback tasks use this and are told apart by physics. */
	protected PartyTask() {
		super(gate(Map.of()));
	}

	protected PartyTask(Map<MemoryModuleType<?>, MemoryStatus> own) {
		super(gate(own));
	}

	/**
	 * Whether this task has an opinion on whether the party is closing. The cascade tasks do:
	 * once the director has armed the departure, the line of the tree is {@link LeaveTask}
	 * and nothing else should start.
	 */
	protected boolean blocksWhileLeaving() {
		return true;
	}

	/**
	 * Whether the signature that identifies this task is still the live one. This is what
	 * hands off: a task sets the memory that hands over, and the next tick the gate picks
	 * somebody else.
	 */
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return true;
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, SmartYoukaiEntity entity) {
		var module = entity.getModule(PartyModule.class).orElseThrow();
		if (blocksWhileLeaving() && module.leaving()
				&& !BrainUtils.hasMemory(entity, GLBrains.MEM_PARTY_LEAVE.get())) {
			return false;
		}
		return stillMine(entity, new PartyContext(entity, level, module.party(level)));
	}

	@Override
	protected boolean canStillUse(ServerLevel level, SmartYoukaiEntity entity, long gameTime) {
		// a walk order outstanding means YoukaiMoveTask has her, not us
		if (BrainUtils.getMemory(entity, MemoryModuleType.WALK_TARGET) != null) return false;
		var module = entity.getModule(PartyModule.class).orElseThrow();
		return stillMine(entity, new PartyContext(entity, level, module.party(level)));
	}

	@Override
	protected void tick(ServerLevel level, SmartYoukaiEntity entity, long gameTime) {
		var module = entity.getModule(PartyModule.class).orElseThrow();
		PartyAttachment party = module.party(level);
		var ctx = new PartyContext(entity, level, party);
		if (party == null && !canRunWithoutParty()) {
			ctx.dismiss();
			return;
		}
		step(level, entity, ctx);
	}

	/** Whether this task still makes sense with no party to look at. Only the exit does. */
	protected boolean canRunWithoutParty() {
		return false;
	}

	/** This task's one tick of work. */
	protected abstract void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx);
}