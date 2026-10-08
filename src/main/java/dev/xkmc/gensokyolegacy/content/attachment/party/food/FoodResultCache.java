package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodHandler;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a block food state once and remembers the answer.
 *
 * <p>
 * Resolving is not cheap. Finding a serving item costs a registry lookup and a string
 * build, reading a serve count asks the block for its default state and walks the
 * property's range, and a feast allocates an {@code ItemStack} to do it. The party
 * planner scans up to a thirteen-cube box on every send and every guest re-reads the dish
 * it is eating, so the same few hundred states get resolved over and over.
 *
 * <p>
 * Keyed by {@link BlockState}, and therefore bounded without any limit of its own: states
 * are interned per block, so the map can never hold more entries than the blocks it is
 * asked about have states between them — seven for a cake, five for a feast. It does not
 * grow with world size, with uptime, or with how many parties have happened.
 *
 * <p>
 * <b>Nothing here is datapack data.</b> What goes in a result is the serve count, the
 * food properties and the consume action, and all three come from registration — the
 * block's state definition, the item registry, and the handler itself. The
 * {@code party_food} data map is deliberately resolved by the caller in
 * {@code PartyFoodValue} and never enters the cache, so a reload cannot leave a stale
 * appeal behind here.
 *
 * <p>
 * {@link #invalidate()} is wired to the server's datapack sync anyway. It is belt and
 * braces rather than a correctness requirement, and it costs nothing.
 */
public final class FoodResultCache {

	private static final Map<BlockState, FoodResult> RESULTS = new ConcurrentHashMap<>();

	/**
	 * The result for this state. Never null, and never an invalid half-answer: a block
	 * that is not food comes back as {@link FoodResult#INVALID}.
	 */
	public static FoodResult of(BlockState state) {
		var cached = RESULTS.get(state);
		if (cached != null) return cached;
		var resolved = resolve(state);
		RESULTS.put(state, resolved);
		return resolved;
	}

	/** Forget every resolved state. Called on datapack sync. */
	public static void invalidate() {
		RESULTS.clear();
	}

	private static FoodResult resolve(BlockState state) {
		var handler = PartyFoodHandler.of(state.getBlock());
		int serves = Math.max(0, handler.serves(state));
		if (serves <= 0) return FoodResult.INVALID;   // a container with nothing in it
		var food = handler.food(state);
		if (food == null || !servable(food)) return FoodResult.INVALID;
		return new FoodResult(serves, food, handler::consume);
	}

	private static boolean servable(FoodProperties food) {
		return food.nutrition() > 0 && !Servings.refuses(food);
	}

	private FoodResultCache() {
	}
}