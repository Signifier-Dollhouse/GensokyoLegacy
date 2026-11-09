package dev.xkmc.gensokyolegacy.init.registrate;

import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyInv;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.AlchemyRecipe;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.UnorderedAlchemyRecipe;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.WitchEnhanceRecipe;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.WitchMergeRecipe;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.stage.AlchemyStageRecipe;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.stage.PotionStageRecipe;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.stage.SimpleAlchemyStageRecipe;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2library.serial.recipe.BaseRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class GLRecipes {

	public static final RegistryEntry<RecipeType<AlchemyRecipe<?>>> ALCHEMY_RT =
			GensokyoLegacy.REGISTRATE.<RecipeType<?>, RecipeType<AlchemyRecipe<?>>>simple(
					"alchemy", Registries.RECIPE_TYPE,
					() -> RecipeType.simple(GensokyoLegacy.loc("alchemy")));
	public static final RegistryEntry<BaseRecipe.RecType<UnorderedAlchemyRecipe, AlchemyRecipe<?>, AlchemyInv>> ALCHEMY_UNORDERED =
			GensokyoLegacy.REGISTRATE.<RecipeSerializer<?>, BaseRecipe.RecType<UnorderedAlchemyRecipe, AlchemyRecipe<?>, AlchemyInv>>simple(
					"unordered_alchemy", Registries.RECIPE_SERIALIZER,
					() -> new BaseRecipe.RecType<>(UnorderedAlchemyRecipe.class, ALCHEMY_RT));

	public static final RegistryEntry<RecipeType<AlchemyStageRecipe<?>>> ALCHEMY_STAGE_RT =
			GensokyoLegacy.REGISTRATE.<RecipeType<?>, RecipeType<AlchemyStageRecipe<?>>>simple(
					"alchemy_stage", Registries.RECIPE_TYPE,
					() -> RecipeType.simple(GensokyoLegacy.loc("alchemy_stage")));
	public static final RegistryEntry<BaseRecipe.RecType<SimpleAlchemyStageRecipe, AlchemyStageRecipe<?>, AlchemyInv>> ALCHEMY_STAGE_SIMPLE =
			GensokyoLegacy.REGISTRATE.<RecipeSerializer<?>, BaseRecipe.RecType<SimpleAlchemyStageRecipe, AlchemyStageRecipe<?>, AlchemyInv>>simple(
					"simple_alchemy_stage", Registries.RECIPE_SERIALIZER,
					() -> new BaseRecipe.RecType<>(SimpleAlchemyStageRecipe.class, ALCHEMY_STAGE_RT));
	public static final RegistryEntry<BaseRecipe.RecType<PotionStageRecipe, AlchemyStageRecipe<?>, AlchemyInv>> ALCHEMY_STAGE_POTION =
			GensokyoLegacy.REGISTRATE.<RecipeSerializer<?>, BaseRecipe.RecType<PotionStageRecipe, AlchemyStageRecipe<?>, AlchemyInv>>simple(
					"potion_alchemy_stage", Registries.RECIPE_SERIALIZER,
					() -> new BaseRecipe.RecType<>(PotionStageRecipe.class, ALCHEMY_STAGE_RT));

	public static final RegistryEntry<BaseRecipe.RecType<WitchMergeRecipe, AlchemyRecipe<?>, AlchemyInv>> ALCHEMY_WITCH_MERGE =
			GensokyoLegacy.REGISTRATE.<RecipeSerializer<?>, BaseRecipe.RecType<WitchMergeRecipe, AlchemyRecipe<?>, AlchemyInv>>simple(
					"witch_merge", Registries.RECIPE_SERIALIZER,
					() -> new BaseRecipe.RecType<>(WitchMergeRecipe.class, ALCHEMY_RT));
	public static final RegistryEntry<BaseRecipe.RecType<WitchEnhanceRecipe, AlchemyRecipe<?>, AlchemyInv>> ALCHEMY_WITCH_ENHANCE =
			GensokyoLegacy.REGISTRATE.<RecipeSerializer<?>, BaseRecipe.RecType<WitchEnhanceRecipe, AlchemyRecipe<?>, AlchemyInv>>simple(
					"witch_enhance", Registries.RECIPE_SERIALIZER,
					() -> new BaseRecipe.RecType<>(WitchEnhanceRecipe.class, ALCHEMY_RT));

	public static void register() {

	}

}
