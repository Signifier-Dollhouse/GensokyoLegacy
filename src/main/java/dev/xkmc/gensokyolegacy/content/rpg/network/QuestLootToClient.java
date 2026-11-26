package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.rpg.reward.QuestLoot;
import dev.xkmc.gensokyolegacy.content.rpg.reward.RewardRule;
import dev.xkmc.l2serial.network.RecordPacketBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Loot-table drops behind quest rewards. Loot tables are never synced, so this is a client's only
 * way to learn a quest's reward; sent once per datapack sync. Keyed by quest, since the rule
 * describes the whole reward list and not one table.
 */
public record QuestLootToClient(LinkedHashMap<ResourceLocation, QuestLoot> loot)
		implements RecordPacketBase {

	@Override
	public void handle(Player player) {
		ClientHandler.replace(loot);
	}

	public static class ClientHandler {

		private static LinkedHashMap<ResourceLocation, QuestLoot> loot = new LinkedHashMap<>();

		private ClientHandler() {
		}

		static void replace(LinkedHashMap<ResourceLocation, QuestLoot> loot) {
			ClientHandler.loot = new LinkedHashMap<>(loot);
		}

		/**
		 * what the quest can drop, as far as it has been sampled; empty if never heard of
		 */
		public static QuestLoot get(ResourceLocation quest) {
			return loot.getOrDefault(quest, new QuestLoot(RewardRule.MIXED, new ArrayList<>()));
		}

	}

}
