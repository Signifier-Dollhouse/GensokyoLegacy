package dev.xkmc.gensokyolegacy.content.item.dagger;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The id → {@link DaggerGloveRune} table the glove stack and the flying dagger both read a rune
 * through. A glove stores a bare id rather than an object, so the rune has to survive a reload and
 * a client that never heard of it.
 * <p>
 * Two consequences, both deliberate: an id with nothing registered behind it reads as {@link #NONE}
 * rather than failing, so a glove that outlives its rune still works; and only the id is saved, so
 * runes are stateless singletons — a rune needing per-stack numbers has to grow a codec on the
 * component instead, which is the open question in dagger_glove.md §7. No rune is registered yet;
 * this class is the seam they go in.
 *
 * @see doc/design/dagger_glove.md
 */
public class DaggerGloveRunes {

	/** The empty rune: no effect, no extra cooldown. Also the fallback for an unknown id. */
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

	/** The rune under this id, or {@link #NONE} if there is none. Never null. */
	public static DaggerGloveRune get(ResourceLocation id) {
		return REGISTRY.getOrDefault(id, NONE);
	}

	/** Every registered rune, by id. Empty until a rune ships. */
	public static Map<ResourceLocation, DaggerGloveRune> all() {
		return Collections.unmodifiableMap(REGISTRY);
	}

}