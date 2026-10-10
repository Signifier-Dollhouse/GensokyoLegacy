package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.attachment.party.food.BlockFoodHandler;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.BlockItemFood;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.ContainerFoodHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The ordered list of block food handlers.
 *
 * <p>
 * Resolution is a linear scan and the first handler whose {@link BlockFoodHandler#handles}
 * accepts the block wins. There is no block-to-handler table, and registering another
 * kind of food is a one-line change.
 *
 * <p>
 * Two lists, because there are two kinds of food. A {@link BlockFoodHandler} reads its
 * answer off the block state and can therefore be cached; a {@link ContainerFoodHandler}
 * reads it out of a block entity and must not be — see {@link ContainerFoodHandler}. The
 * two are never mixed, and the container list is consulted first, because a container is
 * not a food the catch-all should be reaching for.
 *
 * <p>
 * Only handlers that recognise a <em>kind</em> of food belong in the list. The
 * catch-all is not one of them — it lives outside the list as the lookup's fallback, so
 * order is only ever a tie-break between two handlers that both claim a block, never a
 * race against one that claims everything.
 */
public final class PartyFoodHandler {

	private static final List<BlockFoodHandler> HANDLERS = new CopyOnWriteArrayList<>();
	private static final List<ContainerFoodHandler> CONTAINERS = new CopyOnWriteArrayList<>();

	/**
	 * The handler for blocks no registered handler claims. Deliberately
	 * <b>not in the list</b>: it is how a datapack's foreign food is scored, not a
	 * declaration that any block at all is food.
	 */
	private static final BlockFoodHandler FALLBACK = new BlockItemFood();

	/** Add a handler. Later handlers are only consulted if earlier ones decline. */
	public static void register(BlockFoodHandler handler) {
		HANDLERS.add(handler);
	}

	/**
	 * Add a handler whose food lives in a block entity.
	 * <p>
	 * Kept in its own list rather than interleaved, so that nothing can accidentally route
	 * a container block through {@link FoodResultCache} by way of a state-derived handler
	 * that happens to match its class first.
	 */
	public static void register(ContainerFoodHandler handler) {
		CONTAINERS.add(handler);
	}

	/** The first handler that claims this block, or the fallback if none does. */
	public static BlockFoodHandler of(Block block) {
		var claimed = claims(block);
		return claimed != null ? claimed : FALLBACK;
	}

	/**
	 * The handler that recognises this block as a kind of food, or null if none does.
	 * <p>
	 * This is the "is this block party food" test, and it is the <b>whole</b> of it: a
	 * block is food because a handler recognises its class and finds something worth
	 * eating in it. The {@code party_food} data map is not consulted here and cannot opt
	 * anything in — it only revalues a block that already passed. The fallback is never
	 * returned from here, so an unclaimed block is not food just because food happened to
	 * be looking at it.
	 */
	@Nullable
	public static BlockFoodHandler claims(Block block) {
		for (var handler : HANDLERS) {
			if (handler.handles(block)) return handler;
		}
		return null;
	}

	/** The container handler for this block, or null if it is not a container food. */
	@Nullable
	public static ContainerFoodHandler containerOf(Block block) {
		for (var handler : CONTAINERS) {
			if (handler.handles(block)) return handler;
		}
		return null;
	}

	/**
	 * Whether this block is party food <i>in this world, right now</i>.
	 * <p>
	 * A separate question from {@link #claims}, because a container that is food may be
	 * empty: the block is claimed either way, but only a container holding something worth
	 * eating is food. Callers that only need the block's class answered use
	 * {@link #claims}, which cannot be answered without a level.
	 */
	public static boolean claimsHere(Level level, BlockPos pos, BlockState state) {
		var container = containerOf(state.getBlock());
		if (container != null) {
			for (var result : container.contents(level, pos, state)) {
				if (result.valid()) return true;
			}
			return false;
		}
		return claims(state.getBlock()) != null;
	}

	private PartyFoodHandler() {
	}
}