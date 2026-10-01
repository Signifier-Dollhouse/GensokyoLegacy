package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.ui.dialog.DialogScreen;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

/**
 * Server-driven close of a dialog screen, sent when the conversation ends or
 * the character stops talking. Carries the session id so a close that raced a
 * newly opened dialog does not take that one down with it.
 */
public record DialogCloseToClient(int session) implements SerialPacketBase<DialogCloseToClient> {

	@Override
	public void handle(Player player) {
		if (player.level().isClientSide) {
			DialogScreen.close(session);
		}
	}

}
