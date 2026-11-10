package dev.xkmc.gensokyolegacy.init;

import dev.xkmc.gensokyolegacy.content.entity.dolls.client.HeadPossession;
import dev.xkmc.gensokyolegacy.content.item.common.client.GloveTargetCache;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(value = Dist.CLIENT, modid = GensokyoLegacy.MODID)
public class GLClientTick {

	@SubscribeEvent
	public static void clientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		// the shared glove target cache (glove.md §2) traces for whichever gloves are held,
		// so it is driven from one place rather than from every glove's inventoryTick
		GloveTargetCache.tick();
		// after the level tick, so the head under a possessed camera reports the frame it
		// just flew rather than the keys it has not pressed yet
		HeadPossession.tick();
	}

	@SubscribeEvent
	public static void clientTickPre(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		// before the level tick, so the head flies on this frame's keys rather than last frame's
		HeadPossession.readInput();
	}

}
