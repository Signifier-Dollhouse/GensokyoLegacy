package dev.xkmc.gensokyolegacy.content.worldgen.feature;

import dev.xkmc.gensokyolegacy.content.worldgen.feature.template.TemplateFeatureConfig;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.template.TemplateReplaceProcessor;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.MagicalForestFeatures;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration.TreeConfigurationBuilder;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.CherryFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.CherryTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class TreeFeatures {

	/**
	 * The trunk and crown of a {@link TreeType}. Every tree here is grown from its sapling and by nothing else.
	 *
	 * @see "doc/design/magical_forest_vegetation.md"
	 */
	public interface TreeShape {

		ConfiguredFeature<?, ?> build(BlockStateProvider log, BlockStateProvider leaves);

		/**
		 * Vanilla cherry: a branched trunk under a wide drooping crown, the numbers of {@code minecraft:cherry}.
		 */
		TreeShape CHERRY = new TreeShape() {
			@Override
			public ConfiguredFeature<?, ?> build(BlockStateProvider log, BlockStateProvider leaves) {
				return new ConfiguredFeature<>(Feature.TREE, new TreeConfigurationBuilder(log,
						new CherryTrunkPlacer(7, 1, 0,
								new WeightedListInt(SimpleWeightedRandomList.<IntProvider>builder()
										.add(ConstantInt.of(1), 1).add(ConstantInt.of(2), 1).add(ConstantInt.of(3), 1).build()),
								UniformInt.of(2, 4), UniformInt.of(-4, -3), UniformInt.of(-1, 0)),
						leaves,
						new CherryFoliagePlacer(ConstantInt.of(4), ConstantInt.of(0), ConstantInt.of(5), 0.25F, 0.5F, 1 / 6F, 1 / 3F),
						new TwoLayersFeatureSize(1, 0, 2)).ignoreVines().build());
			}
		};

		/**
		 * Straight trunk under a layered flat crown, sized to one tree.
		 */
		record Spruce(int trunkBase, int trunkRandA, int trunkRandB,
					  IntProvider radius, IntProvider offset, IntProvider height) implements TreeShape {
			@Override
			public ConfiguredFeature<?, ?> build(BlockStateProvider log, BlockStateProvider leaves) {
				return new ConfiguredFeature<>(Feature.TREE, new TreeConfigurationBuilder(log,
						new StraightTrunkPlacer(trunkBase, trunkRandA, trunkRandB),
						leaves,
						new SpruceFoliagePlacer(radius, offset, height),
						new TwoLayersFeatureSize(2, 0, 2)).ignoreVines().build());
			}
		}

		static TreeShape maple(Supplier<GLNaturalBlocks.TreeSet> tree, Supplier<Block> fallenLeaves) {
			return new Templates(() -> Map.of(
					GLNaturalBlocks.RED_MAPLE_SET.leaves.get(), tree.get().leaves.get(),
					GLNaturalBlocks.RED_MAPLE_FALLEN_LEAVES.get(), fallenLeaves.get()),
					"maple_tree_01", "maple_tree_02", "maple_tree_03", "maple_tree_04", "maple_tree_05");
		}

		record Templates(Supplier<Map<Block, Block>> replacements, String... ids) implements TreeShape {
			Templates(String... ids) {
				this(Map::of, ids);
			}

			@Override
			public ConfiguredFeature<?, ?> build(BlockStateProvider log, BlockStateProvider leaves) {
				var templates = SimpleWeightedRandomList.<ResourceLocation>builder();
				for (var id : ids) {
					templates.add(GensokyoLegacy.loc("magical_forest/tree/" + id), 1);
				}
				var blocks = replacements.get();
				var processors = blocks.isEmpty() ? TemplateFeatureConfig.NO_PROCESSORS
						: Holder.direct(new StructureProcessorList(List.of(new TemplateReplaceProcessor(blocks))));
				return new ConfiguredFeature<>(GLWorldGen.TEMPLATE.get(), new TemplateFeatureConfig(
						templates.build(), processors, BlockTags.DIRT,
						0, 0, 0, 3, List.of()));
			}
		}
	}

	public enum TreeType {
		// young tree only (4-6 tall): saplings grow the hand-built template trees instead
		BLUE_FIR("blue_fir_tree", () -> GLNaturalBlocks.BLUE_FUR_SET,
				new TreeShape.Spruce(4, 2, 0, UniformInt.of(1, 2), UniformInt.of(0, 1), UniformInt.of(1, 2)),
				MagicalForestFeatures.BLUE_FIR_MEDIUM,
				MagicalForestFeatures.BLUE_FIR_LARGE,
				MagicalForestFeatures.BLUE_FIR_GIANT),
		// the maple and locust use templates; the cherry grows like minecraft:cherry
		ORANGE_MAPLE("orange_maple_tree", () -> GLNaturalBlocks.ORANGE_MAPLE_SET,
				TreeShape.maple(() -> GLNaturalBlocks.ORANGE_MAPLE_SET,
						() -> GLNaturalBlocks.ORANGE_MAPLE_FALLEN_LEAVES.get())),
		RED_MAPLE("red_maple_tree", () -> GLNaturalBlocks.RED_MAPLE_SET,
				TreeShape.maple(() -> GLNaturalBlocks.RED_MAPLE_SET,
						() -> GLNaturalBlocks.RED_MAPLE_FALLEN_LEAVES.get())),
		YELLOW_MAPLE("yellow_maple_tree", () -> GLNaturalBlocks.YELLOW_MAPLE_SET,
				TreeShape.maple(() -> GLNaturalBlocks.YELLOW_MAPLE_SET,
						() -> GLNaturalBlocks.YELLOW_MAPLE_FALLEN_LEAVES.get())),
		PINK_CHERRY("pink_cherry_tree", () -> GLNaturalBlocks.PINK_CHERRY_SET, TreeShape.CHERRY),
		PURPLE_CHERRY("purple_cherry_tree", () -> GLNaturalBlocks.PURPLE_CHERRY_SET, TreeShape.CHERRY),
		LOCUST("locust_tree", () -> GLNaturalBlocks.LOCUST_SET,
				new TreeShape.Templates("locust_1", "locust_2", "locust_3", "locust_4", "locust_5")),
		;

		public final String id;
		public final TreeShape shape;
		public final Supplier<GLNaturalBlocks.TreeSet> set;
		public final ResourceKey<ConfiguredFeature<?, ?>> cfKey;
		// what a sapling grows: the blue fir's features for 1 / 2x2 / 3x3 saplings, or the tree's own
		// configured feature
		public final Optional<ResourceKey<ConfiguredFeature<?, ?>>> saplingTree;
		public final Optional<ResourceKey<ConfiguredFeature<?, ?>>> saplingMega;
		public final Optional<ResourceKey<ConfiguredFeature<?, ?>>> saplingGiant;

		TreeType(String id, Supplier<GLNaturalBlocks.TreeSet> set, TreeShape shape,
				 ResourceKey<ConfiguredFeature<?, ?>> saplingTree,
				 ResourceKey<ConfiguredFeature<?, ?>> saplingMega, ResourceKey<ConfiguredFeature<?, ?>> saplingGiant) {
			this(id, set, shape, Optional.of(saplingTree), Optional.of(saplingMega), Optional.of(saplingGiant));
		}

		TreeType(String id, Supplier<GLNaturalBlocks.TreeSet> set, TreeShape shape) {
			this(id, set, shape, Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, GensokyoLegacy.loc(id))),
					Optional.empty(), Optional.empty());
		}

		TreeType(String id, Supplier<GLNaturalBlocks.TreeSet> set, TreeShape shape,
				 Optional<ResourceKey<ConfiguredFeature<?, ?>>> saplingTree,
				 Optional<ResourceKey<ConfiguredFeature<?, ?>>> saplingMega,
				 Optional<ResourceKey<ConfiguredFeature<?, ?>>> saplingGiant) {
			this.id = id;
			this.shape = shape;
			this.set = set;
			this.cfKey = ResourceKey.create(Registries.CONFIGURED_FEATURE, GensokyoLegacy.loc(id));
			this.saplingTree = saplingTree;
			this.saplingMega = saplingMega;
			this.saplingGiant = saplingGiant;
		}

		public ConfiguredFeature<?, ?> createConfiguredFeature() {
			var set = this.set.get();
			return shape.build(BlockStateProvider.simple(set.trunk.get()), BlockStateProvider.simple(set.leaves.get()));
		}
	}

}
