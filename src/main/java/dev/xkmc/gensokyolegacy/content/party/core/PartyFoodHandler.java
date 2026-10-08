package dev.xkmc.gensokyolegacy.content.party.core;

import dev.xkmc.gensokyolegacy.content.party.core.food.BlockFoodHandler;
import dev.xkmc.gensokyolegacy.content.party.core.food.BlockItemFood;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The ordered list of block food handlers.
 *
 * <p>
 * Resolution is a linear scan and the first handler whose {@link BlockFoodHandler#handles}
 * accepts the block wins. There is no block-to-handler table and no special "default"
 * path: the fallback is just the last entry in the list, an ordinary handler like any
 * other, which is why registering more handlers later is a one-line change.
 *
 * <p>
 * Order matters and is the caller's responsibility. The specific handlers go in first
 * and the catch-all last, because {@code BlockItemFood} accepts everything and would
 * otherwise swallow them all.
 */
public final class PartyFoodHandler {

	private static final List<BlockFoodHandler> HANDLERS = new CopyOnWriteArrayList<>();

	/** Add a handler. Later handlers are only consulted if earlier ones decline. */
	public static void register(BlockFoodHandler handler) {
		HANDLERS.add(handler);
	}

	/** The first handler that covers this block. Never null while the list is set up. */
	public static BlockFoodHandler of(Block block) {
		for (var handler : HANDLERS) {
			if (handler.handles(block)) return handler;
		}
		return MISSING;
	}

	/** Used only if setup never ran; keeps {@link #of} total. */
	private static final BlockFoodHandler MISSING = new BlockItemFood();

	private PartyFoodHandler() {
	}
}