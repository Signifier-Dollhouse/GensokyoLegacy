package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A cake: several slices, each valued on its own. {@code instanceof CakeBlock}, so Youkai's Feasts'
 * cakes land here too. Vanilla {@code CakeBlock} carries no {@link FoodProperties} at all, so its
 * slice values are a fallback and a modded slice item is authoritative.
 *
 * @see "doc/design/party/food_mods.md"
 */
public class CakeFood extends AbstractCountdownFood {

	/**
	 * Nutrition and saturation of one slice, from {@code CakeBlock.eat}'s {@code eat(2, 0.1F)}.
	 */
	public static final float SLICE_NUTRITION = 2f;
	public static final float SLICE_SATURATION = 0.4f;

	/**
	 * Vanilla cake's own numbers, used when no slice item can be found.
	 */
	private final FoodProperties fallback = new FoodProperties.Builder()
			.nutrition((int) SLICE_NUTRITION)
			.saturationModifier(SLICE_SATURATION / (SLICE_NUTRITION * 2f))
			.build();

	@Override
	public boolean handles(Block block) {
		return block instanceof CakeBlock;
	}

	/**
	 * The slice item where the mod ships one; empty for vanilla cake, which has no slice item in the game.
	 */
	@Override
	public ItemStack serving(BlockState state) {
		var slice = Permutations.itemOf(state.getBlock());
		return slice == null ? ItemStack.EMPTY : new ItemStack(slice);
	}

	/**
	 * The slice's own values where there is one, vanilla's hard-coded numbers where there is not.
	 */
	@Override
	public @Nullable FoodProperties food(BlockState state) {
		var permuted = Permutations.foodOf(state.getBlock());
		return permuted == null ? fallback : permuted;
	}
}