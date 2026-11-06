package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import vectorwing.farmersdelight.common.block.PieBlock;

/**
 * A Farmer's Delight pie: four bites counted <em>up</em>, food on a slice item rather than on the
 * block. Every pie is the same class but has its own slice, so the slice is asked of the block;
 * Youkai's Feasts' {@code YHPieBlock} extends {@code PieBlock}.
 *
 * @see "doc/design/party/food_mods.md"
 */
public class PieFood extends AbstractCountdownFood {

	@Override
	public boolean handles(Block block) {
		return block instanceof PieBlock;
	}

	/**
	 * The cut slice, which is both the item handed over and where its food is read; copied because the result is
	 * shared.
	 */
	@Override
	public ItemStack serving(BlockState state) {
		if (!(state.getBlock() instanceof PieBlock pie)) return ItemStack.EMPTY;
		return pie.getPieSliceItem().copy();
	}
}