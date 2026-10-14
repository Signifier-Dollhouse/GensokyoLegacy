package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.BlockPos;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * How one kind of block food is read and eaten, answered in one call so a handler cannot name
 * a food one way and hand over another.
 * <p>Handlers form one ordered list; the first whose {@link #handles} matches wins.
 *
 * @see dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodHandler for the list
 * @see "doc/design/party/food.md"
 */
public interface BlockFoodHandler {

	/** Implementations {@code instanceof} the block class they were written for, so subclasses are covered. */
	boolean handles(Block block);

	/** Assembles the three below; override what differs, inherit the rest. */
	default FoodResult result(BlockState state) {
		return result(serving(state), state.getBlock(), serves(state), food(state));
	}

	/** Servings left; a block with no serve counter is one indivisible serving. */
	default int serves(BlockState state) {
		return 1;
	}

	/** What a guest is handed. Empty for food with no item form, which is then named after its block. */
	default ItemStack serving(BlockState state) {
		return new ItemStack(state.getBlock());
	}

	/** @return null when there is nothing worth serving */
	@Nullable
	default FoodProperties food(BlockState state) {
		return Servings.of(serving(state));
	}

	/**
	 * Advances the placed block to its next state. Read the {@link #result} beforehand — the appeal
	 * of a serving depends on the state before this call.
	 * @return servings remaining
	 */
	int consume(Level level, BlockPos pos, BlockState state);

	/** The single place the "worthless" rule is applied, so no handler can forget it. */
	default FoodResult result(ItemStack serving, Block block, int serves, @Nullable FoodProperties food) {
		return FoodResult.of(serving, block, serves, food, this::consume);
	}
}