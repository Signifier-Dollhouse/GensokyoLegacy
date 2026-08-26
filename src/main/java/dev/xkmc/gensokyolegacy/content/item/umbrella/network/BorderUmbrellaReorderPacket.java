package dev.xkmc.gensokyolegacy.content.item.umbrella.network;

import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaSlots;

public record BorderUmbrellaReorderPacket(int from, int to) implements SerialPacketBase<BorderUmbrellaReorderPacket> {

	@Override
	public void handle(Player player) {
		ItemStack stack = BorderUmbrellaSelectionListener.getHeldUmbrella(player);
		if (stack == null || stack.isEmpty()) return;
		int a = Math.floorMod(from, BorderUmbrellaSlots.MAX_SLOTS);
		int b = Math.floorMod(to, BorderUmbrellaSlots.MAX_SLOTS);
		if (a == b) return;
		BorderUmbrellaItem.swapSlots(stack, a, b);
		// also update selected slot if it pointed to swapped indices
		// keep selection index as is (physical slot), not content-following
	}
}
