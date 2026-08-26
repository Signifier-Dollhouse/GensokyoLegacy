package dev.xkmc.gensokyolegacy.content.item.umbrella.network;

import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.umbrella.wheel.BorderUmbrellaDistanceEntry;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaSlots;

public record BorderUmbrellaSelectPacket(int wheel, int index) implements SerialPacketBase<BorderUmbrellaSelectPacket> {

	@Override
	public void handle(Player player) {
		ItemStack stack = BorderUmbrellaSelectionListener.getHeldUmbrella(player);
		if (stack == null || stack.isEmpty()) return;
		if (wheel == 0) {
			// mode selection: index is among available modes
			var available = BorderUmbrellaSelectionListener.getAvailableModes(stack);
			if (index < 0 || index >= available.size()) return;
			var mode = available.get(index);
			stack.set(GLItems.UMBRELLA_TYPE.get(), mode);
		} else if (wheel == 1) {
			// slot selection
			if (index < 0 || index >= BorderUmbrellaSlots.MAX_SLOTS) return;
			stack.set(GLItems.UMBRELLA_SLOT_SELECTED.get(), Math.floorMod(index, BorderUmbrellaSlots.MAX_SLOTS));
		} else if (wheel == 2) {
			// distance selection
			if (index < 0 || index >= BorderUmbrellaDistanceEntry.DISTANCES.length) return;
			int dist = BorderUmbrellaDistanceEntry.distanceOf(index);
			stack.set(GLItems.UMBRELLA_DISTANCE.get(), dist);
		}
	}
}
