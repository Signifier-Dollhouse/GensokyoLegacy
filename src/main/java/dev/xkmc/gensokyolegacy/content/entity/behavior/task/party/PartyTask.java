package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

/**
 * One task of the party machine. A task does not know what it is a state <i>of</i>: it knows
 * only whether its own fields on {@link PartyModule} still describe the situation. Which task
 * that amounts to is decided by the brain's gate, in registration order, so the machine has no
 * dispatcher and no phase enum.
 *
 * <p>
 * <b>Walking is not a task:</b> every task in the cascade carries {@code WALK_TARGET: VALUE_ABSENT}
 * and {@link #canStillUse} yields while a walk order is outstanding, so the walk order stands in
 * for reach. <b>{@link #stillMine} must go false once a task has done its job</b>, or a running
 * task keeps acting: every task that sets a field therefore also tests for it.
 *
 * @see PartyTasks for the order, which is the priority
 * @see PartyContext for the moves
 * @see "doc/design/party/guest.md"
 */
public abstract class PartyTask extends Behavior<SmartYoukaiEntity> {

	protected PartyTask() {
		this(Map.of(
				MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), Behavior.DEFAULT_DURATION);
	}

	/**
	 * For a task an outstanding walk order does not yield. Every task in the cascade takes the
	 * default and yields on one, because {@code YoukaiMoveTask} has her then; the director does
	 * not, because watching that walk is its job. See {@link PartyDirectorTask}.
	 *
	 * @param duration how long one run of the behavior lasts before it stops and is started again
	 */
	protected PartyTask(Map<MemoryModuleType<?>, MemoryStatus> entry, int duration) {
		super(entry, duration);
	}

	/**
	 * Whether the party is closing. The cascade tasks do not start once it is: the director has
	 * armed the departure by then, and the line of the cascade is {@link LeaveTask}.
	 */
	protected boolean blocksWhileLeaving() {
		return true;
	}

	/**
	 * Whether this task still describes the situation. This is the handoff: a task sets the
	 * field that hands over, and the next tick the gate picks somebody else.
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
		// an outstanding walk order means YoukaiMoveTask has her, not us
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

	/**
	 * Whether this task still makes sense with no party to look at. Only the exit does.
	 */
	protected boolean canRunWithoutParty() {
		return false;
	}

	/**
	 * This task's one tick of work.
	 */
	protected abstract void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx);
}