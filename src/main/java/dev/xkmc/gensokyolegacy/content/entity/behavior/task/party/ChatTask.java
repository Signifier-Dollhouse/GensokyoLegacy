package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyChat;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * Walk over to the partner and hold the conversation there. Only the initiator walks; the other
 * guest keeps her chair until the initiator is close, then stands. The chair claim is kept, so
 * when {@code PartyChat} ends the talk both go back to their own seats and on to the next dish.
 *
 * @see PartyTasks for where this sits in the cascade
 * @see "doc/design/party/guest.md"
 */
public class ChatTask extends PartyTask {

	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return ctx.chatting() && !ctx.leaving();
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		var id = ctx.chatPartner();
		if (id == null) {
			ctx.endChat();
			return;
		}
		var partner = level.getEntity(id);
		// a partner who is gone ends the talk instead of leaving her waiting at a stale spot
		if (partner == null || partner == entity) {
			ctx.endChat();
			return;
		}
		// once the talk has started PartyChat drives the clock and the animations
		if (ctx.talking()) {
			ctx.unseat();
			return;
		}
		boolean near = entity.distanceToSqr(partner) <= PartyChat.TALK_RANGE * PartyChat.TALK_RANGE;
		if (!ctx.chatLead()) {
			// the one who waits stays on her chair until the initiator is close enough
			if (near) ctx.unseat();
			return;
		}
		if (near) {
			ctx.clearWalk();
			ctx.unseat();
			return;
		}
		var spot = ctx.standingNear(partner.blockPosition());
		// one block of reach on both halves: close enough to talk, and never a target she can
		// "arrive" at while still outside TALK_RANGE, which would leave her waiting for ever
		var target = new PartyTarget(partner.blockPosition(), spot,
				PartyTarget.WALK_REACH, PartyTarget.WALK_REACH);
		ctx.walkTo(target);
	}

}
