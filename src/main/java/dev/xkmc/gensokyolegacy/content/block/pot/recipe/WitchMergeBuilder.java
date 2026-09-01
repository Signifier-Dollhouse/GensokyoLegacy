package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class WitchMergeBuilder extends AbstractAlchemyRecipeBuilder<WitchMergeRecipe, WitchMergeBuilder> {

	private final ResourceLocation id;

	public WitchMergeBuilder(ResourceLocation id, ItemLike potion, int count) {
		super(GLRecipes.ALCHEMY_WITCH_MERGE.get());
		this.id = id;
		recipe.potionIngredient = Ingredient.of(potion);
		recipe.potionCount = count;
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

	public void save(RegistrateRecipeProvider pvd) {
		super.save(pvd, pvd.safeId(id));
	}

	public void save(RegistrateRecipeProvider pvd, ResourceLocation id) {
		super.save(pvd, pvd.safeId(id));
	}
}
