package dev.xkmc.gensokyolegacy.content.rpg.handle;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.ui.dialog.FirstDialogSession;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public record GroupHandle(Component display, List<IDialogHandle> members, Component text) implements IDialogHandle {

	@Override
	public Component display() {
		return display;
	}

	@Override
	public Component groupLabel() {
		return display;
	}

	@Override
	public void open(ServerPlayer sp, YoukaiEntity character) {
		FirstDialogSession.openGroup(sp, character, this);
	}

	@Override
	public Optional<Holder<Quest>> getQuest() {
		return Optional.empty();
	}

}