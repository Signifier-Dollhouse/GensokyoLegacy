package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.attachment.party.food.BlockFoodHandler;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.BlockItemFood;
import net.minecraft.world.level.block.Block;
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
 * Only handlers that recognise a <em>kind</em> of food belong in the list. The
 * catch-all is not one of them — it lives outside the list as the lookup's fallback, so
 * order is only ever a tie-break between two handlers that both claim a block, never a
 * race against one that claims everything.
 */
public final class PartyFoodHandler {

	private static final List<BlockFoodHandler> HANDLERS = new CopyOnWriteArrayList<>();

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

	/** The first handler that claims this block, or the fallback if none does. */
	public static BlockFoodHandler of(Block block) {
		var claimed = claims(block);
		return claimed != null ? claimed : FALLBACK;
	}

	/**
	 * The handler that recognises this block as a kind of food, or null if none does.
	 * <p>
	 * This is the "is this block party food" test, and it is what replaces a
	 * {@code party_food} tag: a block is food because a handler recognises its class, or
	 * because a datapack gave it a {@code party_food} data map entry. The fallback is
	 * never returned from here, so an unclaimed block is not food just because food
	 * happened to be looking at it.
	 */
	@Nullable
	public static BlockFoodHandler claims(Block block) {
		for (var handler : HANDLERS) {
			if (handler.handles(block)) return handler;
		}
		return null;
	}

	private PartyFoodHandler() {
	}
}