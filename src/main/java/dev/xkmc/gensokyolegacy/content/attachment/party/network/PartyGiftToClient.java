package dev.xkmc.gensokyolegacy.content.attachment.party.network;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.GiftTier;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * The gift ladder of every party-eligible character, keyed by her entity type. Loot tables are
 * never synced, so this is a client's only way to learn what a guest leaves behind; sent once per
 * datapack sync.
 */
public record PartyGiftToClient(LinkedHashMap<ResourceLocation, ArrayList<GiftTier>> tiers)
		implements SerialPacketBase<PartyGiftToClient> {

	@Override
	public void handle(Player player) {
		ClientHandler.replace(tiers);
	}

	public static class ClientHandler {

		private static LinkedHashMap<ResourceLocation, ArrayList<GiftTier>> tiers = new LinkedHashMap<>();

		private ClientHandler() {
		}

		static void replace(LinkedHashMap<ResourceLocation, ArrayList<GiftTier>> tiers) {
			PartyGiftToClient.ClientHandler.tiers = new LinkedHashMap<>(tiers);
		}

		/**
		 * her ladder, as far as it has been read; empty if never heard of
		 */
		public static ArrayList<GiftTier> get(ResourceLocation character) {
			return tiers.getOrDefault(character, new ArrayList<>());
		}

	}

}