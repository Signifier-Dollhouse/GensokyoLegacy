package dev.xkmc.gensokyolegacy.content.item.common.network;

import dev.xkmc.l2itemselector.select.item.IItemSelector;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

/**
 * A pick from a selector wheel, for every selector item in the mod at once: the border umbrella's mode
 * wheel and both gloves' mode wheels.
 *
 * <p>The library already resolves the held stack to its own selector, so this is the whole job: resolve,
 * swap, done — each selector's {@link IItemSelector#swap} stays the one place validating the index, so a
 * wheel pick and a scroll pick cannot disagree. Only the mode wheels use this; the umbrella's accessory
 * wheels stay on their own packet.
 *
 * @see "doc/design/dagger_glove.md"
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
