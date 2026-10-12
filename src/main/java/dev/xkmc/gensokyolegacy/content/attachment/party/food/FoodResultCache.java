package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodHandler;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a block food state once and remembers the answer. Bounded without a limit:
 * states are interned per block. Containers never route through here — anything cached
 * must be answerable from the state alone.
 *
 * @see doc/design/party/food.md
 */
public final class FoodResultCache {

	private static final Map<BlockState, FoodResult> RESULTS = new ConcurrentHashMap<>();

	/** Never null, and never a half-answer: a non-food block comes back as {@link FoodResult#INVALID}. */
	public static FoodResult of(BlockState state) {
		var cached = RESULTS.get(state);
		if (cached != null) return cached;
		var resolved = resolve(state);
		RESULTS.put(state, resolved);
		return resolved;
	}

	/** Belt and braces on datapack sync: nothing cached here is itself datapack data. */
	public static void invalidate() {
		RESULTS.clear();
	}

	private static FoodResult resolve(BlockState state) {
		return PartyFoodHandler.of(state.getBlock()).result(state);
	}

	private FoodResultCache() {
	}
}