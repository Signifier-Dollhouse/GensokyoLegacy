package dev.xkmc.gensokyolegacy.content.block.pot.stage;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.l2core.serial.recipe.BaseRecipe;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import java.util.List;

@SerialClass
public abstract class AlchemyStageRecipe<T extends AlchemyStageRecipe<T>> extends BaseRecipe<T, AlchemyStageRecipe<?>, AlchemyInv> {

	@SerialField
	public FluidIngredient inputFluid = FluidIngredient.empty();

	public AlchemyStageRecipe(RecType<T, AlchemyStageRecipe<?>, AlchemyInv> fac) {
		super(fac);
	}

	public abstract int getPriority();

	public abstract void removeConsumed(List<ItemStack> list);

	public abstract int getColor(AlchemyInv inv, HolderLookup.Provider access);

	@Override
	public boolean canCraftInDimensions(int i, int j) {
		return true;
	}

	@Override
	public boolean matches(AlchemyInv inv, Level level) {
		if (inv.fluid().isEmpty()) return false;
		return inputFluid.isEmpty() || inputFluid.test(inv.fluid());
	}

	@Override
	public ItemStack assemble(AlchemyInv inv, HolderLookup.Provider access) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider access) {
		return ItemStack.EMPTY;
	}
}
