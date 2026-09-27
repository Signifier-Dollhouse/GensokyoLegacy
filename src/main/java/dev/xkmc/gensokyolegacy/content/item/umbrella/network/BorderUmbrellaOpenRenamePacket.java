package dev.xkmc.gensokyolegacy.content.item.umbrella.network;

import dev.xkmc.gensokyolegacy.content.item.umbrella.screen.BorderUmbrellaNameScreen;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record BorderUmbrellaOpenRenamePacket(int slot,
											 String currentName,
											 BlockPos pos,
											 ResourceLocation dim,
											 boolean overwrite) implements SerialPacketBase<BorderUmbrellaOpenRenamePacket> {

	@Override
	public void handle(Player player) {
		// client side: open rename screen
		if (player.level().isClientSide) {
			BorderUmbrellaNameScreen.open(slot, currentName, pos, dim, overwrite);
		}
	}
}
