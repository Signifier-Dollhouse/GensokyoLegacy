package dev.xkmc.gensokyolegacy.content.rpg.handle;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public interface IDialogHandle {

	Component display();

	default String groupKey() {
		return "";
	}

	default Component groupLabel() {
		return display();
	}

	/**
	 * What happens when the player picks this option: another dialog, the
	 * topic list again, or the trade screen.
	 */
	void open(ServerPlayer sp, YoukaiEntity character);

	Optional<Holder<Quest>> getQuest();

}
