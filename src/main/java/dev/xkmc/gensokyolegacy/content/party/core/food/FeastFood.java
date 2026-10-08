package dev.xkmc.gensokyolegacy.content.party.core.food;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import vectorwing.farmersdelight.common.block.FeastBlock;
import org.jetbrains.annotations.Nullable;

/**
 * A Farmer's Delight feast: a set number of servings that counts <em>down</em>, and whose
 * food lives on a serving item rather than on the block.
 *
 * <p>
 * {@code FeastBlock} extends {@code IntegerProperty.create("servings", 0, 4)} and starts
 * at its <em>maximum</em>, so it counts down — the opposite of vanilla cake. That is why
 * {@link AbstractCountdownFood} reads the direction off the block rather than assuming
 * one.
 *
 * <p>
 * The block's own item carries no food at all: clicking a feast hands the player a
 * serving item. Every feast is the same {@code FeastBlock} class but each hands out a
 * different item, so the serving item is asked of the block through
 * {@link FeastBlock#getServingItem} rather than looked up in a table. That is what lets
 * one handler serve all six dishes and any the mod adds later — and why this class
 * needs no per-block registration at all.
 *
 * <p>
 * <b>Load order:</b> this class references {@code FeastBlock}, so it can only be loaded
 * when Farmer's Delight is present. {@code BlockFoods} only constructs it inside its
 * gated section, and a handler that was never constructed is never asked.
 */
public class FeastFood extends AbstractCountdownFood {

	@Override
	public boolean handles(Block block) {
		return block instanceof FeastBlock;
	}

	@Override
	public @Nullable FoodProperties food(BlockState state) {
		if (!(state.getBlock() instanceof FeastBlock feast)) return null;
		return Servings.of(feast.getServingItem(state));
	}
}