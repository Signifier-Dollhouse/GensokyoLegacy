package dev.xkmc.gensokyolegacy.content.party.core.food;

import dev.xkmc.youkaishomecoming.content.block.food.FoodSaucerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Youkai's Feasts saucer dish: a single serving, eaten whole, food on the block's own
 * item.
 *
 * <p>
 * There is no serve counter to read — eating the dish empties the saucer rather than
 * stepping a property — so a dish is worth exactly one serve and is gone afterwards.
 *
 * <p>
 * Narrowed to {@code FoodSaucerBlock} rather than its {@code BaseSaucerBlock} parent,
 * because the parent also covers {@code EmptySaucerBlock}, which serves nothing. Being
 * precise here is what lets {@link BlockItemFood} stay a blunt catch-all.
 *
 * <p>
 * <b>Load order:</b> references {@code FoodSaucerBlock}, so it is only ever constructed
 * when Youkai's Feasts is present.
 */
public class SaucerFood extends BlockItemFood {

	@Override
	public boolean handles(Block block) {
		return block instanceof FoodSaucerBlock;
	}

	@Override
	public int serves(BlockState state) {
		return 1;
	}

	@Override
	public @Nullable FoodProperties food(BlockState state) {
		return Servings.of(new ItemStack(state.getBlock()));
	}

	@Override
	public int consume(Level level, BlockPos pos, BlockState state) {
		level.destroyBlock(pos, false);
		return 0;
	}
}