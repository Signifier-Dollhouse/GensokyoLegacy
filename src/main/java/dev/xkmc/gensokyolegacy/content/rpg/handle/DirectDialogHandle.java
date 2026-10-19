package dev.xkmc.gensokyolegacy.content.rpg.handle;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.Dialog;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.ui.dialog.SimpleDialogSession;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * A dialog addressed directly, with no {@link DialogStarter} behind it — what a character opens on the
 * spot when a gift changes hands, rather than something the player picks off the topic list. It has
 * therefore no group and no quest, and nothing ever navigates out of it.
 */
public record DirectDialogHandle(Holder<Dialog> dialog) implements IDialogHandle {

	@Override
	public Component display() {
		return Component.translatable(dialog.value().text());
	}

	@Override
	public void open(ServerPlayer sp, YoukaiEntity character) {
		SimpleDialogSession.open(sp, character, this, dialog);
	}

	@Override
	public Optional<Holder<Quest>> getQuest() {
		return Optional.empty();
	}

}