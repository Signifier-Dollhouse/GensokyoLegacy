package dev.xkmc.gensokyolegacy.content.item.umbrella.network;

import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;
import dev.xkmc.gensokyolegacy.content.item.umbrella.screen.BorderUmbrellaNameScreen;

public record BorderUmbrellaOpenRenamePacket(int slot, String currentName) implements SerialPacketBase<BorderUmbrellaOpenRenamePacket> {

	@Override
	public void handle(Player player) {
		// client side: open rename screen
		if (player.level().isClientSide) {
			BorderUmbrellaNameScreen.open(slot, currentName);
		}
	}
}
