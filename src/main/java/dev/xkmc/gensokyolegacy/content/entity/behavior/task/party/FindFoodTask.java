package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Draw a dish, and commit to it. Random rather than nearest, and weighted rather than uniform,
 * so a guest works her way across the spread instead of clearing the nearest plate first — and so
 * the tasty things go first. Nothing left she can reach is the end of her visit rather than
 * another draw: she settles up with a gift and goes.
 *
 * @see "doc/design/party/guest.md"
 */
public class FindFoodTask extends PartyTask {

	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		var party = ctx.attachment();
		// the two fields this task sets are also what ends it: without them it would redraw the dish
		// every tick, and would sit on top of LeaveTask holding the gate with the spread gone
		return ctx.seated()
				&& party != null && party.phase() == PartyAttachment.Phase.FEASTING
				&& ctx.dish() == null
				&& !ctx.leaving();
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		BlockPos dish = ctx.drawDish();
		if (dish == null) {
			// armLeave leaves the gift as well
			ctx.armLeave();
			return;
		}
		ctx.dish(dish);
	}
}