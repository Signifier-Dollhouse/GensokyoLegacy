package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = GensokyoLegacy.MODID, value = Dist.CLIENT)
public class ClientAreaEffectEvents {

	@SubscribeEvent
	public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		ClientAreaEffectTracker.clear();
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel().isClientSide()) {
			ClientAreaEffectTracker.clear();
		}
	}
}
