package dev.xkmc.gensokyolegacy.content.item.gift;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * Category of a gift. Characters have per-category preferences
 * (see {@link GiftPreference}), so the same category is worth different
 * favor to different characters.
 */
public enum GiftType {
	TOY("Toy"),
	FOOD("Food"),
	DRINK("Drink"),
	BOOK("Book"),
	FAN("Fan"),
	CRYSTAL("Crystal"),
	MAGIC("Magical");

	private final String lang;

	GiftType(String lang) {
		this.lang = lang;
	}

	public String getLangName() {
		return lang;
	}

	public MutableComponent getDisplay() {
		return Component.translatable("gensokyolegacy.gift.type." + name().toLowerCase(Locale.ROOT));
	}
}
