package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.ui.dialog.DialogSession;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

/**
 * A click on one of the dialog's options; {@code index} indexes the option list the server last sent.
 */
public record DialogClickToServer(
		int session, int character, int index
) implements SerialPacketBase<DialogClickToServer> {

	@Override
	public void handle(Player player) {
		var target = DialogSession.resolve(player, session, character);
		if (target != null) target.click(index);
	}

}
