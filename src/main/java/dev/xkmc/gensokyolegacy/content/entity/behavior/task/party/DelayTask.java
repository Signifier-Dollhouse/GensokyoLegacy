package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

/**
 * Standing in front of the dish, deciding.
 *
 * <p>
 * Purely a wait, and deliberately a state of its own rather than a delay inside the one
 * before or after it: this is the beat where a guest looks like she is choosing rather than
 * executing, and it is the only place she can still change her mind. Without it a table
 * draws, stands and eats in lockstep, which reads as one organism rather than as people.
 *
 * <p>
 * The floor matters as much as the ceiling. An instant draw reads as the character
 * reacting to the player rather than deciding on anything.
 */
public class DelayTask extends PartyTask {

	public DelayTask() {
		super(Map.of(
				GLBrains.MEM_PARTY_DISH.get(), MemoryStatus.VALUE_PRESENT,
				GLBrains.MEM_PARTY_SERVE.get(), MemoryStatus.VALUE_ABSENT));
	}

	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return BrainUtils.hasMemory(entity, GLBrains.MEM_PARTY_HESITATE.get());
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		if (ctx.tick(GLBrains.MEM_PARTY_HESITATE.get()) > 0) return;
		// the decision is made; the hand is what takes a moment after it
		ctx.waitFor(GLBrains.MEM_PARTY_GRAB.get(), PartyTasks.PICKUP_TICKS);
	}
}