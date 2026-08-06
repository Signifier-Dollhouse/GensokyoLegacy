package dev.xkmc.gensokyolegacy.content.client.debug;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IDebugOverlayWand {

	/**
	 * Add this tool's overlay lines while it is held/equipped.
	 */
	void addTooltip(Player player, ItemStack stack, List<Component> lines, long gameTime);

}
