package dev.xkmc.gensokyolegacy.content.rpg.handle;

import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Client-side mirror of an {@link IDialogHandle}: the label to draw and the quest the option is about, carried as an id
 * because the packet codec cannot write a datapack registry holder; the client resolves it back through {@link
 * CodecRegistry.Keys#QUEST}.
 */
public record ClientHandle(Component display, @Nullable ResourceLocation quest) {

	/**
	 * The id form of a quest holder, or null when there is no quest or the
	 * holder is a direct reference with no key.
	 */
	public static @Nullable ResourceLocation questId(Optional<Holder<Quest>> quest) {
		return quest.flatMap(Holder::unwrapKey).map(k -> k.location()).orElse(null);
	}

}
