package dev.xkmc.gensokyolegacy.content.rpg.handle;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.action.ActionContext;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.ui.dialog.SimpleDialogSession;
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
	public void open(ServerPlayer sp, YoukaiEntity character) {
		var result = dialog.resolve(sp.getRandom());
		if (result == null) return;
		// The topic option is a step of the conversation like any other, so its
		// actions run on click - a quest may be started here rather than from an
		// option inside the first dialog.
		var context = new ActionContext(sp, character, getQuest());
		for (var e : result.actions()) {
			e.execute(context);
		}
		var next = result.next();
		if (next.isEmpty()) return;
		SimpleDialogSession.open(sp, character, this, next.get());
	}

	@Override
	public Optional<Holder<Quest>> getQuest() {
		return Optional.of(quest);
	}

}
