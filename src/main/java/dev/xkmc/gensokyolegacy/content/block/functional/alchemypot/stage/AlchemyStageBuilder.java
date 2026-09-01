package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.stage;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyInv;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2core.serial.recipe.BaseRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public class AlchemyStageBuilder extends BaseRecipeBuilder<AlchemyStageBuilder, SimpleAlchemyStageRecipe, AlchemyStageRecipe<?>, AlchemyInv> {

	private final ResourceLocation id;

	public AlchemyStageBuilder(ResourceLocation id) {
		super(GLRecipes.ALCHEMY_STAGE_SIMPLE.get(), Items.AIR);
		this.id = id;
	}

	public AlchemyStageBuilder fluid(FluidIngredient ing) {
		recipe.inputFluid = ing;
		return getThis();
	}

	public AlchemyStageBuilder fluid(Fluid fluid) {
		return fluid(FluidIngredient.of(fluid));
	}

	public AlchemyStageBuilder fluid(TagKey<Fluid> tag) {
		return fluid(FluidIngredient.tag(tag));
	}

	public AlchemyStageBuilder color(int color) {
		recipe.color = color;
		return getThis();
	}

	public AlchemyStageBuilder add(Ingredient ing) {
		recipe.input.add(ing);
		return this;
	}

	public AlchemyStageBuilder add(ItemLike item) {
		recipe.input.add(Ingredient.of(item));
		return this;
	}

	public AlchemyStageBuilder add(TagKey<Item> tag) {
		recipe.input.add(Ingredient.of(tag));
		return this;
	}

	public void save(RegistrateRecipeProvider pvd) {
		super.save(pvd, pvd.safeId(id));
	}

	public void save(RegistrateRecipeProvider pvd, ResourceLocation id) {
		super.save(pvd, pvd.safeId(id));
	}
}
