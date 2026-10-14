package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.server.level.ServerLevel;


/**
 * Sitting between dishes. Leaving is rolled here, and only here: it is the reason a guest ever
 * goes home while there is still food on the table. Rolled per satisfied sit rather than on a
 * timer, so a long {@code fullSeat} also means a slow departure.
 *
 * @see "doc/design/party/guest.md"
 */
public class RestTask extends PartyTask {


	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return ctx.resting();
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		if (ctx.tickRest() > 0) return;
		// resting is over, so the gate drops to whichever task now fits — either back to
		// finding a dish, or out of the door
		if (ctx.party().satisfied() && entity.getRandom().nextDouble() < ctx.party().leaveChance()) {
			ctx.armLeave();
		}
	}
}