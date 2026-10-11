package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

/**
 * Sitting between dishes.
 *
 * <p>
 * Leaving is rolled here, and only here. It is the reason a guest ever goes home while there
 * is still food on the table, and it has to exist — otherwise a guest who is already full
 * would occupy a chair at a party someone is trying to enjoy until the spread is gone.
 * Rolled per satisfied sit rather than on a timer, so the two per-character fields mean
 * something together: a long {@code fullSeat} also means a slow departure.
 */
public class RestTask extends PartyTask {

	public RestTask() {
		super(Map.of(GLBrains.MEM_PARTY_REST.get(), MemoryStatus.VALUE_PRESENT));
	}

	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return BrainUtils.hasMemory(entity, GLBrains.MEM_PARTY_REST.get());
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		if (ctx.tick(GLBrains.MEM_PARTY_REST.get()) > 0) return;
		// resting is over, so the gate drops to whichever task now fits — either back to
		// finding a dish, or out of the door
		if (ctx.party().satisfied() && entity.getRandom().nextDouble() < ctx.party().leaveChance()) {
			ctx.armLeave();
		}
	}
}