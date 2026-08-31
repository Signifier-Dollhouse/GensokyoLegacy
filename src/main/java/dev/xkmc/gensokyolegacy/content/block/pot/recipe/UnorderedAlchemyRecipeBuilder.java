package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class UnorderedAlchemyRecipeBuilder extends AlchemyRecipeBuilder<UnorderedAlchemyRecipe, UnorderedAlchemyRecipeBuilder> {

	public UnorderedAlchemyRecipeBuilder() {
		super(GLRecipes.ALCHEMY_UNORDERED.get());
	}

	public UnorderedAlchemyRecipeBuilder add(Ingredient ing) {
		recipe.input.add(ing);
		return this;
	}

	public UnorderedAlchemyRecipeBuilder add(ItemLike item) {
		recipe.input.add(Ingredient.of(item));
		return this;
	}

	public UnorderedAlchemyRecipeBuilder add(TagKey<Item> tag) {
		recipe.input.add(Ingredient.of(tag));
		return this;
	}
}
