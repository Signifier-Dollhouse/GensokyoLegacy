package dev.xkmc.gensokyolegacy.content.item.umbrella.network;

import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.umbrella.UmbrellaUtil;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaSlots;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record BorderUmbrellaDeletePacket(int slot) implements SerialPacketBase<BorderUmbrellaDeletePacket> {

	@Override
	public void handle(Player player) {
		ItemStack stack = BorderUmbrellaSelectionListener.getHeldUmbrella(player);
		if (stack == null || stack.isEmpty()) return;
		int idx = Math.floorMod(slot, BorderUmbrellaSlots.MAX_SLOTS);
		UmbrellaUtil.deleteSlot(stack, idx);
	}
}
