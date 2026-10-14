package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.ui.dialog.DialogScreen;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

/**
 * Server-driven close of a dialog screen; the session id keeps a raced close from taking down a newer dialog.
 */
public record DialogCloseToClient(int session) implements SerialPacketBase<DialogCloseToClient> {

	@Override
	public void handle(Player player) {
		if (player.level().isClientSide) {
			DialogScreen.close(session);
		}
	}

}
