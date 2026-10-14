package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Go and get the dish she has committed to. This task moves her and {@link DelayTask} is her
 * deciding: she crosses first and hesitates at the dish, which is the only arrangement in which
 * changing her mind at the last moment is representable at all.
 *
 * @see "doc/design/party/guest.md"
 */
public class GoToFoodTask extends PartyTask {

	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		BlockPos dish = ctx.dish();
		return dish != null
				&& !ctx.hesitating()
				&& !ctx.grabbing();
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		BlockPos dish = ctx.dish();
		if (dish == null) return;
		// seated reach from her chair, standing reach from her feet: see PartyTarget
		var target = PartyTarget.forDish(dish, ctx.standingNear(dish), ctx.seated());
		if (target.inReach(ctx)) {
			if (!target.arrived(ctx)) {
				// in range but not on the tile: the walk order is what closes that gap
				ctx.walkTo(target);
				return;
			}
			ctx.arrived();
			// set before handing over, and never refreshed: Delay spends a wait without it
			ctx.hesitate(
					PartyTasks.BITE_DELAY_MIN + ctx.entity().getRandom()
							.nextInt(PartyTasks.BITE_DELAY - PartyTasks.BITE_DELAY_MIN + 1));
			return;
		}
		ctx.walkTo(target);
	}
}