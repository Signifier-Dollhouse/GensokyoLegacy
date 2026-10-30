package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.ui.dialog.SimpleDialogScreen;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * One step of a conversation: the body text, the quest it is about, and the option labels this player may take.
 * Dialogs are inlined into the data files, so there is no id for the client to resolve; it only needs the lang
 * keys. Sending visible options rather than a per-option condition flag keeps a click index meaning the same on
 * both sides.
 */
public record SimpleDialogToClient(
		int session, int character, String text, @Nullable ResourceLocation quest,
		ArrayList<String> options
) implements SerialPacketBase<SimpleDialogToClient> {

	@Override
	public void handle(Player player) {
		if (player.level().isClientSide) {
			SimpleDialogScreen.open(this);
		}
	}

}
