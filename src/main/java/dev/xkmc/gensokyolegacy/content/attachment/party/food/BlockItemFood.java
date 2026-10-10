package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.food.FoodProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Food whose only description is the block's own item: one serving, eaten whole.
 *
 * <p>
 * The base case, and the fallback. Youkai's Feasts' {@code FoodSaucerBlock} is the real
 * user — unlike feasts, pies and pots, a saucer carries its food directly, because
 * {@code FoodSaucerBlock.useWithoutItem} reads {@code state.asItem().getDefaultInstance()}
 * and feeds that to the player — but this class also serves as the last entry in the
 * handler list, which is what lets a datapack author's foreign block be worth something
 * from a tag entry alone.
 *
 * <p>
 * Registered <b>last</b>: {@link #handles} accepts everything, so order is the only
 * thing that keeps it from swallowing the specific handlers. It is an ordinary entry in
 * the list, not a special case in the lookup.
 */
public class BlockItemFood implements BlockFoodHandler {

	@Override
	public boolean handles(Block block) {
		return true;
	}

	@Override
	public int serves(BlockState state) {
		return 1;   // no serve counter: one indivisible serving
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

	/**
	 * Named by the block's own item, because that item <i>is</i> the food — {@link #food}
	 * reads it and this handler exists to serve exactly one of them, eaten whole.
	 */
	@Override
	public ResourceLocation id(BlockState state) {
		return BuiltInRegistries.ITEM.getKey(state.getBlock().asItem());
	}
}