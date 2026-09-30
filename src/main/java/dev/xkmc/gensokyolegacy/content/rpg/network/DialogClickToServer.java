package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.ui.dialog.DialogSession;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

/**
 * A click on one of the dialog's options, standing in for the inventory button
 * click a container menu would have carried. {@code index} is the option index
 * the server used when it built the list this client is looking at.
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
