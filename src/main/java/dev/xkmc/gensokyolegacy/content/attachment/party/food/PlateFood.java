package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.PlateBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.PlateRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A Kaleidoscope Cookery plate or platter: a stack of one named food, up to five of it.
 * <p>A plate looks like a container and is not one — the state says <i>how many</i> and
 * {@code PlateRegistry} says <i>of what</i>. An ordinary {@link AbstractCountdownFood}.
 *
 * @see "doc/design/party/food_mods.md"
 */
public class PlateFood extends AbstractCountdownFood {

	@Override
	public boolean handles(Block block) {
		return block instanceof PlateBlock;
	}

	/**
	 * The first of the plate's serving items that carries food — a loop rather than a
	 * {@code getFirst}, so a plate whose items are not all food still resolves.
	 */
	@Override
	public ItemStack serving(BlockState state) {
		var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		var data = PlateRegistry.PLATE_DATA_MAP.get(id);
		if (data == null) return ItemStack.EMPTY;   // not a registered plate after all
		for (var serving : data.getServingItems()) {
			if (Servings.of(serving.get().getDefaultInstance()) == null) continue;
			return serving.get().getDefaultInstance();
		}
		return ItemStack.EMPTY;
	}
}
