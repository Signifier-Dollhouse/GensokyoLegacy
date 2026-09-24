package dev.xkmc.gensokyolegacy.content.rpg.handle;

import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.ui.dialog.SimpleDialogProvider;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public record QuestHandle(Holder<Quest> quest, DialogOption<?> dialog, Kind kind) implements IDialogHandle {

	public enum Kind {
		START,
		FOLLOW_UP,
		COMPLETE
	}

	@Override
	public Component display() {
		return dialog.display();
	}

	@Override
	public String groupKey() {
		return dialog.groupKey();
	}

	@Override
	public Component groupLabel() {
		return Component.translatable(quest.value().title());
	}

	@Override
	public void openMenu(ServerPlayer sp, YoukaiEntity character) {
		var next = dialog.resolve(sp.getRandom()).next();
		if (next.isEmpty()) return;
		if (character instanceof GeoYoukaiAnim anim) {
			if (kind == Kind.COMPLETE) anim.broadcastAgreeAnim();
			else anim.broadcastThinkAnim();
		}
		new SimpleDialogProvider(sp, character, this, next.get()).open();
	}

	@Override
	public Optional<Holder<Quest>> getQuest() {
		return Optional.of(quest);
	}

}
