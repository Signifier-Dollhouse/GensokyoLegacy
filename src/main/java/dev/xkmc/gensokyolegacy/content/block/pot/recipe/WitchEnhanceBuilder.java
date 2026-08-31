package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2core.serial.recipe.BaseRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public class WitchEnhanceBuilder extends BaseRecipeBuilder<WitchEnhanceBuilder, WitchEnhanceRecipe, AlchemyRecipe<?>, AlchemyInv> {

	private final ResourceLocation id;

	public WitchEnhanceBuilder(ResourceLocation id) {
		super(GLRecipes.ALCHEMY_WITCH_ENHANCE.get(), Items.AIR);
		this.id = id;
		recipe.inputFluid = FluidIngredient.of((Fluid) HexBrew.WITCH.fluid.getSource());
		recipe.resultFluid = new FluidStack((Fluid) HexBrew.WITCH.fluid.getSource(), 250);
	}

	public WitchEnhanceBuilder add(Ingredient ing) {
		recipe.extra.add(ing);
		return this;
	}

	public WitchEnhanceBuilder add(ItemLike item) {
		recipe.extra.add(Ingredient.of(item));
		return this;
	}

	public WitchEnhanceBuilder add(TagKey<Item> tag) {
		recipe.extra.add(Ingredient.of(tag));
		return this;
	}

	public WitchEnhanceBuilder fluid(FluidIngredient ing) {
		recipe.inputFluid = ing;
		return getThis();
	}

	public WitchEnhanceBuilder fluid(Fluid fluid) {
		return fluid(FluidIngredient.of(fluid));
	}

	public WitchEnhanceBuilder time(int time) {
		recipe.time = time;
		return getThis();
	}

	public WitchEnhanceBuilder resultFluid(FluidStack stack) {
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
