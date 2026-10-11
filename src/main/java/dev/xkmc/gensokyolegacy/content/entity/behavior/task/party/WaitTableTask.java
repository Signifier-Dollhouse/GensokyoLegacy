package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * Sitting at the table while the rest of it arrives.
 *
 * <p>
 * The feast is party-wide: nobody eats until every guest has a chair, because a guest
 * starting early would clear a dish out from under someone still walking in.
 */
public class WaitTableTask extends PartyTask {

	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		var party = ctx.attachment();
		return ctx.seated() && party != null && party.phase() != PartyAttachment.Phase.FEASTING;
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		// nothing to do: her presence is the whole of the contribution
	}
}