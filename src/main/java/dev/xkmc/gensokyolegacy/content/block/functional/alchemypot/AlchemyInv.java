package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record AlchemyInv(FluidStack fluid, List<ItemStack> list, boolean isComplete) implements RecipeInput {

	@Override
	public int size() {
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

}
