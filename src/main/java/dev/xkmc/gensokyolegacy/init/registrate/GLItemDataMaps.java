package dev.xkmc.gensokyolegacy.init.registrate;

import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.MorichikaOfferData;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftItemData;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Gift and shelf-offer data authored next to each item. The entry is kept unresolved because
 * {@code get()} is illegal in the static initializer, before the registry is bound; config
 * datagen resolves it.
 */
public final class GLItemDataMaps {

	public record Entry(RegistryEntry<? extends ItemLike> entry, @Nullable GiftItemData gift,
						@Nullable MorichikaOfferData offer) {}

	private static final List<Entry> ENTRIES = new ArrayList<>();

	private GLItemDataMaps() {}

	public static void add(RegistryEntry<? extends ItemLike> entry, @Nullable GiftItemData gift,
						   @Nullable MorichikaOfferData offer) {
		ENTRIES.add(new Entry(entry, gift, offer));
	}

	public static Stream<Entry> entries() {
		return ENTRIES.stream();
	}

}
