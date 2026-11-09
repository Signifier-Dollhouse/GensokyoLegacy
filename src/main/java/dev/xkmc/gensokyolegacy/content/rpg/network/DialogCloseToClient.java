package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.ui.dialog.DialogScreen;
import dev.xkmc.l2serial.network.RecordPacketBase;
import net.minecraft.world.entity.player.Player;

/**
 * Server-driven close of a dialog screen; the session id keeps a raced close from taking down a newer dialog.
 */
public record DialogCloseToClient(int session) implements RecordPacketBase {

	@Override
	public void handle(Player player) {
		if (player.level().isClientSide) {
			DialogScreen.close(session);
		}
	}

}
