package dev.xkmc.gensokyolegacy.content.item.gift;

import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Per-character gift preferences: 1.0 is neutral, >1 means the character likes the category, 0 means
 * she will not take it at all. {@code preference} is keyed by category, {@code override} by the single
 * item — the override wins outright. A character with no entry at all is neutral on everything.
 * <p>
 * The datamap is keyed by entity type, so what a whole species shares is one constant handed to each
 * of its registrations rather than a hook on the entity class — see {@code GIFT_FAIRY} in
 * {@code GLEntities}. An entity type named in several entries would not merge, so a character with
 * tastes of her own (Cirno) states both in a single entry.
 *
 * @see "doc/design/gift.md"
 */
public record GiftPreference(LinkedHashMap<GiftType, Double> preference, LinkedHashMap<Item, Double> override) {

	public static GiftPreference of(Map<GiftType, Double> preference) {
		return new GiftPreference(new LinkedHashMap<>(new TreeMap<>(preference)), new LinkedHashMap<>());
	}

}
