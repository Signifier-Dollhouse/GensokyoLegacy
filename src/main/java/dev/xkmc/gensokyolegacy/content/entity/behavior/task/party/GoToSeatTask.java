package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.server.level.ServerLevel;


/**
 * Carry the serving back to her chair.
 *
 * <p>
 * <b>Nothing gets eaten away from the table.</b> Having food in hand and not being seated is
 * precisely this task, so picking a dish up leads here and nowhere else.
 *
 * <p>
 * Losing the chair on the way back is the awkward case, and the only honest answer is to eat
 * standing: she is holding a plate and the chair is gone.
 *
 * @see doc/design/party/guest.md
 */
public class GoToSeatTask extends PartyTask {


	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		// both of the fields this task sets are also what ends it: the chew, which is where a guest
		// with no chair goes, and the departure. Staying true here would re-arm the chew every
		// tick and starve Chew.
		return ctx.holding() && !ctx.seated()
				&& !ctx.chewing()
				&& !ctx.leaving();
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		var seat = ctx.herSeat();
		// the chair is gone, or she never claimed one: there is nothing to carry it to, so
		// she eats standing. Chew does not require a chair, so this resolves.
		if (seat == null) {
			ctx.beginChew();
			return;
		}
		if (seat.inReach(ctx) && seat.arrived(ctx)) {
			ctx.sit(seat);
			return;
		}
		ctx.walkTo(seat);
	}
}