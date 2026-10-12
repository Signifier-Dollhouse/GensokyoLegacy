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
 * The ordered block food handler lists: the first handler whose
 * {@link BlockFoodHandler#handles} accepts the block wins. {@link ContainerFoodHandler}s
 * are kept apart because their answers are never cacheable, and the catch-all fallback
 * lives outside both lists.
 *
 * @see doc/design/party/food.md
 */
public final class PartyFoodHandler {

	private static final List<BlockFoodHandler> HANDLERS = new CopyOnWriteArrayList<>();
	private static final List<ContainerFoodHandler> CONTAINERS = new CopyOnWriteArrayList<>();

	/** Deliberately not in the list: it scores any block whose item is food, and claims nothing. */
	private static final BlockFoodHandler FALLBACK = new BlockItemFood();

	/** Add a handler. Later handlers are only consulted if earlier ones decline. */
	public static void register(BlockFoodHandler handler) {
		HANDLERS.add(handler);
	}

	/**
	 * Add a handler whose food lives in a block entity. Kept in its own list so a container
	 * can never be routed through the state-derived cache.
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
	 * The handler that recognises this block as a kind of food, or null if none does — the
	 * whole of the edibility test. The fallback is never returned, and the data map cannot
	 * opt anything in.
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

}