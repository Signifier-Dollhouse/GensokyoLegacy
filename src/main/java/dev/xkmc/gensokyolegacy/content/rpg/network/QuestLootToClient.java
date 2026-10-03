package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.rpg.reward.LootDrop;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the loot tables behind quest rewards can drop, read server-side
 * ({@link dev.xkmc.gensokyolegacy.content.rpg.reward.LootReader}) and handed over as it is:
 * loot tables are never synced, so this is the only way a client can learn a quest's reward.
 * <p>
 * Sent once per datapack sync, which covers joining a world and {@code /reload} alike.
 */
public record QuestLootToClient(LinkedHashMap<ResourceLocation, ArrayList<LootDrop>> drops)
		implements SerialPacketBase<QuestLootToClient> {

	@Override
	public void handle(Player player) {
		ClientHandler.replace(drops);
	}

	public static class ClientHandler {

		private static LinkedHashMap<ResourceLocation, ArrayList<LootDrop>> drops = new LinkedHashMap<>();

		private ClientHandler() {
		}

		static void replace(LinkedHashMap<ResourceLocation, ArrayList<LootDrop>> drops) {
			ClientHandler.drops = new LinkedHashMap<>(drops);
		}

		/**
		 * what {@code table} can drop, as far as it has been sampled; empty if never heard of
		 */
		public static ArrayList<LootDrop> get(ResourceLocation table) {
			return drops.getOrDefault(table, new ArrayList<>());
		}

	}

}
