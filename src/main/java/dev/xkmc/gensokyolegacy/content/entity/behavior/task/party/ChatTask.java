package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * Walk to a meeting spot and hold the conversation there. The chair claim is kept, so when
 * {@code PartyChat} ends the talk she goes back to her own seat and straight on to the next dish.
 *
 * @see PartyTasks for where this sits in the cascade
 * @see "doc/design/party/guest.md"
 */
public class ChatTask extends PartyTask {

	/**
	 * The spot is both the thing to reach and the tile to stand on, so a loose reach lets
	 * the second guest count as there while standing beside the first.
	 */
	private static final double CHAT_REACH = 2;

	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return ctx.chatting() && !ctx.leaving();
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		// stand her up without releasing the claim: she comes back to this chair afterwards
		ctx.unseat();
		var spot = ctx.chatSpot();
		if (spot == null) {
			ctx.endChat();
			return;
		}
		// once both have arrived PartyChat drives the talk and its animations
		if (ctx.talking()) return;
		var target = new PartyTarget(spot, spot, CHAT_REACH, CHAT_REACH);
		if (target.arrived(ctx)) {
			if (!ctx.chatArrived()) {
				ctx.markChatArrived();
				ctx.clearWalk();
			}
			return;
		}
		if (ctx.canWalk()) ctx.walkTo(target);
	}

}
