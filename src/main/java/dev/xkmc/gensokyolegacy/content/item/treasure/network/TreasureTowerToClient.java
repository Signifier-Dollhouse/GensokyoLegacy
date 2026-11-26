package dev.xkmc.gensokyolegacy.content.item.treasure.network;

import dev.xkmc.gensokyolegacy.content.item.treasure.OreRecipe;
import dev.xkmc.gensokyolegacy.content.item.treasure.TreasureTowerClient;
import dev.xkmc.l2serial.network.RecordPacketBase;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

/**
 * The derived ore recipe list. Loot tables and worldgen registries never reach a client, so this is
 * its only way to learn what the tower accepts; sent once per datapack sync.
 *
 * @see "doc/design/item/tower.md"
 */
public record TreasureTowerToClient(ArrayList<OreRecipe> recipes) implements RecordPacketBase {

	@Override
	public void handle(Player player) {
		TreasureTowerClient.set(recipes);
	}

}
