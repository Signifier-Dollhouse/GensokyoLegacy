package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Food whose only description is the block's own item: one serving, eaten whole. Also the
 * catch-all, and registered last for that reason.
 *
 * @see "doc/design/party/food_mods.md"
 */
public class BlockItemFood implements BlockFoodHandler {

	@Override
	public boolean handles(Block block) {
		return true;
	}

	@Override
	public int consume(Level level, BlockPos pos, BlockState state) {
		level.destroyBlock(pos, false);
		return 0;
	}
}