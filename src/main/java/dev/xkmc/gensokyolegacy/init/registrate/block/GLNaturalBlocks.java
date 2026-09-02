package dev.xkmc.gensokyolegacy.init.registrate.block;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import dev.xkmc.gensokyolegacy.content.block.nature.CedarFallenLeavesBlock;
import dev.xkmc.gensokyolegacy.content.block.nature.EvergreenVineBodyBlock;
import dev.xkmc.gensokyolegacy.content.block.nature.EvergreenVineHeadBlock;
import dev.xkmc.gensokyolegacy.content.block.nature.WaterloggedCrossBlock;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.MushroomFeatures;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import net.minecraft.advancements.critereon.*;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GLNaturalBlocks {

	public static final BlockEntry<GrassBlock> STAR_FLOWER;
	public static final BlockEntry<WaterloggedCrossBlock> FLAME_CATTAIL;
	public static final BlockEntry<GrassBlock> BRACKEN;
	public static final BlockEntry<DelegateBlock> EUGUNE_RED, EUGUNE_BROWN, EUGUNE_GHOST_FIRE;
	public static final BlockEntry<EvergreenVineHeadBlock> EVERGREEN_VINE;
	public static final BlockEntry<EvergreenVineBodyBlock> EVERGREEN_VINE_PLANT;
	public static final BlockEntry<CedarFallenLeavesBlock> CEDAR_FALLEN_LEAVES;

	public static final TreeSet BLUE_FUR_SET;

	public static final MushroomSet GHOST_FIRE_MUSHROOM_SET, DREAM_MUSHROOM_SET, DEMONIC_MIASMA_MUSHROOM_SET;

	public static final BlockEntry<TallGrassBlock> BROOM_GRASS;

	static {
		var reg = GensokyoLegacy.REGISTRATE;

		// 星星花
		STAR_FLOWER = reg.block("star_flower", GrassBlock::new)
				.properties(p -> p.offsetType(BlockBehaviour.OffsetType.XYZ).mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> {
				})
				.simpleItem()
				.register();

		// 燃蒲
		FLAME_CATTAIL = reg.block("flame_cattail", WaterloggedCrossBlock::new)
				.properties(p -> p.offsetType(BlockBehaviour.OffsetType.XYZ).mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
						pvd.models().getBuilder("block/" + ctx.getName())
								.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/flame_cattail")))
								.renderType("cutout")))
				.simpleItem()
				.register();

		// 蕨菜
		BRACKEN = reg.block("bracken", GrassBlock::new)
				.properties(p -> p.offsetType(BlockBehaviour.OffsetType.XYZ).mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().pushReaction(PushReaction.DESTROY).dynamicShape())
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
						new ModelFile.UncheckedModelFile(pvd.modLoc("block/bracken"))))
				.simpleItem()
				.register();

		// 红耳姑
		EUGUNE_RED = reg.block("eugune_red", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL))
				.properties(p -> p.mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
						new ModelFile.UncheckedModelFile(pvd.modLoc("block/eugune_red"))))
				.simpleItem()
				.register();

		// 棕耳姑
		EUGUNE_BROWN = reg.block("eugune_brown", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL))
				.properties(p -> p.mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
						new ModelFile.UncheckedModelFile(pvd.modLoc("block/eugune_brown"))))
				.simpleItem()
				.register();

		// 鬼火耳姑
		EUGUNE_GHOST_FIRE = reg.block("eugune_ghost_fire", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL))
				.properties(p -> p.mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
						new ModelFile.UncheckedModelFile(pvd.modLoc("block/eugune_ghost_fire"))))
				.simpleItem()
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
						.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("block/" + ctx.getName()))))
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
		CEDAR_FALLEN_LEAVES = reg.block("cedar_fallen_leaves", CedarFallenLeavesBlock::new)
				.properties(p -> p.mapColor(MapColor.PLANT).strength(0).sound(SoundType.GRASS).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> {
					var layer1 = pvd.models().getBuilder("block/" + ctx.getName() + "_layer1")
							.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/cedar_fallen_leaves_layer1")))
							.renderType("cutout");
					var layer2 = pvd.models().getBuilder("block/" + ctx.getName() + "_layer2")
							.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/cedar_fallen_leaves_layer2")))
							.renderType("cutout");
					pvd.getVariantBuilder(ctx.get())
							.partialState().with(CedarFallenLeavesBlock.LAYERS, 1).modelForState().modelFile(layer1).addModel()
							.partialState().with(CedarFallenLeavesBlock.LAYERS, 2).modelForState().modelFile(layer2).addModel();
				})
				.item().model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
						.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/cedar_fallen_leaves_layer1"))))
				.build()
				.register();


		BLUE_FUR_SET = new TreeSet(
				reg, "blue_fir",
				BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LOG).mapColor(MapColor.COLOR_CYAN),
				BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LEAVES),
				TreeFeatures.TreeType.BLUE_FIR
		);

		GHOST_FIRE_MUSHROOM_SET = new MushroomSet(
				reg, "ghost_fire_mushroom", "cyan_mushroom", false, 3, true,
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.COLOR_CYAN).lightLevel(b -> 5),
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM).mapColor(MapColor.COLOR_CYAN).lightLevel(b -> 5),
				MushroomFeatures.MushroomTreeType.GHOST_FIRE.cfKey
		);

		DREAM_MUSHROOM_SET = new MushroomSet(
				reg, "dream_mushroom", "purple_mushroom", false, 3, false,
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.COLOR_PURPLE),
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM).mapColor(MapColor.COLOR_PURPLE),
				null
		);

		DEMONIC_MIASMA_MUSHROOM_SET = new MushroomSet(
				reg, "demonic_miasma_mushroom", "red_mushroom", false, 2, false,
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.CRIMSON_HYPHAE),
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM).mapColor(MapColor.CRIMSON_HYPHAE),
				null
		);

		BROOM_GRASS = reg.block("broom_grass", TallGrassBlock::new)
				.initialProperties(() -> Blocks.SHORT_GRASS)
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
						pvd.models().cross(ctx.getName(), pvd.modLoc("block/plant/" + ctx.getName()))
								.renderType("cutout")))
				.loot((pvd, block) -> pvd.add(block, RegistrateBlockLootTables.createShearsOnlyDrop(block)))
				.item().model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
						.parent(new ModelFile.UncheckedModelFile("item/generated"))
						.texture("layer0", pvd.modLoc("block/plant/" + ctx.getName()))).build()
				.register();
	}

	public static void register() {

	}

	public static class TreeSet {

		public final BlockEntry<RotatedPillarBlock> log;
		public final BlockEntry<LeavesBlock> leaves;
		public final BlockEntry<SaplingBlock> sapling;

		public TreeSet(L2Registrate reg, String id,
		               BlockBehaviour.Properties logProp, BlockBehaviour.Properties leafProp,
		               TreeFeatures.TreeType type) {
			log = reg.block(id + "_log", RotatedPillarBlock::new)
					.properties(p -> logProp)
					.blockstate((ctx, pvd) -> genColumnState(ctx, pvd,
							pvd.modLoc("block/wood/" + ctx.getName() + "_side"),
							pvd.modLoc("block/wood/" + ctx.getName() + "_top")))
					.tag(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS)
					.simpleItem()
					.register();
			leaves = reg.block(id + "_leaves", LeavesBlock::new)
					.properties(p -> leafProp)
					.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(),
							pvd.modLoc("block/wood/" + ctx.getName())).renderType("cutout")))
					.loot(TreeSet::genLeavesLoot)
					.tag(BlockTags.MINEABLE_WITH_HOE, BlockTags.LEAVES)
					.simpleItem()
					.register();
			sapling = reg.block(id + "_sapling", p -> new SaplingBlock(new TreeGrower(
							id + "_tree", Optional.empty(), Optional.of(type.cfKey), Optional.empty()), p))
					.properties(p -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING))
					.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models().cross(ctx.getName(),
							pvd.modLoc("block/wood/" + ctx.getName())).renderType("cutout")))
					.loot(RegistrateBlockLootTables::dropSelf)
					.tag(BlockTags.SAPLINGS)
					.item().model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
							.parent(new ModelFile.UncheckedModelFile("item/generated"))
							.texture("layer0", pvd.modLoc("block/wood/" + ctx.getName()))).build()
					.register();
		}

		private static void genLeavesLoot(RegistrateBlockLootTables tb, Block block) {
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
							.add(tb.applyExplosionDecay(block, LootItem.lootTableItem(Items.STICK)
									.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
									.when(BonusLevelTableCondition.bonusLevelFlatChance(
											enchantments.getOrThrow(Enchantments.FORTUNE),
											0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F))))));
		}
	}

	public static class MushroomSet {

		private static final Map<String, BlockEntry<Block>> STEMS = new HashMap<>();

		public final BlockEntry<Block> stem;
		public final BlockEntry<Block> block;
		public final BlockEntry<? extends Block> cap;

		public MushroomSet(L2Registrate reg, String id, String stemTex, boolean pillarStem, int capVariants,
		                   boolean emissive, BlockBehaviour.Properties blockProp,
		                   BlockBehaviour.Properties capProp,
		                   @Nullable ResourceKey<ConfiguredFeature<?, ?>> feature) {
			stem = STEMS.computeIfAbsent(stemTex + ":" + pillarStem, key -> {
				var stemProp = BlockBehaviour.Properties.ofFullCopy(Blocks.MUSHROOM_STEM);
				var stemBuilder = reg.block(stemTex + "_stem", p -> pillarStem ? new RotatedPillarBlock(p) : new Block(p))
						.properties(p -> stemProp)
						.tag(BlockTags.MINEABLE_WITH_AXE);
				if (pillarStem) {
					stemBuilder.blockstate((ctx, pvd) -> {
						var side = pvd.modLoc("block/mushroom/" + stemTex + "_stem_side");
						var top = pvd.modLoc("block/mushroom/" + stemTex + "_stem_top");
						genColumnState(ctx, pvd, side, top);
					});
				} else {
					stemBuilder.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(),
							pvd.modLoc("block/mushroom/" + stemTex + "_stem"))));
				}
				return stemBuilder
						.loot(RegistrateBlockLootTables::dropWhenSilkTouch)
						.item().tag(GLTagGen.HUGE_MUSHROOM).build()
						.register();
			});

			NonNullFunction<BlockBehaviour.Properties, ? extends Block> capFactory;
			if (feature == null) {
				capFactory = HugeMushroomBlock::new;
			} else {
				capFactory = p -> new MushroomBlock(feature, p);
			}
			cap = reg.block(id, capFactory)
					.properties(p -> capProp)
					.blockstate((ctx, pvd) -> genCapState(ctx, pvd, capVariants, emissive))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().model((ctx, pvd) -> genFlatItemModel(ctx.getName(), pvd,
							pvd.modLoc("block/mushroom/" + capModelName(ctx.getName(), capVariants, 1)), emissive)).build()
					.register();

			block = reg.block(id + "_block", Block::new)
					.properties(p -> blockProp)
					.blockstate((ctx, pvd) -> genPlainState(ctx, pvd, emissive))
					.loot((tb, blk) -> tb.add(blk, tb.createMushroomBlockDrop(blk, cap)))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().tag(GLTagGen.HUGE_MUSHROOM).build()
					.register();
		}

		private static void genFlatItemModel(String name, RegistrateItemModelProvider pvd, ResourceLocation tex, boolean emissive) {
			if (!emissive) {
				pvd.getBuilder(name)
						.parent(new ModelFile.UncheckedModelFile("item/generated"))
						.texture("layer0", tex);
				return;
			}
			pvd.getBuilder(name)
					.texture("layer0", tex)
					.texture("particle", tex)
					.guiLight(BlockModel.GuiLight.FRONT)
					.ao(false)
					.transforms()
					.transform(ItemDisplayContext.GROUND).translation(0, 2, 0).scale(0.5f).end()
					.transform(ItemDisplayContext.HEAD).rotation(0, 180, 0).translation(0, 13, 7).end()
					.transform(ItemDisplayContext.FIXED).rotation(0, 180, 0).end()
					.end()
					.element()
					.from(0, 0, 7.5f).to(16, 16, 8.5f)
					.shade(false)
					.emissivity(15, 15)
					.face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#layer0").end()
					.face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#layer0").end()
					.end();
		}

		private static void genPlainState(DataGenContext<Block, ? extends Block> ctx, RegistrateBlockstateProvider pvd, boolean emissive) {
			if (emissive) {
				pvd.simpleBlock(ctx.get(), emissiveCube(pvd, ctx.getName(),
						pvd.modLoc("block/mushroom/" + ctx.getName())));
			} else {
				pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(),
						pvd.modLoc("block/mushroom/" + ctx.getName())));
			}
		}

		private static BlockModelBuilder emissiveCube(RegistrateBlockstateProvider pvd, String name, ResourceLocation tex) {
			return pvd.models().withExistingParent(name, ResourceLocation.withDefaultNamespace("block/block"))
					.texture("all", tex)
					.texture("particle", tex)
					.element().from(0, 0, 0).to(16, 16, 16)
					.emissivity(15, 15)
					.cube("#all")
					.end();
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

	private static void genColumnState(DataGenContext<Block, ? extends Block> ctx, RegistrateBlockstateProvider pvd,
	                                   ResourceLocation side, ResourceLocation end) {
		var vertical = pvd.models().cubeColumn(ctx.getName(), side, end);
		var horizontal = pvd.models().cubeColumnHorizontal(ctx.getName() + "_horizontal", side, end);
		pvd.getVariantBuilder(ctx.get())
				.partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.Y)
				.modelForState().modelFile(vertical).addModel()
				.partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.Z)
				.modelForState().modelFile(horizontal).rotationX(90).addModel()
				.partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.X)
				.modelForState().modelFile(horizontal).rotationX(90).rotationY(90).addModel();
	}

}
