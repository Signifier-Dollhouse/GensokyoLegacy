package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.server.level.ServerLevel;


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