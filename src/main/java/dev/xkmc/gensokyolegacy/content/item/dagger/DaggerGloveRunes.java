package dev.xkmc.gensokyolegacy.content.item.dagger;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The id → {@link DaggerGloveRune} table the glove stack and the flying dagger both read through: only
 * the id is saved, so a rune must survive a reload and an unknown id reads as {@link #NONE} rather than
 * failing. A rune needing per-stack numbers has to grow a codec on the component (§7). None registered yet.
 *
 * @see "doc/design/dagger_glove.md"
 */
public class DaggerGloveRunes {

	/**
	 * The empty rune: no effect, no extra cooldown. Also the fallback for an unknown id.
	 */
	public static final DaggerGloveRune NONE = new DaggerGloveRune() {
		@Override
		public int cooldownCost() {
			return 0;
		}

		@Override
		public String toString() {
			return "DaggerGloveRune.NONE";
		}
	};

	public static final ResourceLocation NONE_ID = GensokyoLegacy.loc("none");

	private static final Map<ResourceLocation, DaggerGloveRune> REGISTRY = new HashMap<>();

	/**
	 * Adds a rune under its id, replacing any previous one. Wired during mod construction, beside
	 * the glove itself.
	 */
	public static void register(ResourceLocation id, DaggerGloveRune rune) {
		REGISTRY.put(id, rune);
	}

	/**
	 * The rune under this id, or {@link #NONE} if there is none. Never null.
	 */
	public static DaggerGloveRune get(ResourceLocation id) {
		return REGISTRY.getOrDefault(id, NONE);
	}

	/**
	 * Every registered rune, by id. Empty until a rune ships.
	 */
	public static Map<ResourceLocation, DaggerGloveRune> all() {
		return Collections.unmodifiableMap(REGISTRY);
	}

}