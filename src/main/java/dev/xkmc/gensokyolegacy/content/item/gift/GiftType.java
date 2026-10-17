package dev.xkmc.gensokyolegacy.content.item.gift;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * Category of a gift. Characters have per-category preferences (see {@link GiftPreference}), so the same
 * category is worth different favor to different characters.
 *
 * @see "doc/design/gift.md"
 */
public enum GiftType {
	TOY("Toy"),
	FOOD("Food"),
	DRINK("Drink"),
	ALCOHOL("Alcohol"),
	BOOK("Book"),
	MAGIC("Magical"),
	UTENSIL("Utensil"),
	TOOL("Tool"),
	// neutral 0 rather than 1: a frozen frog is a fairy's delicacy and nobody else's, so a
	// character who has never heard of the category refuses it instead of taking it at face value
	FROG("Frog", 0.0);

	/**
	 * What a character who has no opinion of the category at all is worth: 1.0 for everything
	 * anyone will take, and 0.0 for a category only a character who asks for it by name wants.
	 */
	private static final double NEUTRAL = 1.0;

	private final String lang;
	private final double neutral;

	GiftType(String lang) {
		this(lang, NEUTRAL);
	}

	GiftType(String lang, double neutral) {
		this.lang = lang;
		this.neutral = neutral;
	}

	public String getLangName() {
		return lang;
	}

	public double neutral() {
		return neutral;
	}

	public MutableComponent getDisplay() {
		return Component.translatable("gensokyolegacy.gift.type." + name().toLowerCase(Locale.ROOT));
	}
}