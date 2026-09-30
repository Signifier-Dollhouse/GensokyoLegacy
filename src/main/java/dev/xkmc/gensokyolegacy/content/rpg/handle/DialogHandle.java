package dev.xkmc.gensokyolegacy.content.rpg.handle;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.ui.dialog.SimpleDialogSession;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public record DialogHandle(Holder<DialogStarter> starter) implements IDialogHandle {

	@Override
	public Component display() {
		return Component.translatable(starter.value().text());
	}

	@Override
	public void open(ServerPlayer sp, YoukaiEntity character) {
		SimpleDialogSession.open(sp, character, this, starter.value().dialog());
	}

	@Override
	public Optional<Holder<Quest>> getQuest() {
		return Optional.empty();
	}

}
