package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.server.level.ServerLevel;


/**
 * Standing in front of the dish, deciding.
 *
 * <p>
 * Purely a wait, and deliberately a state of its own: this is the beat where a guest looks like
 * she is choosing rather than executing, and the only place she can still change her mind. The
 * floor matters as much as the ceiling — an instant draw reads as reacting to the player.
 *
 * @see "doc/design/party/guest.md"
 */
public class DelayTask extends PartyTask {


	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return ctx.hesitating();
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		if (ctx.tickHesitate() > 0) return;
		// the decision is made; the hand is what takes a moment after it
		ctx.grab(PartyTasks.PICKUP_TICKS);
	}
}