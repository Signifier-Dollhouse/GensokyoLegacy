package dev.xkmc.gensokyolegacy.init.data;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import dev.xkmc.gensokyolegacy.content.block.pot.recipe.UnorderedAlchemyRecipeBuilder;
import dev.xkmc.gensokyolegacy.content.block.pot.recipe.WitchEnhanceBuilder;
import dev.xkmc.gensokyolegacy.content.block.pot.recipe.WitchMergeBuilder;
import dev.xkmc.gensokyolegacy.content.block.pot.stage.PotionStageBuilder;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLNaturalBlocks;
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
				.add(GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET.cap.get())
				.add(GLNaturalBlocks.DREAM_MUSHROOM_SET.cap.get())
				.time(200)
				.resultFluid(HexBrew.MUNDANE_HEXBREW.getSource(), 1000)
				::unlockedBy, GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET.cap.get().asItem())
				.save(pvd, GensokyoLegacy.loc("mundane_hexbrew"));

		unlock(pvd, new UnorderedAlchemyRecipeBuilder()
				.fluid(HexBrew.MUNDANE_HEXBREW.getSource())
				.add(Items.BLAZE_POWDER)
				.add(Items.GUNPOWDER)
				.time(200)
				.resultFluid(HexBrew.EXPLOSIVE_HEXBREW.getSource(), 1000)
				::unlockedBy, Items.BLAZE_POWDER)
				.save(pvd, GensokyoLegacy.loc("explosive_hexbrew"));

		unlock(pvd, new UnorderedAlchemyRecipeBuilder()
				.fluid(Fluids.WATER)
				.add(GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap.get())
				.add(Items.SPIDER_EYE)
				.add(Items.ROTTEN_FLESH)
				.add(Items.ROTTEN_FLESH)
				.add(Items.ROTTEN_FLESH)
				.add(Items.ROTTEN_FLESH)
				.time(400)
				.resultFluid(HexBrew.MIASMA_HEXBREW.getSource(), 1000)
				::unlockedBy, GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap.get().asItem())
				.save(pvd, GensokyoLegacy.loc("miasma_hexbrew"));

		// potion
		{
			unlock(pvd, new PotionStageBuilder(GensokyoLegacy.loc("potion_stage"))::unlockedBy, Items.POTION).save(pvd);

			unlock(pvd, new WitchMergeBuilder(GensokyoLegacy.loc("witch_hexbrew_1"), Items.POTION, 1)
					.fluid(HexBrew.MUNDANE_HEXBREW.getSource())
					.add(GLNaturalBlocks.FLAME_CATTAIL)
					.add(GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET.cap)
					.time(200)
					.resultFluid(HexBrew.WITCH_HEXBREW.getSource(), 250)
					::unlockedBy, GLNaturalBlocks.FLAME_CATTAIL.asItem())
					.save(pvd);

			unlock(pvd, new WitchMergeBuilder(GensokyoLegacy.loc("witch_splash_1"), Items.SPLASH_POTION, 1)
					.fluid(HexBrew.MUNDANE_HEXBREW.getSource())
					.add(GLNaturalBlocks.FLAME_CATTAIL)
					.add(GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET.cap)
					.add(Items.GUNPOWDER)
					.time(200)
					.resultFluid(HexBrew.WITCH_SPLASH.getSource(), 250)
					::unlockedBy, GLNaturalBlocks.FLAME_CATTAIL.asItem())
					.save(pvd);

			unlock(pvd, new WitchMergeBuilder(GensokyoLegacy.loc("witch_hexbrew_2"), Items.POTION, 2)
					.fluid(HexBrew.MUNDANE_HEXBREW.getSource())
					.add(GLNaturalBlocks.FLAME_CATTAIL)
					.add(GLNaturalBlocks.STAR_FLOWER)
					.time(200)
					.resultFluid(HexBrew.WITCH_HEXBREW.getSource(), 250)
					::unlockedBy, GLNaturalBlocks.STAR_FLOWER.asItem())
					.save(pvd);

			unlock(pvd, new WitchMergeBuilder(GensokyoLegacy.loc("witch_splash_2"), Items.SPLASH_POTION, 2)
					.fluid(HexBrew.MUNDANE_HEXBREW.getSource())
					.add(GLNaturalBlocks.FLAME_CATTAIL)
					.add(GLNaturalBlocks.STAR_FLOWER)
					.add(Items.GUNPOWDER)
					.time(200)
					.resultFluid(HexBrew.WITCH_SPLASH.getSource(), 250)
					::unlockedBy, GLNaturalBlocks.FLAME_CATTAIL.asItem())
					.save(pvd);

			unlock(pvd, new WitchMergeBuilder(GensokyoLegacy.loc("witch_hexbrew_3"), Items.POTION, 3)
					.fluid(HexBrew.MUNDANE_HEXBREW.getSource())
					.add(GLNaturalBlocks.FLAME_CATTAIL)
					.add(Items.DRAGON_BREATH)
					.time(200)
					.resultFluid(HexBrew.WITCH_HEXBREW.getSource(), 250)
					::unlockedBy, Items.DRAGON_BREATH)
					.save(pvd);

			unlock(pvd, new WitchMergeBuilder(GensokyoLegacy.loc("witch_splash_3"), Items.SPLASH_POTION, 3)
					.fluid(HexBrew.MUNDANE_HEXBREW.getSource())
					.add(GLNaturalBlocks.FLAME_CATTAIL)
					.add(Items.DRAGON_BREATH)
					.add(Items.GUNPOWDER)
					.time(200)
					.resultFluid(HexBrew.WITCH_SPLASH.getSource(), 250)
					::unlockedBy, GLNaturalBlocks.FLAME_CATTAIL.asItem())
					.save(pvd);

			unlock(pvd, new WitchEnhanceBuilder(GensokyoLegacy.loc("witch_hexbrew_enhance"))
					.fluid(HexBrew.WITCH_HEXBREW.getSource())
					.add(GLNaturalBlocks.FLAME_CATTAIL)
					.add(Items.BLAZE_POWDER)
					.add(Items.REDSTONE)
					.time(200)
					.resultFluid(HexBrew.WITCH_HEXBREW.getSource(), 250)
					::unlockedBy, Items.BLAZE_POWDER)
					.save(pvd);

			unlock(pvd, new WitchEnhanceBuilder(GensokyoLegacy.loc("witch_splash_enhance"))
					.fluid(HexBrew.WITCH_SPLASH.getSource())
					.add(GLNaturalBlocks.FLAME_CATTAIL)
					.add(Items.BLAZE_POWDER)
					.add(Items.REDSTONE)
					.time(200)
					.resultFluid(HexBrew.WITCH_SPLASH.getSource(), 250)
					::unlockedBy, Items.BLAZE_POWDER)
					.save(pvd);

			unlock(pvd, new UnorderedAlchemyRecipeBuilder()
					.fluid(HexBrew.WITCH_HEXBREW.getSource())
					.add(Items.GUNPOWDER)
					.time(200)
					.resultFluid(HexBrew.WITCH_SPLASH.getSource(), 1000)
					::unlockedBy, Items.GUNPOWDER)
					.save(pvd, GensokyoLegacy.loc("witch_splash_from_hexbrew"));
		}

	}

	public static <T> T unlock(RegistrateRecipeProvider pvd, BiFunction<String, Criterion<InventoryChangeTrigger.TriggerInstance>, T> func, Item item) {
		return func.apply("has_" + pvd.safeName(item), DataIngredient.items(item).getCriterion(pvd));
	}

}
