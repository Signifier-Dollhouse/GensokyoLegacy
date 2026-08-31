package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.l2core.serial.recipe.BaseRecipe;
import dev.xkmc.l2core.serial.recipe.BaseRecipeBuilder;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public abstract class AlchemyRecipeBuilder<T extends AlchemyRecipe<T>, B extends AlchemyRecipeBuilder<T, B>>
		extends BaseRecipeBuilder<B, T, AlchemyRecipe<?>, AlchemyInv> {

	protected AlchemyRecipeBuilder(BaseRecipe.RecType<T, AlchemyRecipe<?>, AlchemyInv> type) {
		super(type, Items.AIR);
	}

	public B fluid(FluidIngredient ing) {
		recipe.inputFluid = ing;
		return getThis();
	}

	public B fluid(Fluid fluid) {
		return fluid(FluidIngredient.of(fluid));
	}

	public B fluid(TagKey<Fluid> tag) {
		return fluid(FluidIngredient.tag(tag));
	}

	public B fluid(FluidStack stack) {
		return fluid(FluidIngredient.of(stack));
	}

	public B time(int time) {
		recipe.time = time;
		return getThis();
	}

	public B result(ItemStack stack) {
		recipe.resultItem = stack.copy();
		return getThis();
	}

	public B result(ItemLike item) {
		return result(new ItemStack(item));
	}

	public B result(ItemLike item, int count) {
		return result(new ItemStack(item, count));
	}

	public B resultFluid(FluidStack stack) {
		recipe.resultFluid = stack.copy();
		return getThis();
	}

	public B resultFluid(Fluid fluid, int amount) {
		return resultFluid(new FluidStack(fluid, amount));
	}

	public B resultFluid(Fluid fluid) {
		return resultFluid(fluid, 1000);
	}

	public void save(RegistrateRecipeProvider pvd) {
		ResourceLocation id;
		if (!recipe.resultItem.isEmpty()) {
			var item = recipe.resultItem.getItem();
			var key = item.builtInRegistryHolder().unwrapKey().orElseThrow().location();
			id = pvd.safeId(key);
		} else if (!recipe.resultFluid.isEmpty()) {
			var fluid = recipe.resultFluid.getFluid();
			var key = fluid.builtInRegistryHolder().unwrapKey().orElseThrow().location();
			id = pvd.safeId(key);
		} else {
			throw new IllegalStateException("Alchemy recipe needs result item or fluid");
		}
		super.save(pvd, id);
	}

	public void save(RegistrateRecipeProvider pvd, ResourceLocation id) {
		super.save(pvd, pvd.safeId(id));
	}
}
