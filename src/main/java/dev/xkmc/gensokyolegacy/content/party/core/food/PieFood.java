package dev.xkmc.gensokyolegacy.content.party.core.food;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import vectorwing.farmersdelight.common.block.PieBlock;
import org.jetbrains.annotations.Nullable;

/**
 * A Farmer's Delight pie: bites that count <em>up</em>, food on a slice item.
 *
 * <p>
 * {@code PieBlock} extends {@code IntegerProperty.create("bites", 0, 3)} starting at
 * zero, so a whole pie is four bites. Its block item carries no food; the slice does,
 * and {@link AbstractCountdownFood} handles the counting.
 *
 * <p>
 * As with {@link FeastFood}, every pie is the same class but each has its own slice item,
 * so the slice is asked of the block through {@link PieBlock#getPieSliceItem}. Youkai's
 * Feasts' {@code YHPieBlock} extends {@code PieBlock} and so is covered by the same
 * {@code instanceof}.
 *
 * <p>
 * <b>Load order:</b> references {@code PieBlock}, so it is only ever constructed when
 * Farmer's Delight is present.
 */
public class PieFood extends AbstractCountdownFood {

	@Override
	public boolean handles(Block block) {
		return block instanceof PieBlock;
	}

	@Override
	public @Nullable FoodProperties food(BlockState state) {
		if (!(state.getBlock() instanceof PieBlock pie)) return null;
		return Servings.of(pie.getPieSliceItem());
	}
}