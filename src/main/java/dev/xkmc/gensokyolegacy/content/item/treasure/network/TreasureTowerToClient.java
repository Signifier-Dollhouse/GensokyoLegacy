package dev.xkmc.gensokyolegacy.content.item.treasure.network;

import dev.xkmc.gensokyolegacy.content.item.treasure.OreRecipe;
import dev.xkmc.gensokyolegacy.content.item.treasure.TreasureTowerClient;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

/**
 * The derived ore recipe list. Loot tables and worldgen registries never reach a client, so this is
 * its only way to learn what the tower accepts; sent once per datapack sync.
 *
 * @see "doc/design/tower.md"
 */
public record TreasureTowerToClient(ArrayList<OreRecipe> recipes) implements SerialPacketBase<TreasureTowerToClient> {

	@Override
	public void handle(Player player) {
		TreasureTowerClient.set(recipes);
	}

}
