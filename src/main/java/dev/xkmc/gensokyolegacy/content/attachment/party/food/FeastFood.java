package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import vectorwing.farmersdelight.common.block.FeastBlock;

/**
 * A Farmer's Delight feast: servings counted <em>down</em>, food on a serving item rather
 * than on the block — whose own item carries none.
 *
 * <p>Every feast is the same {@code FeastBlock} class but hands out a different item, so
 * the serving is asked of the block rather than looked up in a table.
 *
 * @see doc/design/party/food_mods.md
 */
public class FeastFood extends AbstractCountdownFood {

	@Override
	public boolean handles(Block block) {
		return block instanceof FeastBlock;
	}

	/** The slice a feast hands over, which is both the item and the food; copied because the result is shared. */
	@Override
	public ItemStack serving(BlockState state) {
		if (!(state.getBlock() instanceof FeastBlock feast)) return ItemStack.EMPTY;
		return feast.getServingItem(state).copy();
	}
}