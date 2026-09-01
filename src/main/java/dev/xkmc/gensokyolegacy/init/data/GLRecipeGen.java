package dev.xkmc.gensokyolegacy.init.data;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import dev.xkmc.gensokyolegacy.content.block.pot.recipe.UnorderedAlchemyRecipeBuilder;
import dev.xkmc.gensokyolegacy.content.block.pot.recipe.WitchEnhanceBuilder;
import dev.xkmc.gensokyolegacy.content.block.pot.recipe.WitchMergeBuilder;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLDecoBlocks;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import java.util.function.BiFunction;

public class GLRecipeGen {

	public static void genRecipe(RegistrateRecipeProvider pvd) {

		unlock(pvd, new UnorderedAlchemyRecipeBuilder()
				.fluid(Fluids.WATER)
				.add(GLDecoBlocks.GHOST_FIRE_MUSHROOM_SET.cap.get())
				.add(GLDecoBlocks.DREAM_MUSHROOM_SET.cap.get())
				.time(200)
				.resultFluid(HexBrew.MUNDANE.getSource(), 1000)
				::unlockedBy, GLDecoBlocks.GHOST_FIRE_MUSHROOM_SET.cap.get().asItem())
				.save(pvd, GensokyoLegacy.loc("alchemy/mundane_hexbrew"));

		unlock(pvd, new UnorderedAlchemyRecipeBuilder()
				.fluid(HexBrew.MUNDANE.getSource())
				.add(Items.BLAZE_POWDER)
				.add(Items.GUNPOWDER)
				.time(200)
				.resultFluid(HexBrew.EXPLOSIVE.getSource(), 1000)
				::unlockedBy, Items.BLAZE_POWDER)
				.save(pvd, GensokyoLegacy.loc("alchemy/explosive_hexbrew"));

		unlock(pvd, new UnorderedAlchemyRecipeBuilder()
				.fluid(Fluids.WATER)
				.add(GLDecoBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap.get())
				.add(Items.SPIDER_EYE)
				.add(Items.ROTTEN_FLESH)
				.add(Items.ROTTEN_FLESH)
				.add(Items.ROTTEN_FLESH)
				.add(Items.ROTTEN_FLESH)
				.time(400)
				.resultFluid(HexBrew.MIASMA.getSource(), 1000)
				::unlockedBy, GLDecoBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap.get().asItem())
				.save(pvd, GensokyoLegacy.loc("alchemy/miasma_hexbrew"));

		unlock(pvd, new WitchMergeBuilder(GensokyoLegacy.loc("alchemy/witch_hexbrew_1"))
				.fluid(HexBrew.MUNDANE.getSource())
				.add(GLDecoBlocks.FLAME_CATTAIL)
				.potionCount(1)
				.time(200)
				.resultFluid(HexBrew.WITCH.getSource(), 250)
				::unlockedBy, GLDecoBlocks.FLAME_CATTAIL.asItem())
				.save(pvd);

		unlock(pvd, new WitchMergeBuilder(GensokyoLegacy.loc("alchemy/witch_hexbrew_2"))
				.fluid(HexBrew.MUNDANE.getSource())
				.add(GLDecoBlocks.FLAME_CATTAIL)
				.add(GLDecoBlocks.STAR_FLOWER)
				.potionCount(2)
				.time(200)
				.resultFluid(HexBrew.WITCH.getSource(), 250)
				::unlockedBy, GLDecoBlocks.STAR_FLOWER.asItem())
				.save(pvd);

		unlock(pvd, new WitchMergeBuilder(GensokyoLegacy.loc("alchemy/witch_hexbrew_3"))
				.fluid(HexBrew.MUNDANE.getSource())
				.add(GLDecoBlocks.FLAME_CATTAIL)
				.add(Items.DRAGON_BREATH)
				.potionCount(3)
				.time(200)
				.resultFluid(HexBrew.WITCH.getSource(), 250)
				::unlockedBy, Items.DRAGON_BREATH)
				.save(pvd);

		unlock(pvd, new WitchEnhanceBuilder(GensokyoLegacy.loc("alchemy/witch_hexbrew_enhance"))
				.fluid(HexBrew.WITCH.getSource())
				.add(GLDecoBlocks.FLAME_CATTAIL)
				.add(Items.BLAZE_POWDER)
				.add(Items.REDSTONE)
				.time(200)
				.resultFluid(HexBrew.WITCH.getSource(), 250)
				::unlockedBy, Items.BLAZE_POWDER)
				.save(pvd);
	}

	public static <T> T unlock(RegistrateRecipeProvider pvd, BiFunction<String, Criterion<InventoryChangeTrigger.TriggerInstance>, T> func, Item item) {
		return func.apply("has_" + pvd.safeName(item), DataIngredient.items(item).getCriterion(pvd));
	}

}
