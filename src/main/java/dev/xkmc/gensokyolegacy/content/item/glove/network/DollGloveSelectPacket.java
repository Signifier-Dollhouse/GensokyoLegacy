package dev.xkmc.gensokyolegacy.content.item.glove.network;

import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.glove.mode.DollGloveMode;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record DollGloveSelectPacket(int wheel, int index) implements SerialPacketBase<DollGloveSelectPacket> {

	@Override
	public void handle(Player player) {
		ItemStack stack = DollGloveSelectionListener.getHeldGlove(player);
		if (stack == null || stack.isEmpty()) return;
		if (wheel == 0) {
			var modes = DollGloveMode.values();
			if (index < 0 || index >= modes.length) return;
			stack.set(GLItems.DOLL_GLOVE_MODE.get(), index);
		}
	}

}
