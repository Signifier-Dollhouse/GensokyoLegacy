package dev.xkmc.gensokyolegacy.content.item.dagger;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The id → {@link DaggerGloveRune} table the glove stack and the flying dagger both read a rune
 * through.
 * <p>
 * A glove stores its rune as a bare id (the {@code DAGGER_GLOVE_RUNE} component, and the
 * {@code IronDaggerBulletEntity} field), not as an object, so the rune has to survive a reload
 * and a client that never heard of it. Two consequences, both deliberate:
 * <ul>
 * <li>An id with nothing registered behind it reads as {@link #NONE} rather than failing. A glove
 * that outlives the rune applied to it still works, just without the extra effect — a stack of
 * daggers is never bricked by a content update.</li>
 * <li>Only the <em>id</em> is saved. Runes are therefore stateless singletons; a rune that needs
 * per-stack numbers (a stored damage bonus, say) has to grow a codec on the component instead,
 * which is recorded as an open question in {@code doc/design/dagger_glove.md} §7.</li>
 * </ul>
 * No rune is registered yet — this class is the seam they go in.
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
	 * Adds a rune under its id, replacing any previous one. Runes are wired during mod
	 * construction, alongside the glove itself.
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