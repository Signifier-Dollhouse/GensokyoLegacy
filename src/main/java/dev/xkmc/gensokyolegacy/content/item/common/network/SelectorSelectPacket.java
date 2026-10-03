package dev.xkmc.gensokyolegacy.content.item.common.network;

import dev.xkmc.l2itemselector.select.item.IItemSelector;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

/**
 * A pick from a selector wheel, for every selector item in the mod at once: the border
 * umbrella's mode wheel and both gloves' mode wheels.
 *
 * <p>There used to be one packet per wheel-owning item, and each was the scroll selector's
 * server-side path — {@link IItemSelector#swap} — written out again so the index could be
 * bounds-checked by hand. Nothing is left to share once you notice the library already resolves
 * the held stack to its own selector, so this is the whole job: resolve, swap, done. Each
 * selector's {@code swap} is now the single place that validates the index and writes the
 * component, which is also why a wheel pick and a scroll pick can no longer disagree about what
 * an out-of-range index means.
 *
 * <p>Only the mode wheels use this. The umbrella's two accessory wheels — recorded position and
 * teleport distance — write umbrella-only components and stay on their own packet, since
 * nothing else has any use for them.
 */
public record SelectorSelectPacket(int index) implements SerialPacketBase<SelectorSelectPacket> {

	@Override
	public void handle(Player player) {
		// the held stack decides which selector answers, so a client cannot name one
		var sel = IItemSelector.getSelection(player);
		if (sel == null) return;
		sel.swap(player, index);
	}

}
