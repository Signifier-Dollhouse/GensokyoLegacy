package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.l2core.serial.recipe.BaseRecipe;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.gensokyolegacy.content.fluid.GLHexFluid;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
	@SerialField
	public ItemStack inputFluidItem = ItemStack.EMPTY;
	@SerialField
	public ItemStack outputFluidItem = ItemStack.EMPTY;

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

	public List<ItemStack> getInputFluidItemStacks() {
		if (!inputFluidItem.isEmpty()) return List.of(inputFluidItem.copy());
		if (inputFluid.isEmpty()) return List.of();
		List<ItemStack> out = new ArrayList<>();
		for (FluidStack fs : inputFluid.getStacks()) {
			out.addAll(fluidToItem(fs));
		}
		return out;
	}

	public List<ItemStack> getOutputFluidItemStacks() {
		if (!outputFluidItem.isEmpty()) return List.of(outputFluidItem.copy());
		if (resultFluid.isEmpty()) return List.of();
		return fluidToItem(resultFluid);
	}

	private static List<ItemStack> fluidToItem(FluidStack fs) {
		Fluid fluid = fs.getFluid();
		int amount = fs.getAmount();
		// only use item representation when the fluid amount exactly matches whole containers
		if (fluid instanceof GLHexFluid gl && gl.brew != null) {
			if (amount <= 0 || amount % 250 != 0) return List.of();
			int count = amount / 250;
			ItemStack stack = gl.brew.bottle.asStack(count);
			gl.brew.copyToItem(fs, stack);
			return List.of(stack);
		}
		if (fluid == Fluids.WATER) {
			if (amount <= 0) return List.of();
			List<ItemStack> out = new ArrayList<>();
			if (amount % 1000 == 0) {
				out.add(new ItemStack(Items.WATER_BUCKET, amount / 1000));
			}
			if (amount % 250 == 0) {
				ItemStack bottle = new ItemStack(Items.POTION, amount / 250);
				bottle.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.of(Potions.WATER), Optional.empty(), List.of()));
				out.add(bottle);
			}
			return out;
		}
		Item bucket = fluid.getBucket();
		if (bucket != Items.AIR && amount > 0 && amount % 1000 == 0) {
			return List.of(new ItemStack(bucket, amount / 1000));
		}
		return List.of();
	}

}
