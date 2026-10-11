package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

/**
 * One task of the party machine.
 *
 * <p>
 * A task does not know what it is a state <i>of</i> — it only knows whether its own fields
 * on {@link PartyModule} still describe the situation, and it keeps running exactly as long
 * as they do. Which task that amounts to is decided by the brain's gate, in registration
 * order, so the machine has no dispatcher and no phase enum.
 *
 * <p>
 * <b>Walking is not a task.</b> Every party task carries {@code WALK_TARGET:
 * VALUE_ABSENT}, and {@link #canStillUse} yields the moment a walk order is outstanding:
 * {@code YoukaiMoveTask} does the walking and clears the memory on arrival, at which point
 * the gate re-picks this same task and it finds itself arrived. That is also why reach is
 * not state — the walk order already stands in for it.
 *
 * <p>
 * <b>{@link #stillMine} must go false when a task has done its job.</b> A running task
 * keeps the gate — the brain only re-picks when nothing is running — so a task whose test
 * stays true after it acts will simply keep acting: redrawing the dish every tick, or
 * re-arming its own chew. Every task that sets a field therefore also tests for that field
 * here, and that is the whole handoff.
 *
 * @see PartyTasks for the order, which is the priority
 * @see PartyContext for the moves
 */
public abstract class PartyTask extends Behavior<SmartYoukaiEntity> {

	protected PartyTask() {
		super(Map.of(
				MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
	}

	/**
	 * Whether the party is closing. The cascade tasks do not start once it is: the
	 * director has armed the departure by then, and the line of the cascade is
	 * {@link LeaveTask}.
	 */
	protected boolean blocksWhileLeaving() {
		return true;
	}

	/**
	 * Whether this task still describes the situation. This is what hands off: a task sets
	 * the field that hands over, and the next tick the gate picks somebody else.
	 */
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return true;
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, SmartYoukaiEntity entity) {
		var module = entity.getModule(PartyModule.class).orElseThrow();
		if (blocksWhileLeaving() && module.leaveArmed) {
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