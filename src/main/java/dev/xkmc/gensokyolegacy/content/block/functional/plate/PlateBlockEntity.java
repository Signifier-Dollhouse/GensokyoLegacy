package dev.xkmc.gensokyolegacy.content.block.functional.plate;

import dev.xkmc.l2core.base.tile.BaseBlockEntity;
import dev.xkmc.l2modularblock.tile_api.BlockContainer;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The one item a plate is serving. {@link BlockContainer} is what makes it drop with the plate: the
 * delegate block's removal hook empties every container a removed block entity hands it, so breaking
 * the plate cannot void the food.
 */
@SerialClass
public class PlateBlockEntity extends BaseBlockEntity implements BlockContainer {

	@SerialField
	public ItemStack stack = ItemStack.EMPTY;

	public PlateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** Empties the plate and hands back what was on it. */
	public ItemStack clear() {
		ItemStack out = stack;
		stack = ItemStack.EMPTY;
		notifyTile();
		return out;
	}

	/** Hand the food to the player, dropping it at the plate's feet if there is no room. */
	public void take(Player player) {
		ItemStack out = clear();
		if (!player.getInventory().add(out)) player.drop(out, false);
	}

	public void notifyTile() {
		setChanged();
		sync();
	}

	@Override
	public List<Container> getContainers() {
		SimpleContainer c = new SimpleContainer(1);
		if (!stack.isEmpty()) c.addItem(stack.copy());
		return List.of(c);
	}

}
