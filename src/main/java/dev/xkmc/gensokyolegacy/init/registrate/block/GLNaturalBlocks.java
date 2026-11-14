package dev.xkmc.gensokyolegacy.init.registrate.block;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.xkmc.gensokyolegacy.content.block.nature.*;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftItemData;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftType;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.MushroomFeatures;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLRecipeGen;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.gensokyolegacy.init.registrate.GLParticles;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import dev.xkmc.l2core.init.reg.registrate.SimpleEntry;
import net.minecraft.advancements.critereon.*;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class GLNaturalBlocks {

	public static final SimpleEntry<CreativeModeTab> TAB;

	public static final BlockEntry<SimpleBushBlock> STAR_FLOWER, RED_SPIDER_LILY;
	public static final BlockEntry<GoldenLotusBlock> GOLDEN_LOTUS;
	public static final BlockEntry<SimpleBushBlock> TOWER_GRASS_SMALL, TOWER_GRASS_LARGE;
	public static final BlockEntry<SimpleBushBlock> SPOOKY_BUSH;
	public static final BlockEntry<BirdNestBlock> BIRD_NEST;
	public static final BlockEntry<WaterloggedBushBlock> FLAME_CATTAIL;
	public static final BlockEntry<SimpleBushBlock> BRACKEN;
	public static final BlockEntry<SideBushBlock> EUGUNE_RED, EUGUNE_BROWN, EUGUNE_GHOST_FIRE;
	public static final BlockEntry<EvergreenVineHeadBlock> EVERGREEN_VINE;
	public static final BlockEntry<EvergreenVineBodyBlock> EVERGREEN_VINE_PLANT;
	public static final BlockEntry<FallenLeavesBlock> CEDAR_FALLEN_LEAVES, ORANGE_MAPLE_FALLEN_LEAVES,
			RED_MAPLE_FALLEN_LEAVES, YELLOW_MAPLE_FALLEN_LEAVES, MIXED_MAPLE_FALLEN_LEAVES,
			PINK_CHERRY_FALLEN_LEAVES, PURPLE_CHERRY_FALLEN_LEAVES, LOCUST_FALLEN_LEAVES,
			MIXED_FALLEN_LEAVES;

	// one wood per species, shared by every leaf colour grown on it. The cherries need none: they
	// grow on minecraft:cherry_log
	public static final WoodSet MAPLE_WOOD, LOCUST_WOOD;
	public static final BlockEntry<Block> LOCUST_PLANKS;
	public static final BlockEntry<RotatedPillarBlock> STRIPPED_LOCUST_LOG, STRIPPED_LOCUST_WOOD;
	public static final BlockEntry<WallBlock> LOCUST_WALL;

	public static final TreeSet BLUE_FUR_SET, ORANGE_MAPLE_SET, RED_MAPLE_SET, YELLOW_MAPLE_SET,
			PINK_CHERRY_SET, PURPLE_CHERRY_SET, LOCUST_SET;

	public static final BlockEntry<HugeMushroomBlock> CYAN_MUSHROOM_STEM, PURPLE_MUSHROOM_STEM, RED_MUSHROOM_STEM;

	public static final BlockEntry<LargeWaterLilyBlock> LARGE_WATER_LILY;
	public static final BlockEntry<WaterLilyBlock> SMALL_WATER_LILY;

	public static final MushroomSet GHOST_FIRE_MUSHROOM_SET, DREAM_MUSHROOM_SET, DEMONIC_MIASMA_MUSHROOM_SET;

	public static final BlockEntry<TallGrassBlock> BROOM_GRASS;

	public static final BlockEntry<HyphaeBlock> HYPHAE;

	public static final BlockEntry<MiasmaAirBlock> MIASMA_AIR;

	static {
		var reg = GensokyoLegacy.REGISTRATE;

		TAB = reg.buildModCreativeTab("natural_blocks", "Gensokyo Legacy - Building Blocks",
				e -> e.icon(() -> GLNaturalBlocks.STAR_FLOWER.asItem().getDefaultInstance()));

		// 星星花
		STAR_FLOWER = reg.block("star_flower", p -> new SimpleBushBlock(p, SimpleBushBlock.FULL))
				.properties(p -> p.offsetType(BlockBehaviour.OffsetType.XYZ).mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> {
					var modelA = pvd.models().cross(ctx.getName(),
							pvd.modLoc("block/nature/star_flower")).renderType("cutout");
					var modelB = pvd.models().cross(ctx.getName() + "_2",
							pvd.modLoc("block/nature/star_flower_2")).renderType("cutout");
					var modelC = pvd.models().cross(ctx.getName() + "_3",
							pvd.modLoc("block/nature/star_flower_3")).renderType("cutout");
					pvd.getVariantBuilder(ctx.get()).partialState().setModels(
							new ConfiguredModel(modelA), new ConfiguredModel(modelB), new ConfiguredModel(modelC));
				})
				.item()
				.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/gift/" + ctx.getName())))
				.dataMap(GLMeta.GIFT_DATA.reg(), new GiftItemData(1, 1000, GiftType.MAGIC))
				.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.65f))
				.tag(ItemTags.SMALL_FLOWERS)
				.build()
				.tag(BlockTags.SMALL_FLOWERS)
				.register();

		RED_SPIDER_LILY = reg.block("red_spider_lily", p -> new SimpleBushBlock(p, Block.box(3, 0, 3, 13, 14, 13)))
				.initialProperties(() -> Blocks.POPPY)
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models().cross(ctx.getName(),
						pvd.modLoc("block/nature/" + ctx.getName())).renderType("cutout")))
				.tag(BlockTags.SMALL_FLOWERS)
				.item().model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("block/nature/" + ctx.getName())))
				.tag(ItemTags.SMALL_FLOWERS)
				.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.65f))
				.build()
				.register();

		// 地涌金莲: random-tick growth through three sizes, the last one a double plant
		GOLDEN_LOTUS = reg.block("golden_lotus", GoldenLotusBlock::new)
				.initialProperties(() -> Blocks.POPPY)
				.properties(p -> p.randomTicks())
				.blockstate(GoldenLotusBlock::genGoldenLotusState)
				.loot(GoldenLotusBlock::genGoldenLotusLoot)
				.tag(BlockTags.SMALL_FLOWERS, BlockTags.TALL_FLOWERS)
				.item().model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/" + ctx.getName())))
				.tag(ItemTags.SMALL_FLOWERS, ItemTags.TALL_FLOWERS)
				.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.65f))
				.build()
				.register();
		TOWER_GRASS_SMALL = GLBlockDatagenHelper.regTowerGrass(reg, "small");
		TOWER_GRASS_LARGE = GLBlockDatagenHelper.regTowerGrass(reg, "large");

		// 阴森灌木: a low bush of the waste, the block texture twice the size of the item icon
		SPOOKY_BUSH = reg.block("spooky_bush", p -> new SimpleBushBlock(p, Block.box(1, 0, 1, 15, 15, 15)))
				.properties(p -> p.mapColor(MapColor.PLANT).instabreak().sound(SoundType.GRASS)
						.noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models()
						.withExistingParent(ctx.getName(), pvd.modLoc("custom/nature/spooky_bush"))
						.texture("1", pvd.modLoc("block/nature/" + ctx.getName()))
						.texture("particle", pvd.modLoc("block/nature/" + ctx.getName())).renderType("cutout")))
				.loot(RegistrateBlockLootTables::dropSelf)
				.item().model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/" + ctx.getName())))
				.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.3f))
				.build()
				.register();

		BIRD_NEST = reg.block("bird_nest", BirdNestBlock::new)
				.properties(p -> p.mapColor(MapColor.WOOD).strength(0.2f).sound(SoundType.GRASS)
						.noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> {
					for (boolean hasEgg : new boolean[]{false, true}) {
						String name = hasEgg ? "bird_nest_with_egg" : ctx.getName();
						var model = pvd.models().withExistingParent(name, pvd.modLoc("custom/nature/bird_nest"))
								.texture("0", pvd.modLoc("block/nature/" + name))
								.texture("particle", pvd.modLoc("block/nature/" + name)).renderType("cutout");
						pvd.getVariantBuilder(ctx.get()).partialState().with(BirdNestBlock.HAS_EGG, hasEgg)
								.modelForState().modelFile(model).addModel();
					}
				})
				.loot(RegistrateBlockLootTables::dropSelf)
				.item()
				.model((ctx, pvd) -> pvd.withExistingParent(ctx.getName(), pvd.modLoc("block/" + ctx.getName()))
						.guiLight(BlockModel.GuiLight.SIDE)
						.transforms().transform(ItemDisplayContext.GUI).rotation(30, 225, 0).scale(0.625f).end().end())
				.build()
				.register();

		// 燃蒲
		FLAME_CATTAIL = reg.block("flame_cattail", WaterloggedBushBlock::new)
				.properties(p -> p.offsetType(BlockBehaviour.OffsetType.XYZ).mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
						pvd.models().getBuilder("block/" + ctx.getName())
								.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/nature/flame_cattail")))
								.texture("cattail_bottom", pvd.modLoc("block/nature/flame_cattail_bottom"))
								.texture("cattail_top", pvd.modLoc("block/nature/flame_cattail_top"))
								.renderType("cutout")))
				.item().model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
						.parent(new ModelFile.UncheckedModelFile("item/generated"))
						.texture("layer0", pvd.modLoc("item/ingredient/flame_cattail")))
				.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.65f))
				.build()
				.register();

		// 大睡莲:浮在水面上,七种外观
		LARGE_WATER_LILY = reg.block("large_water_lily", LargeWaterLilyBlock::new)
				.properties(p -> p.mapColor(MapColor.PLANT).instabreak().sound(SoundType.LILY_PAD).noOcclusion()
						.offsetType(BlockBehaviour.OffsetType.XYZ).dynamicShape().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> {
					var builder = pvd.getVariantBuilder(ctx.get());
					for (int i = 0; i < LargeWaterLilyBlock.VARIANTS; i++) {
						for (Direction dir : Direction.Plane.HORIZONTAL) {
							builder.partialState().with(WaterLilyBlock.FACING, dir).with(LargeWaterLilyBlock.VARIANT, i)
									.modelForState().modelFile(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/nature/water_lily_" + i)))
									.rotationY(((int) dir.toYRot() + 180) % 360).addModel();
						}
					}
				})
				.item((block, props) -> new PlaceOnWaterBlockItem(block, props))
				.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("block/nature/" + ctx.getName())))
				.build()
				.register();

		// 小睡莲
		SMALL_WATER_LILY = reg.block("small_water_lily", WaterLilyBlock::new)
				.properties(p -> p.mapColor(MapColor.PLANT).instabreak().sound(SoundType.LILY_PAD).noOcclusion()
						.offsetType(BlockBehaviour.OffsetType.XYZ).dynamicShape().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> {
					var builder = pvd.getVariantBuilder(ctx.get());
					for (Direction dir : Direction.Plane.HORIZONTAL) {
						builder.partialState().with(WaterLilyBlock.FACING, dir)
								.modelForState().modelFile(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/nature/water_lily_small")))
								.rotationY(((int) dir.toYRot() + 180) % 360).addModel();
					}
				})
				.item((block, props) -> new PlaceOnWaterBlockItem(block, props))
				.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("block/nature/" + ctx.getName())))
				.build()
				.register();

		BROOM_GRASS = reg.block("broom_grass", TallGrassBlock::new)
				.initialProperties(() -> Blocks.SHORT_GRASS)
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
						pvd.models().cross(ctx.getName(), pvd.modLoc("block/plant/" + ctx.getName()))
								.renderType("cutout")))
				.loot((pvd, block) -> {
					var shearsOrSilk = MatchTool.toolMatches(ItemPredicate.Builder.item().of(Items.SHEARS))
							.or(MatchTool.toolMatches(ItemPredicate.Builder.item()
									.withSubPredicate(ItemSubPredicates.ENCHANTMENTS,
											ItemEnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(
													pvd.getRegistries().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
													MinMaxBounds.Ints.atLeast(1)))))));
					pvd.add(block, LootTable.lootTable()
							.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(shearsOrSilk)
									.add(LootItem.lootTableItem(block)))
							.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(shearsOrSilk.invert())
									.add(LootItem.lootTableItem(GLItems.MYSTICAL_STRAW.get()))));
				})
				.item().model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
						.parent(new ModelFile.UncheckedModelFile("item/generated"))
						.texture("layer0", pvd.modLoc("block/plant/" + ctx.getName())))
				.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.5f))
				.build()
				.register();

		// 蕨菜
		BRACKEN = reg.block("bracken", p -> new SimpleBushBlock(p, SimpleBushBlock.THIN))
				.properties(p -> p.offsetType(BlockBehaviour.OffsetType.XYZ).mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY).dynamicShape())
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
						pvd.models().cross(ctx.getName(), pvd.modLoc("block/nature/bracken"))
								.renderType("cutout")))
				.item().model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
						.parent(new ModelFile.UncheckedModelFile("item/generated"))
						.texture("layer0", pvd.modLoc("block/nature/" + ctx.getName())))
				.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.65f))
				.build()
				.register();

		// 常青垂藤
		EVERGREEN_VINE = reg.block("evergreen_vine", EvergreenVineHeadBlock::new)
				.properties(p -> p.mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
						pvd.models().getBuilder("block/" + ctx.getName())
								.parent(new ModelFile.UncheckedModelFile("minecraft:block/cross"))
								.texture("cross", pvd.modLoc("block/nature/evergreen_vine"))
								.renderType("cutout")))
				.item().model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
						.parent(new ModelFile.UncheckedModelFile("item/generated"))
						.texture("layer0", pvd.modLoc("block/nature/" + ctx.getName())))
				.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.5f))
				.build()
				.register();

		EVERGREEN_VINE_PLANT = reg.block("evergreen_vine_plant", EvergreenVineBodyBlock::new)
				.properties(p -> p.mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
						pvd.models().getBuilder("block/" + ctx.getName())
								.parent(new ModelFile.UncheckedModelFile("minecraft:block/cross"))
								.texture("cross", pvd.modLoc("block/nature/evergreen_vine_plant"))
								.renderType("cutout")))
				.register();

		// 青杉落叶
		CEDAR_FALLEN_LEAVES = GLBlockDatagenHelper.regFallenLeaves(reg, "cedar_fallen_leaves");
		ORANGE_MAPLE_FALLEN_LEAVES = GLBlockDatagenHelper.regFallenLeaves(reg, "orange_maple_fallen_leaves");
		RED_MAPLE_FALLEN_LEAVES = GLBlockDatagenHelper.regFallenLeaves(reg, "red_maple_fallen_leaves");
		YELLOW_MAPLE_FALLEN_LEAVES = GLBlockDatagenHelper.regFallenLeaves(reg, "yellow_maple_fallen_leaves");
		MIXED_MAPLE_FALLEN_LEAVES = GLBlockDatagenHelper.regFallenLeaves(reg, "mixed_maple_fallen_leaves");
		PINK_CHERRY_FALLEN_LEAVES = GLBlockDatagenHelper.regFallenLeaves(reg, "pink_cherry_fallen_leaves");
		PURPLE_CHERRY_FALLEN_LEAVES = GLBlockDatagenHelper.regFallenLeaves(reg, "purple_cherry_fallen_leaves");
		LOCUST_FALLEN_LEAVES = GLBlockDatagenHelper.regFallenLeaves(reg, "locust_fallen_leaves");
		MIXED_FALLEN_LEAVES = GLBlockDatagenHelper.regFallenLeaves(reg, "mixed_fallen_leaves");

		// 青杉
		BLUE_FUR_SET = new TreeSet(
				reg, new WoodSet(reg, "blue_fir", Items.SPRUCE_PLANKS,
				Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_WOOD,
				BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LOG).mapColor(MapColor.COLOR_CYAN), null),
				"blue_fir",
				BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LEAVES),
				TreeFeatures.TreeType.BLUE_FIR
		);

		// 枫: one wood, three autumn leaf colours
		MAPLE_WOOD = new WoodSet(reg, "maple", Items.OAK_PLANKS,
				Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_WOOD,
				BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LOG).mapColor(MapColor.COLOR_ORANGE), null);
		ORANGE_MAPLE_SET = new TreeSet(reg, MAPLE_WOOD, "orange_maple",
				BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LEAVES), TreeFeatures.TreeType.ORANGE_MAPLE,
				p -> new SheddingLeavesBlock(p, () -> GLParticles.ORANGE_MAPLE_LEAF.get()));
		RED_MAPLE_SET = new TreeSet(reg, MAPLE_WOOD, "red_maple",
				BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LEAVES), TreeFeatures.TreeType.RED_MAPLE,
				p -> new SheddingLeavesBlock(p, () -> GLParticles.RED_MAPLE_LEAF.get()));
		YELLOW_MAPLE_SET = new TreeSet(reg, MAPLE_WOOD, "yellow_maple",
				BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LEAVES), TreeFeatures.TreeType.YELLOW_MAPLE,
				p -> new SheddingLeavesBlock(p, () -> GLParticles.YELLOW_MAPLE_LEAF.get()));

		// 樱: two blossom colours, both growing on the vanilla cherry trunk
		PINK_CHERRY_SET = new TreeSet(reg, () -> Blocks.CHERRY_LOG, "pink_cherry",
				BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES), TreeFeatures.TreeType.PINK_CHERRY,
				p -> new SheddingLeavesBlock(p, () -> GLParticles.PINK_CHERRY_BLOSSOM.get()));
		PURPLE_CHERRY_SET = new TreeSet(reg, () -> Blocks.CHERRY_LOG, "purple_cherry",
				BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES), TreeFeatures.TreeType.PURPLE_CHERRY,
				p -> new SheddingLeavesBlock(p, () -> GLParticles.PURPLE_CHERRY_BLOSSOM.get()));

		// 槐: the only tree with a hollow trunk, and the only one with planks and a stripped trunk of its own
		var locustProp = BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LOG).mapColor(MapColor.COLOR_PURPLE);
		var strippedLocustTexture = GensokyoLegacy.loc("block/wood/stripped_locust_log_side");
		var locustWallTexture = GensokyoLegacy.loc("block/wood/locust_wall");
		LOCUST_PLANKS = reg.block("locust_planks", Block::new)
				.properties(p -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_PURPLE))
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(),
						pvd.modLoc("block/wood/" + ctx.getName()))))
				.tag(BlockTags.MINEABLE_WITH_AXE, BlockTags.PLANKS)
				.item().tag(ItemTags.PLANKS).build()
				.register();
		STRIPPED_LOCUST_LOG = reg.block("stripped_locust_log", RotatedPillarBlock::new)
				.properties(p -> locustProp)
				.blockstate((ctx, pvd) -> GLBlockDatagenHelper.genColumnState(ctx, pvd,
						strippedLocustTexture, pvd.modLoc("block/wood/locust_log_top")))
				.tag(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN)
				.item().tag(ItemTags.LOGS_THAT_BURN, ItemTags.LOGS).build()
				.recipe((ctx, pvd) -> GLRecipeGen.unlock(pvd, ShapelessRecipeBuilder.shapeless(
								RecipeCategory.BUILDING_BLOCKS, LOCUST_PLANKS.get(), 4)::unlockedBy, ctx.get().asItem())
						.requires(ctx.get()).save(pvd, GensokyoLegacy.loc("stripped_locust_log_to_locust_planks")))
				.register();
		STRIPPED_LOCUST_WOOD = reg.block("stripped_locust_wood", RotatedPillarBlock::new)
				.properties(p -> locustProp)
				.blockstate((ctx, pvd) ->
						GLBlockDatagenHelper.genColumnState(ctx, pvd, strippedLocustTexture, strippedLocustTexture))
				.tag(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN)
				.item().tag(ItemTags.LOGS_THAT_BURN, ItemTags.LOGS).build()
				.recipe((ctx, pvd) -> GLRecipeGen.unlock(pvd, ShapelessRecipeBuilder.shapeless(
								RecipeCategory.BUILDING_BLOCKS, LOCUST_PLANKS.get(), 4)::unlockedBy, ctx.get().asItem())
						.requires(ctx.get()).save(pvd, GensokyoLegacy.loc("stripped_locust_wood_to_locust_planks")))
				.register();
		LOCUST_WALL = reg.block("locust_wall", WallBlock::new)
				.initialProperties(() -> Blocks.ACACIA_LOG)
				.properties(p -> p.mapColor(MapColor.COLOR_PURPLE).forceSolidOn())
				.blockstate((ctx, pvd) -> pvd.wallBlock(ctx.get(), locustWallTexture))
				.tag(BlockTags.WALLS, BlockTags.MINEABLE_WITH_AXE)
				.item().model((ctx, pvd) -> pvd.wallInventory(ctx.getName(), locustWallTexture))
				.tag(ItemTags.WALLS).tab(GLDecoBlocks.TAB.key())
				.dataMap(NeoForgeDataMaps.FURNACE_FUELS, new FurnaceFuel(150))
				.build()
				.recipe((ctx, pvd) ->
						RegistrateRecipeProvider.wall(pvd, RecipeCategory.BUILDING_BLOCKS, ctx.get(), STRIPPED_LOCUST_LOG.get()))
				.register();
		LOCUST_WOOD = new WoodSet(reg, "locust", LOCUST_PLANKS, STRIPPED_LOCUST_LOG, STRIPPED_LOCUST_WOOD,
				locustProp, "locust_hollow_log");
		LOCUST_SET = new TreeSet(reg, LOCUST_WOOD, "locust",
				BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LEAVES), TreeFeatures.TreeType.LOCUST,
				// half the particles and half the litter of the other trees
				p -> new SheddingLeavesBlock(p, () -> GLParticles.LOCUST_LEAF.get(),
						SheddingLeavesBlock.SHED_CHANCE / 4, SheddingLeavesBlock.PARTICLE_RATE * 4));

		CYAN_MUSHROOM_STEM = MushroomSet.regStem(reg, "cyan_mushroom");
		PURPLE_MUSHROOM_STEM = MushroomSet.regStem(reg, "purple_mushroom");
		RED_MUSHROOM_STEM = MushroomSet.regStem(reg, "red_mushroom");

		GHOST_FIRE_MUSHROOM_SET = new MushroomSet(
				reg, "ghost_fire_mushroom", CYAN_MUSHROOM_STEM, 3, true,
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.COLOR_CYAN).lightLevel(b -> 8),
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM).mapColor(MapColor.COLOR_CYAN).lightLevel(b -> 8),
				MushroomFeatures.MushroomTreeType.GHOST_FIRE
		);

		DREAM_MUSHROOM_SET = new MushroomSet(
				reg, "dream_mushroom", PURPLE_MUSHROOM_STEM, 3, false,
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.COLOR_PURPLE),
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM).mapColor(MapColor.COLOR_PURPLE),
				MushroomFeatures.MushroomTreeType.DREAM
		);

		DEMONIC_MIASMA_MUSHROOM_SET = new MushroomSet(
				reg, "demonic_miasma_mushroom", RED_MUSHROOM_STEM, 2, false,
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.CRIMSON_HYPHAE),
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM).mapColor(MapColor.CRIMSON_HYPHAE),
				MushroomFeatures.MushroomTreeType.DEMONIC_MIASMA,
				(t, p) -> new MiasmaMushroomCapBlock(t.growthSmall, t.growthMedium, t.growthLarge, p),
				MiasmaMushroomHugeBlock::new
		);

		// 红耳菇
		EUGUNE_RED = reg.block("eugune_red", p -> new SideBushBlock(p, Items.RED_MUSHROOM))
				.properties(p -> p.mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
						pvd.models().getBuilder("block/" + ctx.getName())
								.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/nature/eugune")))
								.texture("all", pvd.modLoc("block/nature/eugune_red"))
								.renderType("cutout")))
				.loot(SideBushBlock::loot)
				.item().dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.65f)).build()
				.register();

		// 棕耳菇
		EUGUNE_BROWN = reg.block("eugune_brown", p -> new SideBushBlock(p, Items.BROWN_MUSHROOM))
				.properties(p -> p.mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
						pvd.models().getBuilder("block/" + ctx.getName())
								.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/nature/eugune")))
								.texture("all", pvd.modLoc("block/nature/eugune_brown"))
								.renderType("cutout")))
				.loot(SideBushBlock::loot)
				.item().dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.65f)).build()
				.register();

		// 鬼火耳菇
		EUGUNE_GHOST_FIRE = reg.block("eugune_ghost_fire", p -> new SideBushBlock(p, GHOST_FIRE_MUSHROOM_SET.cap))
				.properties(p -> p.mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY).lightLevel(b -> 8))
				.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
						pvd.models().getBuilder("block/" + ctx.getName())
								.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/nature/eugune_emissive")))
								.texture("all", pvd.modLoc("block/nature/eugune_ghost_fire"))
								.renderType("cutout")))
				.loot(SideBushBlock::loot)
				.item().dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.65f)).build()
				.register();

		HYPHAE = reg.block("hyphae", HyphaeBlock::new)
				.properties(p -> p.mapColor(MapColor.CRIMSON_HYPHAE).noCollission().noOcclusion().strength(0)
						.sound(SoundType.COBWEB).pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
						pvd.models().cross(ctx.getName(), pvd.modLoc("block/misc/hyphae")).renderType("cutout")))
				.loot(HyphaeBlock::loot)
				.item().model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("block/misc/hyphae")))
				.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.5f))
				.build()
				.register();

		MIASMA_AIR = reg.block("miasma_air", MiasmaAirBlock::new)
				.properties(p -> p.mapColor(MapColor.NONE).replaceable().noCollission().noOcclusion()
						.randomTicks().noLootTable().air().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
						new ModelFile.UncheckedModelFile("minecraft:block/air")))
				.register();
	}

	public static void register() {

	}

	/**
	 * The log and wood of one species, shared by every leaf colour grown on it. An axe strips both to the block
	 * supplied by the caller, which may be a vanilla block or one of this mod's own.
	 */
	public static class WoodSet {

		public final BlockEntry<StrippableWoodBlock> log;
		public final BlockEntry<StrippableWoodBlock> wood;
		/**
		 * The hollow trunk of an old tree; only the locust has one.
		 */
		public final @Nullable BlockEntry<StrippableWoodBlock> hollowLog;

		public WoodSet(L2Registrate reg, String id, Item planks, Block strippedLog, Block strippedWood,
					   BlockBehaviour.Properties logProp, @Nullable String hollowLogId) {
			this(reg, id, () -> planks, () -> strippedLog, () -> strippedWood, logProp, hollowLogId);
		}

		public WoodSet(L2Registrate reg, String id, Supplier<? extends ItemLike> planks,
					   Supplier<? extends Block> strippedLog, Supplier<? extends Block> strippedWood,
					   BlockBehaviour.Properties logProp, @Nullable String hollowLogId) {
			log = reg.block(id + "_log", p -> new StrippableWoodBlock(strippedLog::get, p))
					.properties(p -> logProp)
					.blockstate((ctx, pvd) -> GLBlockDatagenHelper.genColumnState(ctx, pvd,
							pvd.modLoc("block/wood/" + ctx.getName() + "_side"),
							pvd.modLoc("block/wood/" + ctx.getName() + "_top")))
					.tag(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN)
					.item().tag(ItemTags.LOGS_THAT_BURN, ItemTags.LOGS).build()
					.recipe((ctx, pvd) -> GLRecipeGen.unlock(pvd, ShapelessRecipeBuilder.shapeless(
									RecipeCategory.BUILDING_BLOCKS, planks.get(), 4)::unlockedBy, ctx.get().asItem())
							.requires(ctx.get()).save(pvd, GensokyoLegacy.loc(plankRecipe(id + "_log", planks.get()))))
					.register();
			wood = reg.block(id + "_wood", p -> new StrippableWoodBlock(strippedWood::get, p))
					.properties(p -> logProp)
					.blockstate((ctx, pvd) -> {
						var side = pvd.modLoc("block/wood/" + id + "_log_side");
						GLBlockDatagenHelper.genColumnState(ctx, pvd, side, side);
					})
					.tag(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN)
					.item().tag(ItemTags.LOGS_THAT_BURN, ItemTags.LOGS).build()
					.recipe((ctx, pvd) -> GLRecipeGen.unlock(pvd, ShapelessRecipeBuilder.shapeless(
									RecipeCategory.BUILDING_BLOCKS, planks.get(), 4)::unlockedBy, ctx.get().asItem())
							.requires(ctx.get()).save(pvd, GensokyoLegacy.loc(plankRecipe(id + "_wood", planks.get()))))
					.register();
			hollowLog = hollowLogId == null ? null : reg.block(hollowLogId, p -> new StrippableWoodBlock(strippedLog::get, p))
					.properties(p -> logProp)
					.blockstate((ctx, pvd) -> {
						var side = pvd.modLoc("block/wood/" + hollowLogId + "_side");
						GLBlockDatagenHelper.genColumnState(ctx, pvd, side, side);
					})
					.tag(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS, BlockTags.LOGS_THAT_BURN)
					.item().tag(ItemTags.LOGS_THAT_BURN, ItemTags.LOGS).build()
					.register();
		}

		/**
		 * {@code maple_log} cut into {@code oak_planks} -> {@code maple_log_to_oak_planks}.
		 */
		private static String plankRecipe(String id, ItemLike planks) {
			return id + "_to_" + BuiltInRegistries.ITEM.getKey(planks.asItem()).getPath();
		}
	}

	/**
	 * One tree type: its leaves and its sapling, over the trunk it grows on.
	 */
	public static class TreeSet {

		public final Supplier<Block> trunk;
		public final BlockEntry<LeavesBlock> leaves;
		public final BlockEntry<? extends SaplingBlock> sapling;

		public TreeSet(L2Registrate reg, WoodSet wood, String id,
					   BlockBehaviour.Properties leafProp, TreeFeatures.TreeType type) {
			this(reg, () -> wood.log.get(), id, leafProp, type, LeavesBlock::new);
		}

		public TreeSet(L2Registrate reg, WoodSet wood, String id,
					   BlockBehaviour.Properties leafProp, TreeFeatures.TreeType type,
					   Function<BlockBehaviour.Properties, LeavesBlock> leafFactory) {
			this(reg, () -> wood.log.get(), id, leafProp, type, leafFactory);
		}

		public TreeSet(L2Registrate reg, Supplier<Block> trunk, String id,
					   BlockBehaviour.Properties leafProp, TreeFeatures.TreeType type) {
			this(reg, trunk, id, leafProp, type, LeavesBlock::new);
		}

		public TreeSet(L2Registrate reg, Supplier<Block> trunk, String id,
					   BlockBehaviour.Properties leafProp, TreeFeatures.TreeType type,
					   Function<BlockBehaviour.Properties, LeavesBlock> leafFactory) {
			this.trunk = trunk;
			var grower = new TreeGrower(id + "_tree", type.saplingMega, type.saplingTree, Optional.empty());
			// only the blue fir needs the 3x3 stage vanilla has no grower slot for
			Function<BlockBehaviour.Properties, ? extends SaplingBlock> saplingFactory = type.saplingGiant.isPresent()
					? p -> new BlueFirSaplingBlock(grower, type.saplingGiant.orElseThrow(), p)
					: p -> new SaplingBlock(grower, p);
			sapling = reg.block(id + "_sapling", saplingFactory::apply)
					.properties(p -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING))
					.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models().cross(ctx.getName(),
							pvd.modLoc("block/wood/" + ctx.getName())).renderType("cutout")))
					.loot(RegistrateBlockLootTables::dropSelf)
					.tag(BlockTags.SAPLINGS)
					.item().model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
							.parent(new ModelFile.UncheckedModelFile("item/generated"))
							.texture("layer0", pvd.modLoc("block/wood/" + ctx.getName())))
					.tag(ItemTags.SAPLINGS)
					.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.3f))
					.build()
					.register();
			leaves = reg.block(id + "_leaves", p -> leafFactory.apply(p))
					.properties(p -> leafProp)
					.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(),
							pvd.modLoc("block/wood/" + ctx.getName()))))
					.loot((tb, block) -> genLeavesLoot(tb, block, sapling.get()))
					.tag(BlockTags.MINEABLE_WITH_HOE, BlockTags.LEAVES)
					.item().tag(ItemTags.LEAVES)
					.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.3f))
					.build()
					.register();
		}

		private static void genLeavesLoot(RegistrateBlockLootTables tb, Block block, Block sapling) {
			var enchantments = tb.getRegistries().lookupOrThrow(Registries.ENCHANTMENT);
			var silkTouch = MatchTool.toolMatches(ItemPredicate.Builder.item()
							.withSubPredicate(ItemSubPredicates.ENCHANTMENTS,
									ItemEnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(
											enchantments.getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1))))))
					.or(MatchTool.toolMatches(ItemPredicate.Builder.item().of(Items.SHEARS)));
			tb.add(block, LootTable.lootTable()
					.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(LootItem.lootTableItem(block).when(silkTouch)))
					.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(silkTouch.invert())
							.add(tb.applyExplosionDecay(block, LootItem.lootTableItem(sapling)
									.when(BonusLevelTableCondition.bonusLevelFlatChance(
											enchantments.getOrThrow(Enchantments.FORTUNE),
											2 / 512F, 3 / 512F, 4 / 512F, 5 / 512F)))))
					.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(silkTouch.invert())
							.add(tb.applyExplosionDecay(block, LootItem.lootTableItem(Items.STICK)
									.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
									.when(BonusLevelTableCondition.bonusLevelFlatChance(
											enchantments.getOrThrow(Enchantments.FORTUNE),
											0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F))))));
		}
	}

	public static class MushroomSet {

		public final BlockEntry<HugeMushroomBlock> stem;
		public final BlockEntry<? extends HugeMushroomBlock> block;
		public final BlockEntry<? extends Block> cap;

		public static BlockEntry<HugeMushroomBlock> regStem(L2Registrate reg, String stemTex) {
			var stemProp = BlockBehaviour.Properties.ofFullCopy(Blocks.MUSHROOM_STEM);
			return reg.block(stemTex + "_stem", HugeMushroomBlock::new)
					.properties(p -> stemProp)
					.blockstate((ctx, pvd) -> {
						var outer = genHugeMushroomModels(pvd, ctx.getName(),
								pvd.modLoc("block/mushroom/" + stemTex + "_stem"), false);
						genHugeMushroomState(ctx, pvd, outer);
					})
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.loot(RegistrateBlockLootTables::dropWhenSilkTouch)
					.item().model((ctx, pvd) -> genInventoryItemModel(ctx.getName(), pvd))
					.tag(GLTagGen.HUGE_MUSHROOM)
					.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.65f))
					.build()
					.register();
		}

		public MushroomSet(L2Registrate reg, String id, BlockEntry<HugeMushroomBlock> stem, int capVariants,
						   boolean emissive, BlockBehaviour.Properties blockProp,
						   BlockBehaviour.Properties capProp,
						   MushroomFeatures.MushroomTreeType type) {
			this(reg, id, stem, capVariants, emissive, blockProp, capProp, type,
					(t, p) -> new WeightedMushroomBlock(t.growthSmall, t.growthMedium, t.growthLarge, p),
					HugeMushroomBlock::new);
		}

		public MushroomSet(L2Registrate reg, String id, BlockEntry<HugeMushroomBlock> stem, int capVariants,
						   boolean emissive, BlockBehaviour.Properties blockProp,
						   BlockBehaviour.Properties capProp,
						   MushroomFeatures.MushroomTreeType type,
						   BiFunction<MushroomFeatures.MushroomTreeType, BlockBehaviour.Properties, ? extends Block> capFactory,
						   Function<BlockBehaviour.Properties, ? extends HugeMushroomBlock> blockFactory) {
			this.stem = stem;

			cap = reg.block(id, p -> capFactory.apply(type, p))
					.properties(p -> capProp)
					.blockstate((ctx, pvd) -> genCapState(ctx, pvd, capVariants, emissive))
					.item().model((ctx, pvd) -> pvd.generated(ctx,
							pvd.modLoc("block/mushroom/" + capModelName(ctx.getName(), capVariants, 1))))
					.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.65f))
					.build()
					.register();

			block = reg.block(id + "_block", blockFactory::apply)
					.properties(p -> blockProp)
					.blockstate((ctx, pvd) -> {
						var outer = genHugeMushroomModels(pvd, ctx.getName(),
								pvd.modLoc("block/mushroom/" + ctx.getName()), emissive);
						genHugeMushroomState(ctx, pvd, outer);
					})
					.loot((tb, blk) -> tb.add(blk, tb.createMushroomBlockDrop(blk, cap)))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().model((ctx, pvd) -> genInventoryItemModel(ctx.getName(), pvd))
					.tag(GLTagGen.HUGE_MUSHROOM)
					.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.85f))
					.build()
					.register();
		}

		private static void genInventoryItemModel(String name, RegistrateItemModelProvider pvd) {
			pvd.getBuilder(name)
					.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("block/" + name + "_inventory")));
		}

		private static ModelFile genHugeMushroomModels(RegistrateBlockstateProvider pvd, String id, ResourceLocation tex, boolean emissive) {
			ModelFile outer;
			if (emissive) {
				outer = pvd.models().withExistingParent(id, ResourceLocation.withDefaultNamespace("block/block"))
						.texture("texture", tex)
						.texture("particle", tex)
						.element().from(0, 0, 0).to(16, 16, 0)
						.shade(false)
						.emissivity(15, 15)
						.face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#texture").cullface(Direction.NORTH).end()
						.end();
			} else {
				outer = pvd.models().withExistingParent(id, ResourceLocation.withDefaultNamespace("block/template_single_face"))
						.texture("texture", tex);
			}
			pvd.models().withExistingParent(id + "_inventory", ResourceLocation.withDefaultNamespace("block/cube_all"))
					.texture("all", tex);
			return outer;
		}

		private static void genHugeMushroomState(DataGenContext<Block, ? extends Block> ctx, RegistrateBlockstateProvider pvd, ModelFile outer) {
			var builder = pvd.getMultipartBuilder(ctx.get());
			var inside = new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("block/mushroom_block_inside"));
			addFace(builder, outer, inside, HugeMushroomBlock.NORTH, 0, 0);
			addFace(builder, outer, inside, HugeMushroomBlock.EAST, 0, 90);
			addFace(builder, outer, inside, HugeMushroomBlock.SOUTH, 0, 180);
			addFace(builder, outer, inside, HugeMushroomBlock.WEST, 0, 270);
			addFace(builder, outer, inside, HugeMushroomBlock.UP, 270, 0);
			addFace(builder, outer, inside, HugeMushroomBlock.DOWN, 90, 0);
		}

		private static void addFace(MultiPartBlockStateBuilder builder, ModelFile outer, ModelFile inside,
									BooleanProperty prop, int x, int y) {
			builder.part().modelFile(outer).rotationX(x).rotationY(y).uvLock(true).addModel().condition(prop, true).end();
			builder.part().modelFile(inside).rotationX(x).rotationY(y).uvLock(false).addModel().condition(prop, false).end();
		}

		private static BlockModelBuilder emissiveCross(RegistrateBlockstateProvider pvd, String name, ResourceLocation tex) {
			return pvd.models().withExistingParent(name, ResourceLocation.withDefaultNamespace("block/block"))
					.ao(false)
					.renderType("cutout")
					.texture("cross", tex)
					.texture("particle", tex)
					.element()
					.from(0.8f, 0, 8).to(15.2f, 16, 8)
					.shade(false)
					.emissivity(15, 15)
					.rotation().origin(8, 8, 8).axis(Direction.Axis.Y).angle(45).rescale(true).end()
					.face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#cross").end()
					.face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#cross").end()
					.end()
					.element()
					.from(8, 0, 0.8f).to(8, 16, 15.2f)
					.shade(false)
					.emissivity(15, 15)
					.rotation().origin(8, 8, 8).axis(Direction.Axis.Y).angle(45).rescale(true).end()
					.face(Direction.WEST).uvs(0, 0, 16, 16).texture("#cross").end()
					.face(Direction.EAST).uvs(0, 0, 16, 16).texture("#cross").end()
					.end();
		}

		private static String capModelName(String name, int variants, int index) {
			return variants <= 1 ? name : name + "_" + index;
		}

		private static void genCapState(DataGenContext<Block, ? extends Block> ctx, RegistrateBlockstateProvider pvd,
										int variants, boolean emissive) {
			ConfiguredModel[] models = new ConfiguredModel[variants];
			for (int i = 1; i <= variants; i++) {
				var name = capModelName(ctx.getName(), variants, i);
				var tex = pvd.modLoc("block/mushroom/" + name);
				models[i - 1] = new ConfiguredModel(emissive
						? emissiveCross(pvd, name, tex)
						: pvd.models().cross(name, tex).renderType("cutout"));
			}
			pvd.getVariantBuilder(ctx.get()).partialState().setModels(models);
		}
	}

}
