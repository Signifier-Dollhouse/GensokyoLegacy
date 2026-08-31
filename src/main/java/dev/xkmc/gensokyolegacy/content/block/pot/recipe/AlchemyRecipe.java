package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.l2core.serial.recipe.BaseRecipe;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import java.util.List;

@SerialClass
public abstract class AlchemyRecipe<T extends AlchemyRecipe<T>> extends BaseRecipe<T, AlchemyRecipe<?>, AlchemyInv> implements TimedRecipe {

	@SerialField
	public FluidIngredient inputFluid = FluidIngredient.empty();
	@SerialField
	public int time;
	@SerialField
	public ItemStack resultItem = ItemStack.EMPTY;
	@SerialField
	public FluidStack resultFluid = FluidStack.EMPTY;

	public AlchemyRecipe(RecType<T, AlchemyRecipe<?>, AlchemyInv> fac) {
		super(fac);
	}

	public abstract List<Ingredient> getInputItems();

	@Override
	public boolean canCraftInDimensions(int i, int j) {
		return true;
	}

	@Override
	public boolean matches(AlchemyInv inv, Level level) {
		if (inv.fluid().isEmpty()) return false;
		if (!inputFluid.test(inv.fluid())) return false;
		return true;
	}

	@Override
	public ItemStack assemble(AlchemyInv inv, HolderLookup.Provider access) {
		return resultItem.copy();
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider access) {
		return resultItem.copy();
	}

	public abstract List<Ingredient> getHints(Level level, AlchemyInv inv);

	@Override
	public int getProcessTime() {
		return time;
	}

	public FluidStack getResultFluid(AlchemyInv inv, HolderLookup.Provider access) {
		return resultFluid.copy();
	}

}
