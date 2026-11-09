package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * Snapshot of the pot contents handed to recipe matching. {@code isComplete} is false while probing
 * whether one more item could still fit into a recipe, true for the final match.
 *
 * @see "doc/design/alchemy_pot.md"
 */
public record AlchemyInv(FluidStack fluid, List<ItemStack> list, boolean isComplete) implements Container {

	public int size() {
		return list.size();
	}

	@Override
	public int getContainerSize() {
		return list.size();
	}

	@Override
	public boolean isEmpty() {
		if (!fluid.isEmpty()) return false;
		for (ItemStack s : list) if (!s.isEmpty()) return false;
		return true;
	}

	@Override
	public ItemStack getItem(int i) {
		return list.get(i);
	}

	@Override
	public ItemStack removeItem(int i, int count) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int i) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int i, ItemStack stack) {
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
	}

}
