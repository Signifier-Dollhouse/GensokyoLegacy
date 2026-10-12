package dev.xkmc.gensokyolegacy.content.item.common.network;

import dev.xkmc.l2itemselector.select.item.IItemSelector;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

/**
 * A pick from a selector wheel, for every selector item in the mod at once: the border
 * umbrella's mode wheel and both gloves' mode wheels.
 *
 * <p>The library already resolves the held stack to its own selector, so this is the whole job:
 * resolve, swap, done — and each selector's {@link IItemSelector#swap} stays the one place that
 * validates the index, which is why a wheel pick and a scroll pick cannot disagree.
 *
 * <p>Only the mode wheels use this; the umbrella's two accessory wheels write umbrella-only
 * components and stay on their own packet.
 *
 * @see doc/design/dagger_glove.md
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
