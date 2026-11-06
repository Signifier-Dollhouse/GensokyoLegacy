package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot;

import dev.xkmc.l2core.base.tile.BaseContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Item slots that gate every insert on the pot's recipe probe, so hoppers and pipes are filtered
 * exactly like a right-click holding an item.
 *
 * @see "doc/design/alchemy_pot.md"
 */
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
