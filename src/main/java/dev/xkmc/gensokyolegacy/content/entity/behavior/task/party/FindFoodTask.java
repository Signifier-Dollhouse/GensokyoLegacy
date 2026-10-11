package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Draw a dish, and commit to it.
 *
 * <p>
 * Random rather than nearest, and weighted rather than uniform, so a guest works her way
 * across the spread instead of clearing the nearest plate first — and so the tasty things
 * go before the plain ones.
 *
 * <p>
 * Nothing left she can reach is the end of her visit rather than another draw: she settles
 * up with a gift and goes.
 */
public class FindFoodTask extends PartyTask {

	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		var party = ctx.attachment();
		// the two memories this task sets are also what ends it. Without them in here it
		// would keep running after drawing, redrawing the dish every tick, and would sit
		// on top of LeaveTask holding the gate with the spread already gone.
		return ctx.seated()
				&& party != null && party.phase() == PartyAttachment.Phase.FEASTING
				&& !BrainUtils.hasMemory(entity, GLBrains.MEM_PARTY_DISH.get())
				&& !BrainUtils.hasMemory(entity, GLBrains.MEM_PARTY_LEAVE.get());
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		BlockPos dish = ctx.drawDish();
		if (dish == null) {
			ctx.giveGift();
			ctx.armLeave();
			return;
		}
		BrainUtils.setMemory(entity, GLBrains.MEM_PARTY_DISH.get(), dish);
	}
}