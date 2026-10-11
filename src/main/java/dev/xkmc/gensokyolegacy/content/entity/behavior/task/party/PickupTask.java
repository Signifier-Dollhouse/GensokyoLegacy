package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;


/**
 * Take the serving: the gesture, and then the plate in her hand.
 *
 * <p>
 * The serve comes off the block here rather than when she eventually eats it, so two guests
 * converging on one cake cannot both be counted for it. It is also the re-validation that
 * sends her back for another dish if the one she hesitated over was eaten while she stood
 * there thinking.
 */
public class PickupTask extends PartyTask {


	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return ctx.grabbing();
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		if (ctx.tickGrab() > 0) return;
		BlockPos dish = ctx.dish();
		// reach is read before the memory is dropped: dishInReach looks the dish up
		boolean takeable = dish != null && ctx.dishInReach();
		ctx.dish(null);
		// someone else got there first, or the host broke it up: draw again
		if (!takeable || !ctx.takeServing(dish)) return;
		// holding it is the whole of the handoff: seated she is already home, otherwise
		// GoToSeat picks her up. There is no path from here to eating that does not end in
		// a chair.
	}
}