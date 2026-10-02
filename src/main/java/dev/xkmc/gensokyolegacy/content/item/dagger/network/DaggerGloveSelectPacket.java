package dev.xkmc.gensokyolegacy.content.item.dagger.network;

import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveItem;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveMode;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveSelectionListener;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side mode switch on the held glove, from the selector wheel.
 * <p>
 * Carries a {@link DaggerGloveMode} ordinal rather than a component value so that a client
 * claiming a mode that does not exist cannot write junk into the stack: the ordinal is bounds-checked
 * against the enum here, where the enum exists, and ignored if it does not name a mode.
 */
public record DaggerGloveSelectPacket(int wheel, int index) implements SerialPacketBase<DaggerGloveSelectPacket> {

	@Override
	public void handle(Player player) {
		if (wheel != 0) return;
		ItemStack stack = DaggerGloveSelectionListener.getHeldGlove(player);
		if (stack == null) return;
		var modes = DaggerGloveMode.values();
		if (index < 0 || index >= modes.length) return;
		DaggerGloveItem.setMode(stack, modes[index]);
	}

}