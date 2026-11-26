package dev.xkmc.gensokyolegacy.content.rpg.dialog;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecElement;
import dev.xkmc.gensokyolegacy.content.rpg.core.GatedEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

public interface DialogOption<T extends DialogOption<T>> extends CodecElement<T>, GatedEntry {

	String text();

	default String groupKey() {
		return "";
	}

	Component display();

	/**
	 * Picks what this click does. The player and character are here rather than only a random source
	 * so an option can choose its answer from world state - see {@link ConditionalDialogOption}.
	 *
	 * @return the step to run, or null when the option has no answer for the current state
	 */
	@Nullable
	OptionResult resolve(ServerPlayer sp, YoukaiEntity ch, @Nullable RandomSource random);

}
