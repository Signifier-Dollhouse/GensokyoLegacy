package dev.xkmc.gensokyolegacy.content.rpg.network;

import dev.xkmc.gensokyolegacy.content.ui.dialog.SimpleDialogScreen;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One step of a conversation: a dialog to show, the quest it is about, and the
 * indices of the options this player is allowed to take. Sending the visible
 * indices rather than a per-option condition flag keeps the click index
 * meaning the same thing on both sides.
 */
public record SimpleDialogToClient(
		int session, int character, ResourceLocation dialog, @Nullable ResourceLocation quest, List<Integer> options
) implements SerialPacketBase<SimpleDialogToClient> {

	@Override
	public void handle(Player player) {
		if (player.level().isClientSide) {
			SimpleDialogScreen.open(this);
		}
	}

}
