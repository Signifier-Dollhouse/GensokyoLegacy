package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.server.level.ServerLevel;


/**
 * Start the chew: she has the serving and she is where she eats it.
 *
 * <p>
 * The tick between arriving and chewing exists so the plate visibly reaches her mouth
 * before it starts going away, rather than appearing and vanishing in one frame.
 */
public class EatTask extends PartyTask {


	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return ctx.holding() && ctx.seated()
				&& !ctx.chewing();
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		ctx.beginChew();
	}
}