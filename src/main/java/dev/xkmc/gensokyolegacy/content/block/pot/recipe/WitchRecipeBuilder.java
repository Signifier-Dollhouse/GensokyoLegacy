package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2core.serial.recipe.BaseRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public class WitchRecipeBuilder extends BaseRecipeBuilder<WitchRecipeBuilder, WitchRecipe, AlchemyRecipe<?>, AlchemyInv> {

	private final ResourceLocation id;

	public WitchRecipeBuilder(ResourceLocation id) {
		super(GLRecipes.ALCHEMY_WITCH.get(), Items.AIR);
		this.id = id;
		recipe.potionCount = 0;
	}

	public WitchRecipeBuilder potionCount(int count) {
		recipe.potionCount = count;
		return getThis();
	}

	public WitchRecipeBuilder add(Ingredient ing) {
		recipe.extra.add(ing);
		return this;
	}

	public WitchRecipeBuilder add(ItemLike item) {
		recipe.extra.add(Ingredient.of(item));
		return this;
	}

	public WitchRecipeBuilder add(TagKey<Item> tag) {
		recipe.extra.add(Ingredient.of(tag));
		return this;
	}

	public WitchRecipeBuilder fluid(FluidIngredient ing) {
		recipe.inputFluid = ing;
		return getThis();
	}

	public WitchRecipeBuilder fluid(Fluid fluid) {
		return fluid(FluidIngredient.of(fluid));
	}

	public WitchRecipeBuilder time(int time) {
		recipe.time = time;
		return getThis();
	}

	public WitchRecipeBuilder resultFluid(FluidStack stack) {
		recipe.resultFluid = stack.copy();
		return getThis();
	}

	public void save(RegistrateRecipeProvider pvd) {
		super.save(pvd, pvd.safeId(id));
	}

	public void save(RegistrateRecipeProvider pvd, ResourceLocation id) {
		super.save(pvd, pvd.safeId(id));
	}
}
