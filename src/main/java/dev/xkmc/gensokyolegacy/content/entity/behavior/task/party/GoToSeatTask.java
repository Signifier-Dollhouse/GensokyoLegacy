package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.server.level.ServerLevel;


/**
 * Carry the serving back to her chair. Losing the chair on the way back means eating standing,
 * because she is holding a plate and the chair is gone.
 *
 * @see "doc/design/party/guest.md"
 */
public class GoToSeatTask extends PartyTask {


	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		// both fields this task sets also end it: the chew, which is where a guest with no chair
		// goes, and the departure. Staying true here would re-arm the chew and starve Chew.
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