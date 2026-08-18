package dev.xkmc.gensokyolegacy.content.item.gift;

import dev.xkmc.gensokyolegacy.content.entity.characters.fairy.CirnoEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.maiden.ReimuEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-character gift preferences.
 * A multiplier of 1.0 is neutral, &gt;1 means the character likes the category,
 * 0 means the character completely ignores it. Lookup is keyed by the entity's
 * concrete class, so subclass-specific overrides take precedence.
 */
public class GiftPreference {

	private static final Map<Class<? extends YoukaiEntity>, EnumMap<GiftType, Double>> TABLE = new HashMap<>();

	static {
		// example preferences; extend as more characters/gifts are added
		set(CirnoEntity.class, GiftType.TOY, 2.0);
		set(CirnoEntity.class, GiftType.FOOD, 1.5);
		set(CirnoEntity.class, GiftType.BOOK, 0.5);
		set(ReimuEntity.class, GiftType.DRINK, 2.0);
		set(ReimuEntity.class, GiftType.BOOK, 1.5);
	}

	public static void set(Class<? extends YoukaiEntity> cls, GiftType type, double mult) {
		TABLE.computeIfAbsent(cls, k -> new EnumMap<>(GiftType.class)).put(type, mult);
	}

	public static double get(YoukaiEntity e, GiftType type) {
		var map = TABLE.get(e.getClass());
		if (map == null) return 1.0;
		return map.getOrDefault(type, 1.0);
	}

}
