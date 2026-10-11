package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Kaleidoscope Cookery dish: a placed block eaten one bite at a time.
 *
 * <p>Not an {@link AbstractCountdownFood}: Cookery's counter stops at the maximum and leaves
 * an emptied block standing, so serves are {@code maxBites - bites}, not {@code bites + 1}.
 *
 * @see doc/design/party/food_mods.md
 */
public class BiteFood implements BlockFoodHandler {

	@Override
	public boolean handles(Block block) {
		// also FoodBiteOneByTwoBlock, whose two halves are independently eatable
		return block instanceof FoodBiteBlock;
	}

	@Override
	public int serves(BlockState state) {
		if (!(state.getBlock() instanceof FoodBiteBlock bite)) return 0;
		return Math.max(0, bite.getMaxBites() - state.getValue(bite.getBites()));
	}

	/**
	 * Cookery has no item for a single bite — the dish item is the whole plate — so the
	 * properties come from the mod's own public registry table, never from a serving item.
	 * {@code itemFood()} there is the whole dish and must not be used.
	 */
	@Override
	public @Nullable FoodProperties food(BlockState state) {
		var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		var data = FoodBiteRegistry.FOOD_DATA_MAP.get(id);
		if (data == null) return null;   // not registered as a bite block after all
		return data.blockFood();
	}

	@Override
	public int consume(Level level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof FoodBiteBlock bite) || serves(state) <= 1) {
			// Cookery leaves an emptied dish standing; we remove it, so a spent dish does
			// not sit on the table forever.
			level.destroyBlock(pos, false);
			return 0;
		}
		var bites = bite.getBites();
		level.setBlock(pos, state.setValue(bites, state.getValue(bites) + 1), 3);
		return serves(state) - 1;
	}
}
