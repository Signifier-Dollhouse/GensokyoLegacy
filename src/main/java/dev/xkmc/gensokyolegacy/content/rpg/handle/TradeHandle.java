package dev.xkmc.gensokyolegacy.content.rpg.handle;

import dev.xkmc.gensokyolegacy.content.attachment.datamap.DialogConfig;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.ui.trade.TradeProvider;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

public record TradeHandle(EntityType<?> type) implements IDialogHandle {

	@Override
	public Component display() {
		var cfg = DialogConfig.of(type);
		if (cfg != null && !cfg.trade().isEmpty())
			return Component.translatable(cfg.trade());
		return GLLang.Trade.OPTION.get();
	}

	@Override
	public void open(ServerPlayer sp, YoukaiEntity character) {
		// the trade screen is a real container menu, so it keeps the
		// conversation alive on its own; the dialog session standing down is
		// handled by the client's close packet
		TradeProvider.open(sp, character);
	}

	@Override
	public Optional<Holder<Quest>> getQuest() {
		return Optional.empty();
	}

}
