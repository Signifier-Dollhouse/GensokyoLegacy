package dev.xkmc.gensokyolegacy.content.item.gift;

import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Per-character gift preferences.
 * A multiplier of 1.0 is neutral, &gt;1 means the character likes the category,
 * 0 means the character completely ignores it. Lookup is keyed by the entity's
 * concrete class, so subclass-specific overrides take precedence.
 */
public record GiftPreference(LinkedHashMap<GiftType, Double> preference, LinkedHashMap<Item, Double> override) {

	public static GiftPreference of(Map<GiftType, Double> preference) {
		return new GiftPreference(new LinkedHashMap<>(new TreeMap<>(preference)), new LinkedHashMap<>());
	}

}
