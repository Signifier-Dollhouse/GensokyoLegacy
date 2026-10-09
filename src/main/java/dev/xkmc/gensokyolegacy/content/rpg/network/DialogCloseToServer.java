package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.ui.dialog.DialogSession;
import dev.xkmc.gensokyolegacy.content.entity.module.TalkModule;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

/**
 * The client dropped its dialog screen: closed it itself, or replaced it with
 * another screen. Only the session is released here - whether the character
 * stops talking is {@link TalkModule#tickServer()}'s call, because another
 * screen (the trade menu) may be taking the conversation over.
 */
public record DialogCloseToServer(
		int session, int character
) implements SerialPacketBase<DialogCloseToServer> {

	@Override
	public void handle(Player player) {
		var opt = DialogSession.resolve(player, session, character);
		if (opt != null) opt.release();
	}

}
