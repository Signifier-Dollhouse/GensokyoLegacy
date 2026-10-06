package dev.xkmc.gensokyolegacy.content.party.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ties {@link BlockFoodHandler}s to block <b>classes</b> rather than to block
 * instances.
 * <p>
 * The lookup walks the class hierarchy, so registering a handler for a base class
 * covers every subclass registered after it. That is what makes the third-party
 * food lists short: Farmer's Delight's feasts all extend {@code FeastBlock}, YSF's
 * saucer foods all extend {@code BaseSaucerBlock}, and one registration each covers
 * every dish, pie and plate those mods add — including any they add later.
 * <p>
 * Registration is per-class, resolved lazily on first lookup, so no mod has to be
 * loaded for the registration to be written down. Everything is additionally gated on
 * the owning mod being present before it is resolved, which means a datapack author's
 * foreign block still scores from its own item's food properties (§2.2's fallback)
 * rather than from a handler meant for a different mod.
 * <p>
 * Resolution is cached per block instance. Classes are matched most-derived first,
 * so a {@code YHPieBlock} is found under {@code PieBlock} before any {@code Block}
 * -wide default.
 */
public final class PartyFoodHandler {

	/**
	 * The fallback for any block with no more specific handler: one serve, worth
	 * whatever its own item is worth.
	 */
	private static final BlockFoodHandler DEFAULT = BlockFoodHandler.whole(null);

	/** Class -> handler; the most derived registered class wins. */
	private static final Map<Class<?>, BlockFoodHandler> BY_CLASS = new ConcurrentHashMap<>();

	/**
	 * Block -> handler, beating any class match. Needed because several blocks of one
	 * class read different food: every Farmer's Delight feast is a {@code FeastBlock},
	 * but each hands out its own serving item.
	 */
	private static final Map<Block, BlockFoodHandler> BY_BLOCK = new ConcurrentHashMap<>();

	/** Resolved per block instance, so the hierarchy walk happens once per block. */
	private static final Map<Block, BlockFoodHandler> RESOLVED = new ConcurrentHashMap<>();

	/**
	 * Teach the party system how to read every block of this class and its
	 * subclasses.
	 * <p>
	 * No-op if the mod owning the class is absent, so a registration list can name
	 * foreign classes unconditionally.
	 *
	 * @param clazz   the base block class; subclasses are covered automatically
	 * @param handler how to read and eat one of its blocks
	 * @param mod     the mod that owns the class, checked before registration; null
	 *                to always apply the handler
	 */
	public static void register(Class<? extends Block> clazz, BlockFoodHandler handler, @Nullable String mod) {
		if (!loaded(mod)) return;
		BY_CLASS.put(clazz, handler);
		RESOLVED.clear();   // a new base class may cover blocks already resolved
	}

	/**
	 * Register a handler for one block only, beating any class match. For the rare
	 * case where blocks of a single class read different food — Farmer's Delight's
	 * feasts all extend {@code FeastBlock} but each hands out its own serving item.
	 *
	 * @param mod the mod that owns the block, checked before registration
	 */
	public static void register(Block block, BlockFoodHandler handler, @Nullable String mod) {
		if (!loaded(mod)) return;
		BY_BLOCK.put(block, handler);
		RESOLVED.remove(block);
	}

	/**
	 * The handler for this block, never null. Walks up from the block's own class
	 * and takes the first registered match; falls back to {@link #DEFAULT} so a
	 * block in {@code party_food} is always worth something.
	 */
	public static BlockFoodHandler of(Block block) {
		var cached = RESOLVED.get(block);
		if (cached != null) return cached;
		var found = resolve(block);
		RESOLVED.put(block, found);
		return found;
	}

	private static BlockFoodHandler resolve(Block block) {
		var own = BY_BLOCK.get(block);
		if (own != null) return own;
		// most-derived first, so a subclass registered explicitly beats its base.
		// Explicitly Class<?>: getClass() returns a capture whose superclass is
		// Class<? super T>, which will not unify with a Class<? extends Block> loop.
		// Explicitly Class<?>: getClass() returns a capture whose superclass is
		// Class<? super T>, which will not unify with a Class<? extends Block> loop.
		for (Class<?> raw = block.getClass(); raw != null; raw = raw.getSuperclass()) {
			var handler = BY_CLASS.get(raw);
			if (handler != null) return handler;
			if (raw == Block.class) break;
		}
		return DEFAULT;
	}

	/** Whether a handler was found for this block, as opposed to the default. */
	public static boolean isRegistered(Block block) {
		return RESOLVED.getOrDefault(block, resolve(block)) != DEFAULT;
	}

	/**
	 * Whether a mod id is loaded. A null or empty id means "no gate", which is what
	 * vanilla and our own blocks use.
	 */
	private static boolean loaded(@Nullable String mod) {
		return mod == null || mod.isEmpty() || ModList.get() == null || ModList.get().isLoaded(mod);
	}

	/** Forgets every resolved block. Only useful if handlers change at runtime. */
	public static void clearResolved() {
		RESOLVED.clear();
	}

	private PartyFoodHandler() {
	}
}