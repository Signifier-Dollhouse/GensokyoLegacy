package dev.xkmc.gensokyolegacy.content.item.umbrella.network;

import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaSlots;
import dev.xkmc.gensokyolegacy.content.item.umbrella.wheel.BorderUmbrellaDistanceEntry;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A pick from one of the umbrella's two <b>accessory</b> wheels: wheel 1 the recorded position
 * to travel to, wheel 2 the teleport distance. Both write umbrella-only components, so unlike
 * the mode wheel — which every selector item shares through
 * {@link dev.xkmc.gensokyolegacy.content.item.selector.SelectorSelectPacket} — there is nothing
 * here to generalise.
 *
 * <p>{@code wheel} is a payload selector, not the wheel index the UI uses: the distance wheel
 * sits at index {@code -1} in the wheel chain and still arrives here as 2, and index 2 is
 * itself a fake "Edit Position" wheel that never leaves the client.
 */
public record BorderUmbrellaWheelSelectPacket(int wheel, int index)
		implements SerialPacketBase<BorderUmbrellaWheelSelectPacket> {

	@Override
	public void handle(Player player) {
		ItemStack stack = BorderUmbrellaSelectionListener.getHeldUmbrella(player);
		if (stack == null || stack.isEmpty()) return;
		if (wheel == 1) {
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
