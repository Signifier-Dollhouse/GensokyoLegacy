package dev.xkmc.gensokyolegacy.content.block.pot;

import dev.xkmc.l2core.base.tile.BaseContainer;
import net.minecraft.world.item.ItemStack;

public class AlchemyItemContainer extends BaseContainer<AlchemyItemContainer> {

	private final AlchemyPotBlockEntity be;

	public AlchemyItemContainer(AlchemyPotBlockEntity be, int size) {
		super(size);
		this.be = be;
		add(be);
	}

	@Override
	public boolean canAddItem(ItemStack stack) {
		return be.tryAddItem(stack, true) && super.canAddItem(stack);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return (stack.isEmpty() || be.tryAddItem(stack, true)) && super.canPlaceItem(slot, stack);
	}
}
