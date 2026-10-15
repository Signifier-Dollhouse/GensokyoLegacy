package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.server.level.ServerLevel;


/**
 * Chewing, with the plate leaving her hand as she goes. The serving is banked when the chew ends,
 * so her tally and the party's served total move together rather than crediting food she has not
 * finished. No chair required: demanding a seat here would strand a guest whose chair was taken
 * away while she was carrying a plate.
 *
 * @see "doc/design/party/guest.md"
 */
public class ChewTask extends PartyTask {


	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return ctx.chewing();
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		int left = ctx.tickChew();
		if (left > 0) {
			// emptied part-way through rather than at the end, so the food visibly does not
			// survive the whole chew
			if (left <= PartyTasks.EAT_TICKS / 2) ctx.dropServing();
			return;
		}
		ctx.bankServe();
		// the sit between dishes starts once the food is counted, so the two cannot drift. Jittered
		// 100-120%: without it every guest at a table draws, eats and rests on the same beat.
		int base = ctx.party().satisfied() ? ctx.party().fullSeat() : ctx.party().hungrySeat();
		ctx.rest(base + ctx.entity().getRandom().nextInt(base / 5 + 1));
	}
}