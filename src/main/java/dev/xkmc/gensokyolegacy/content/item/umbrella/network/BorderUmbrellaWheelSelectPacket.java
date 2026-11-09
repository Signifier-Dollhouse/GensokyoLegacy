package dev.xkmc.gensokyolegacy.content.item.umbrella.network;

import dev.xkmc.gensokyolegacy.content.item.common.network.SelectorSelectPacket;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaSlots;
import dev.xkmc.gensokyolegacy.content.item.umbrella.wheel.BorderUmbrellaDistanceEntry;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.network.RecordPacketBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A pick from one of the umbrella's two <b>accessory</b> wheels: 1 the recorded position, 2 the teleport distance. Both
 * write umbrella-only components, so unlike the shared mode wheel ({@link SelectorSelectPacket}) nothing to generalise,
 * and {@code wheel} is a payload selector, not the UI index: the distance wheel sits at {@code -1}, the fake "Edit"
 * wheel, but arrives as 2.
 */
public record BorderUmbrellaWheelSelectPacket(int wheel, int index)
		implements RecordPacketBase {

	@Override
	public void handle(Player player) {
		ItemStack stack = BorderUmbrellaSelectionListener.getHeldUmbrella(player);
		if (stack == null || stack.isEmpty()) return;
		if (wheel == 1) {
			// slot selection
			if (index < 0 || index >= BorderUmbrellaSlots.MAX_SLOTS) return;
			GLItems.UMBRELLA_SLOT_SELECTED.set(stack, Math.floorMod(index, BorderUmbrellaSlots.MAX_SLOTS));
		} else if (wheel == 2) {
			// distance selection
			if (index < 0 || index >= BorderUmbrellaDistanceEntry.DISTANCES.length) return;
			int dist = BorderUmbrellaDistanceEntry.distanceOf(index);
			GLItems.UMBRELLA_DISTANCE.set(stack, dist);
		}
	}
}
