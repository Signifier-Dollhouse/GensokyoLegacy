package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.rpg.handle.ClientHandle;
import dev.xkmc.gensokyolegacy.content.ui.dialog.FirstDialogScreen;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The topic list of a conversation: open it, or replace the one on screen.
 * This is the whole of the open handshake - the dialog has no container menu,
 * so there is no vanilla screen packet to piggyback on.
 */
public record FirstDialogToClient(
		int session, int character, @Nullable Component body, ArrayList<ClientHandle> options
) implements SerialPacketBase<FirstDialogToClient> {

	@Override
	public void handle(Player player) {
		if (player.level().isClientSide) {
			FirstDialogScreen.open(this);
		}
	}

}
