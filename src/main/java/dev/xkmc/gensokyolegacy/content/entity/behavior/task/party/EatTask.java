package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

/**
 * Start the chew: she has the serving and she is where she eats it.
 *
 * <p>
 * The tick between arriving and chewing exists so the plate visibly reaches her mouth
 * before it starts going away, rather than appearing and vanishing in one frame.
 */
public class EatTask extends PartyTask {

	public EatTask() {
		super(Map.of(
				GLBrains.MEM_PARTY_SERVE.get(), MemoryStatus.VALUE_PRESENT,
				GLBrains.MEM_PARTY_CHEW.get(), MemoryStatus.VALUE_ABSENT));
	}

	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return ctx.holding() && ctx.seated()
				&& !BrainUtils.hasMemory(entity, GLBrains.MEM_PARTY_CHEW.get());
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		ctx.beginChew();
	}
}