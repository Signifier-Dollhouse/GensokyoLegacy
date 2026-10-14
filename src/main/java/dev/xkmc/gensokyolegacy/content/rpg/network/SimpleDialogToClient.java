package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.ui.dialog.SimpleDialogScreen;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One step of a conversation: a dialog, the quest it is about, and the option indices this player may take.
 * Sending visible indices rather than a per-option condition flag keeps a click index meaning the same on both sides.
 */
public record SimpleDialogToClient(
		int session, int character, ResourceLocation dialog, @Nullable ResourceLocation quest, ArrayList<Integer> options
) implements SerialPacketBase<SimpleDialogToClient> {

	@Override
	public void handle(Player player) {
		if (player.level().isClientSide) {
			SimpleDialogScreen.open(this);
		}
	}

}
