package dev.xkmc.gensokyolegacy.init.registrate;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.gensokyolegacy.content.block.pot.recipe.AlchemyRecipe;
import dev.xkmc.gensokyolegacy.content.block.pot.recipe.UnorderedAlchemyRecipe;
import dev.xkmc.gensokyolegacy.content.block.pot.recipe.WitchRecipe;
import dev.xkmc.gensokyolegacy.content.block.pot.stage.AlchemyStageRecipe;
import dev.xkmc.gensokyolegacy.content.block.pot.stage.PotionStageRecipe;
import dev.xkmc.gensokyolegacy.content.block.pot.stage.SimpleAlchemyStageRecipe;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2core.init.reg.simple.SR;
import dev.xkmc.l2core.init.reg.simple.Val;
import dev.xkmc.l2core.serial.recipe.BaseRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class GLRecipes {

	private static final SR<RecipeType<?>> RT = SR.of(GensokyoLegacy.REG, Registries.RECIPE_TYPE);

	private static final SR<RecipeSerializer<?>> RS = SR.of(GensokyoLegacy.REG, Registries.RECIPE_SERIALIZER);

	public static final Val<RecipeType<AlchemyRecipe<?>>> ALCHEMY_RT =
			RT.reg("alchemy", RecipeType::simple);
	public static final Val<BaseRecipe.RecType<UnorderedAlchemyRecipe, AlchemyRecipe<?>, AlchemyInv>> ALCHEMY_UNORDERED =
			RS.reg("unordered_alchemy", () -> new BaseRecipe.RecType<>(UnorderedAlchemyRecipe.class, ALCHEMY_RT));

	public static final Val<RecipeType<AlchemyStageRecipe<?>>> ALCHEMY_STAGE_RT =
			RT.reg("alchemy_stage", RecipeType::simple);
	public static final Val<BaseRecipe.RecType<SimpleAlchemyStageRecipe, AlchemyStageRecipe<?>, AlchemyInv>> ALCHEMY_STAGE_SIMPLE =
			RS.reg("simple_alchemy_stage", () -> new BaseRecipe.RecType<>(SimpleAlchemyStageRecipe.class, ALCHEMY_STAGE_RT));
	public static final Val<BaseRecipe.RecType<PotionStageRecipe, AlchemyStageRecipe<?>, AlchemyInv>> ALCHEMY_STAGE_POTION =
			RS.reg("potion_alchemy_stage", () -> new BaseRecipe.RecType<>(PotionStageRecipe.class, ALCHEMY_STAGE_RT));

	public static final Val<BaseRecipe.RecType<WitchRecipe, AlchemyRecipe<?>, AlchemyInv>> ALCHEMY_WITCH =
			RS.reg("witch_hexbrew", () -> new BaseRecipe.RecType<>(WitchRecipe.class, ALCHEMY_RT));

	public static void register() {

	}

}
