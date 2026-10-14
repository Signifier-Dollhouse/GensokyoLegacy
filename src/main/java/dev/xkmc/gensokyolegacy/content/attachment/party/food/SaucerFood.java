package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.youkaishomecoming.content.block.food.FoodSaucerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A Youkai's Feasts saucer dish: one serving, eaten whole, food on the block's own item.
 * <p>Narrowed to {@code FoodSaucerBlock} rather than its {@code BaseSaucerBlock} parent, which
 * also covers {@code EmptySaucerBlock} and would serve nothing.
 *
 * @see "doc/design/party/food_mods.md"
 */
public class SaucerFood extends BlockItemFood {

	@Override
	public boolean handles(Block block) {
		return block instanceof FoodSaucerBlock;
	}

	@Override
	public int consume(Level level, BlockPos pos, BlockState state) {
		level.destroyBlock(pos, false);
		return 0;
	}
}