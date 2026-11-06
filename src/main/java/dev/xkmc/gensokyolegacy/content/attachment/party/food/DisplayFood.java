package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import com.jkvin114.displaydelight.block.AbstractItemBlock;
import com.jkvin114.displaydelight.block.AbstractStackablePlatedFoodBlock;
import com.jkvin114.displaydelight.block.SmallPlatedFoodBlock;
import com.jkvin114.displaydelight.init.DisplayBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Display Delight display. The mod builds one block instance per food item, so the serving is asked of
 * the block rather than of a table; only the plated displays carry a stack count.
 *
 * @see "doc/design/party/food_mods.md"
 */
public class DisplayFood implements BlockFoodHandler {

	@Override
	public boolean handles(Block block) {
		// the display base class; EmptyPlateBlock extends Block, so a bare plate falls through
		return block instanceof AbstractItemBlock;
	}

	@Override
	public ItemStack serving(BlockState state) {
		if (!(state.getBlock() instanceof AbstractItemBlock block)) return ItemStack.EMPTY;
		return block.getStackFor();
	}

	@Override
	public int serves(BlockState state) {
		if (state.getBlock() instanceof AbstractStackablePlatedFoodBlock plated) return plated.getStacks(state);
		return 1;
	}

	@Override
	public int consume(Level level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof AbstractStackablePlatedFoodBlock plated && serves(state) > 1) {
			level.setBlock(pos, plated.getDecrementedState(state), 3);
			return serves(state) - 1;
		}
		// The stack count runs 1..max and its decrement clamps at 1, so it never reaches zero and the
		// block cannot step itself empty — what is left when the last serving goes is the plate.
		var plate = plateLeftBy(state);
		if (plate == Blocks.AIR) level.destroyBlock(pos, false);
		else level.setBlock(pos, plate.defaultBlockState(), 3);
		return 0;
	}

	/**
	 * The plate under the display, or {@link Blocks#AIR} when it was standing on its own.
	 */
	private static Block plateLeftBy(BlockState state) {
		if (state.getBlock() instanceof AbstractStackablePlatedFoodBlock plated) {
			return plated.hasPlate(state) ? DisplayBlocks.PLATE.get() : Blocks.AIR;
		}
		if (state.getBlock() instanceof SmallPlatedFoodBlock plated) {
			return plated.hasPlate(state) ? DisplayBlocks.SMALL_PLATE.get() : Blocks.AIR;
		}
		return Blocks.AIR;
	}
}