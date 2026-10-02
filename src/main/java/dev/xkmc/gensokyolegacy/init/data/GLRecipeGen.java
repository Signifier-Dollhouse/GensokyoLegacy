package dev.xkmc.gensokyolegacy.init.data;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.UnorderedAlchemyRecipeBuilder;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.WitchEnhanceBuilder;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.WitchMergeBuilder;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.stage.PotionStageBuilder;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLDecoBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLFurniture;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class GLRecipeGen {

	public static void genRecipe(RegistrateRecipeProvider pvd) {
		furniture(pvd);
		wroughtIron(pvd);
		hexbrew(pvd);

		unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GLBlocks.ALCHEMY_POT.get(), 6)::unlockedBy, Items.IRON_INGOT)
				.pattern("I I").pattern("IFI").pattern("III")
				.define('I', Items.IRON_INGOT).define('F', GLNaturalBlocks.STAR_FLOWER).save(pvd);
	}

	public static void hexbrew(RegistrateRecipeProvider pvd) {

		// hexbrew
		{
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
		}

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

	/**
	 * Invented survival recipes for decorative / furniture blocks.
	 * Per-wood pieces are cut on the stonecutter from their own planks, so the
	 * output matches the input wood and crafting-grid collisions with other
	 * mods are impossible. Fuel values follow ingredient burn time: a plank
	 * (300) cutting into N pieces splits the value (walls 1 plank -> 2 = 150).
	 */
	private static void furniture(RegistrateRecipeProvider pvd) {
		for (var e : GLDecoBlocks.WoodType.values()) {
			var plank = DataIngredient.items(e.plank);
			pvd.stonecutting(plank, RecipeCategory.DECORATIONS, e.stool);
			pvd.stonecutting(plank, RecipeCategory.DECORATIONS, e.largeTable);
			pvd.stonecutting(plank, RecipeCategory.DECORATIONS, e.largeChair);
			pvd.stonecutting(plank, RecipeCategory.BUILDING_BLOCKS, e.wall, 2);
			pvd.stonecutting(plank, RecipeCategory.BUILDING_BLOCKS, e.door);
		}

		// scarlet devil mansion chair: oak stool draped with red wool
		unlock(pvd, ShapelessRecipeBuilder.shapeless(
				RecipeCategory.DECORATIONS, GLDecoBlocks.SCARLET_CHAIR.get())::unlockedBy, Items.RED_WOOL)
				.requires(GLDecoBlocks.WoodType.OAK.stool.get()).requires(Items.RED_WOOL).save(pvd);

		// cushion: wool stuffed with string (3 wool at 100 + string = 300 fuel into 6 = 50 each)
		unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GLDecoBlocks.CUSHION.get(), 6)::unlockedBy, Items.STRING)
				.pattern("S S").pattern("WWW")
				.define('W', Blocks.WHITE_WOOL).define('S', Items.STRING).save(pvd);

		// tatami block: bound straw (hay and string are not fuel, so no burn time).
		// one block cuts into 8 thin mats, or 3 blocks craft into 16
		unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, GLDecoBlocks.TATAMI_BLOCK.get(), 4)::unlockedBy, Items.STRING)
				.pattern("SHS").pattern("HHH").pattern("SHS")
				.define('H', Blocks.HAY_BLOCK).define('S', Items.STRING).save(pvd);
		pvd.stonecutting(DataIngredient.items(GLDecoBlocks.TATAMI_BLOCK.get()),
				RecipeCategory.BUILDING_BLOCKS, GLDecoBlocks.TATAMI, 8);
		unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, GLDecoBlocks.TATAMI.get(), 16)::unlockedBy,
				GLDecoBlocks.TATAMI_BLOCK.get().asItem())
				.pattern("TTT")
				.define('T', GLDecoBlocks.TATAMI_BLOCK.get()).save(pvd);

		// paper windows: paper panes in a stick frame, shoji uses a full stick lattice.
		// 4 sticks at 100 burn time into 4 windows = 100 each, cut panes quarter it
		unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, GLDecoBlocks.GLASS.get(), 4)::unlockedBy, Items.PAPER)
				.pattern("SPS").pattern("PPP").pattern("SPS")
				.define('S', Items.STICK).define('P', Items.PAPER).save(pvd);
		unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, GLDecoBlocks.SHOJI_FRAME.get(), 4)::unlockedBy, Items.PAPER)
				.pattern("SSS").pattern("SPS").pattern("SSS")
				.define('S', Items.STICK).define('P', Items.PAPER).save(pvd);
		unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, GLDecoBlocks.GLASS_PANE.get(), 16)::unlockedBy, GLDecoBlocks.GLASS.get().asItem())
				.pattern("PPP").pattern("PPP")
				.define('P', GLDecoBlocks.GLASS.get()).save(pvd);
		unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, GLDecoBlocks.SHOJI_FRAME_PANE.get(), 16)::unlockedBy, GLDecoBlocks.SHOJI_FRAME.get().asItem())
				.pattern("PPP").pattern("PPP")
				.define('P', GLDecoBlocks.SHOJI_FRAME.get()).save(pvd);
		pvd.stonecutting(DataIngredient.items(GLDecoBlocks.GLASS.get()),
				RecipeCategory.BUILDING_BLOCKS, GLDecoBlocks.GLASS_PANE, 4);
		pvd.stonecutting(DataIngredient.items(GLDecoBlocks.SHOJI_FRAME.get()),
				RecipeCategory.BUILDING_BLOCKS, GLDecoBlocks.SHOJI_FRAME_PANE, 4);

		// paper sliding door: shoji lattice door, paper in a stick grid like shoji frame
		unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, GLDecoBlocks.PAPER_SLIDING_DOOR.get())::unlockedBy, Items.PAPER)
				.pattern("SPS").pattern("PSP").pattern("SPS")
				.define('S', Items.STICK).define('P', Items.PAPER).save(pvd);

		// case goods are cut from dark oak logs on the stonecutter (1 log at 300
		// burn time into 1 piece, so fuel stays 300)
		var darkOakLog = DataIngredient.items(Blocks.DARK_OAK_LOG);
		// drawer cabinet: chest core with stick runners
		pvd.stonecutting(darkOakLog, RecipeCategory.DECORATIONS, GLFurniture.DRAWER_CABINET);
		// door cabinet: full plank carcass
		pvd.stonecutting(darkOakLog, RecipeCategory.DECORATIONS, GLFurniture.DOOR_CABINET);
		// birch shelf
		pvd.stonecutting(darkOakLog, RecipeCategory.DECORATIONS, GLFurniture.SHELF);
		// tea table with glass top
		pvd.stonecutting(darkOakLog, RecipeCategory.DECORATIONS, GLFurniture.TEA_TABLE);
		// lending shelf for player books
		pvd.stonecutting(darkOakLog, RecipeCategory.DECORATIONS, GLFurniture.BOOK_SHELF);
		// crate: slatted box cut from dark oak planks
		pvd.stonecutting(DataIngredient.items(Blocks.DARK_OAK_PLANKS),
				RecipeCategory.DECORATIONS, GLFurniture.CRATE);
		// crate: stick-framed plank box
		unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GLFurniture.CRATE.get())::unlockedBy, Items.STICK)
				.pattern("PSP").pattern("S S").pattern("PSP")
				.define('P', ItemTags.PLANKS).define('S', Items.STICK).save(pvd);
		// base carton: folded paper (dyed cartons are shapeless dyeing in GLFurniture)
		unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GLFurniture.CARTON.get(), 2)::unlockedBy, Items.PAPER)
				.pattern("PPP").pattern("PPP")
				.define('P', Items.PAPER).save(pvd);
	}

	/**
	 * Wrought iron: iron bars welded into a ring and blackened with a single
	 * black dye, then two bars stacked into a pillar. Neither step loses bars,
	 * so the cost of the metal is the same as vanilla iron bars throughout.
	 */
	private static void wroughtIron(RegistrateRecipeProvider pvd) {
		// 8 bars around the dye, 8 bars out
		unlock(pvd, ShapedRecipeBuilder.shaped(
				RecipeCategory.DECORATIONS, GLDecoBlocks.WROUGHT_IRON_BARS.get(), 8)::unlockedBy, Items.IRON_BARS)
				.pattern("I I").pattern("IBI").pattern("I I")
				.define('I', Items.IRON_BARS).define('B', Items.BLACK_DYE).save(pvd);
		// two bars stacked, like a vanilla pillar
		unlock(pvd, ShapedRecipeBuilder.shaped(
				RecipeCategory.DECORATIONS, GLDecoBlocks.WROUGHT_IRON_PILLAR.get())::unlockedBy,
				GLDecoBlocks.WROUGHT_IRON_BARS.get().asItem())
				.pattern("W").pattern("W")
				.define('W', GLDecoBlocks.WROUGHT_IRON_BARS.get()).save(pvd);
	}

	/**
	 * Noren: a curtain is its own colour of wool cut on the stonecutter, so the
	 * whole family has a vanilla entry point. Dyeing between the four patterns
	 * is added separately, next to each block's registration.
	 */
	public static void cutWool(RegistrateRecipeProvider pvd, DyeColor color, Supplier<? extends ItemLike> result) {
		pvd.stonecutting(DataIngredient.items(wool(color)), RecipeCategory.DECORATIONS, result);
	}

	/** {@link DyeColor} carries no reference to its wool block, so spell them out. */
	private static Block wool(DyeColor color) {
		return switch (color) {
			case WHITE -> Blocks.WHITE_WOOL;
			case ORANGE -> Blocks.ORANGE_WOOL;
			case MAGENTA -> Blocks.MAGENTA_WOOL;
			case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL;
			case YELLOW -> Blocks.YELLOW_WOOL;
			case LIME -> Blocks.LIME_WOOL;
			case PINK -> Blocks.PINK_WOOL;
			case GRAY -> Blocks.GRAY_WOOL;
			case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL;
			case CYAN -> Blocks.CYAN_WOOL;
			case PURPLE -> Blocks.PURPLE_WOOL;
			case BLUE -> Blocks.BLUE_WOOL;
			case BROWN -> Blocks.BROWN_WOOL;
			case GREEN -> Blocks.GREEN_WOOL;
			case RED -> Blocks.RED_WOOL;
			case BLACK -> Blocks.BLACK_WOOL;
		};
	}

}
