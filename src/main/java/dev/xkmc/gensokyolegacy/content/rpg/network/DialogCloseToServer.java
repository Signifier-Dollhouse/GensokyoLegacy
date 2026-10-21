package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.ui.dialog.DialogSession;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.TalkModule;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

/**
 * The client dropped its dialog screen: closed it, or replaced it with another. Only the session is released;
 * stopping the conversation is {@link TalkModule#tickServer()}'s call, since another screen may take over.
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
