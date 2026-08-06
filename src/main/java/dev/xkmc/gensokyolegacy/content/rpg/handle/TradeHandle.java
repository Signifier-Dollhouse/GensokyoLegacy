package dev.xkmc.gensokyolegacy.content.rpg.handle;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.ui.trade.TradeProvider;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public record TradeHandle() implements IDialogHandle {

	@Override
	public Component display() {
		return GLLang.TRADE$OPTION.get();
	}

	@Override
	public void openMenu(ServerPlayer sp, YoukaiEntity character) {
		TradeProvider.open(sp, character);
	}

	@Override
	public Optional<Holder<Quest>> getQuest() {
		return Optional.empty();
	}

}
