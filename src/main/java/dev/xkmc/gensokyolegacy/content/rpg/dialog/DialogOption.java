package dev.xkmc.gensokyolegacy.content.rpg.dialog;

import dev.xkmc.gensokyolegacy.content.rpg.core.CodecElement;
import dev.xkmc.gensokyolegacy.content.rpg.core.GatedEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public interface DialogOption<T extends DialogOption<T>> extends CodecElement<T>, GatedEntry {

	String text();

	default String groupKey() {
		return "";
	}

	Component display();

	@Contract("!null -> !null")
	@Nullable
	OptionResult resolve(@Nullable RandomSource random);

}
