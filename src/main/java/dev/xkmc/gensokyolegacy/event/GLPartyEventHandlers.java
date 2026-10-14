package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyGiftReader;
import dev.xkmc.gensokyolegacy.content.attachment.party.network.PartyGiftToClient;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class GLPartyEventHandlers {

	/**
	 * Loot tables are not synced, so a client can not work out what a guest leaves behind on its
	 * own; it is told on joining, and again after a {@code /reload}.
	 */
	@SubscribeEvent
	public static void syncPartyGifts(OnDatapackSyncEvent event) {
		var sp = event.getPlayer();
		if (sp == null) return;
		GensokyoLegacy.HANDLER.toClientPlayer(new PartyGiftToClient(PartyGiftReader.all(sp.getServer())), sp);
	}

}