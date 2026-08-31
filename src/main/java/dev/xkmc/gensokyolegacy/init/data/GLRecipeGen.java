package dev.xkmc.gensokyolegacy.init.data;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import dev.xkmc.gensokyolegacy.content.block.pot.recipe.UnorderedAlchemyRecipeBuilder;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLDecoBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.GLFluids;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.BiFunction;

public class GLRecipeGen {

	public static void genRecipe(RegistrateRecipeProvider pvd) {
		// alchemy pot: 1 bucket (4 bottles) per recipe
		unlock(pvd, new UnorderedAlchemyRecipeBuilder()
				.fluid(Fluids.WATER)
				.add(GLDecoBlocks.GHOST_FIRE_MUSHROOM_SET.cap.get())
				.add(GLDecoBlocks.DREAM_MUSHROOM_SET.cap.get())
				.time(200)
				.resultFluid(new FluidStack((Fluid) GLFluids.Hexbrew.MUNDANE.fluid.getSource(), 1000))
				::unlockedBy, GLDecoBlocks.GHOST_FIRE_MUSHROOM_SET.cap.get().asItem())
				.save(pvd, GensokyoLegacy.loc("alchemy/mundane_hexbrew"));
		unlock(pvd, new UnorderedAlchemyRecipeBuilder()
				.fluid(GLFluids.Hexbrew.MUNDANE.fluid.getSource())
				.add(Items.BLAZE_POWDER)
				.add(Items.GUNPOWDER)
				.time(200)
				.resultFluid(new FluidStack((Fluid) GLFluids.Hexbrew.EXPLOSIVE.fluid.getSource(), 1000))
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
				.resultFluid(new FluidStack((Fluid) GLFluids.Hexbrew.MIASMA.fluid.getSource(), 1000))
				::unlockedBy, GLDecoBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap.get().asItem())
				.save(pvd, GensokyoLegacy.loc("alchemy/miasma_hexbrew"));
	}

	public static <T> T unlock(RegistrateRecipeProvider pvd, BiFunction<String, Criterion<InventoryChangeTrigger.TriggerInstance>, T> func, Item item) {
		return func.apply("has_" + pvd.safeName(item), DataIngredient.items(item).getCriterion(pvd));
	}

}
