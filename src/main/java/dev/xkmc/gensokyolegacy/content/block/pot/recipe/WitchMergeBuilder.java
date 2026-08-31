package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2core.serial.recipe.BaseRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public class WitchMergeBuilder extends BaseRecipeBuilder<WitchMergeBuilder, WitchMergeRecipe, AlchemyRecipe<?>, AlchemyInv> {

	private final ResourceLocation id;

	public WitchMergeBuilder(ResourceLocation id) {
		super(GLRecipes.ALCHEMY_WITCH_MERGE.get(), Items.AIR);
		this.id = id;
		recipe.potionCount = 0;
		recipe.inputFluid = FluidIngredient.of((Fluid) HexBrew.MUNDANE.fluid.getSource());
		recipe.resultFluid = new FluidStack((Fluid) HexBrew.WITCH.fluid.getSource(), 250);
	}

	public WitchMergeBuilder potionCount(int count) {
		recipe.potionCount = count;
		return getThis();
	}

	public WitchMergeBuilder add(Ingredient ing) {
		recipe.extra.add(ing);
		return this;
	}

	public WitchMergeBuilder add(ItemLike item) {
		recipe.extra.add(Ingredient.of(item));
		return this;
	}

	public WitchMergeBuilder add(TagKey<Item> tag) {
		recipe.extra.add(Ingredient.of(tag));
		return this;
	}

	public WitchMergeBuilder fluid(FluidIngredient ing) {
		recipe.inputFluid = ing;
		return getThis();
	}

	public WitchMergeBuilder fluid(Fluid fluid) {
		return fluid(FluidIngredient.of(fluid));
	}

	public WitchMergeBuilder time(int time) {
		recipe.time = time;
		return getThis();
	}

	public WitchMergeBuilder resultFluid(FluidStack stack) {
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
