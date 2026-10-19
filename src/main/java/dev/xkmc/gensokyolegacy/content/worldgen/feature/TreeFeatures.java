package dev.xkmc.gensokyolegacy.content.worldgen.feature;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.biomes.MagicalForestBiomeGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.CherryFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.CherryTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;

import java.util.Optional;
import java.util.function.Supplier;

public class TreeFeatures {

	/**
	 * The trunk and crown of a {@link TreeType}. Every tree here grows from its sapling, and six of the seven also
	 * grow in a biome: the maples in the mountain band, the cherries in the sakura grove, the locust in the two
	 * graveyards.
	 *
	 * @see "doc/design/magical_forest_vegetation.md" §9.1
	 * @see "doc/design/gensokyo_biomes.md" §2.2
	 */
	public interface TreeShape {

		TreeConfiguration build(BlockStateProvider log, BlockStateProvider leaves);

		/** Vanilla cherry: a branched trunk under a wide drooping crown, the numbers of {@code minecraft:cherry}. */
		TreeShape CHERRY = new TreeShape() {
			@Override
			public TreeConfiguration build(BlockStateProvider log, BlockStateProvider leaves) {
				return new TreeConfiguration.TreeConfigurationBuilder(log,
						new CherryTrunkPlacer(7, 1, 0,
								new WeightedListInt(SimpleWeightedRandomList.<IntProvider>builder()
										.add(ConstantInt.of(1), 1).add(ConstantInt.of(2), 1).add(ConstantInt.of(3), 1).build()),
								UniformInt.of(2, 4), UniformInt.of(-4, -3), UniformInt.of(-1, 0)),
						leaves,
						new CherryFoliagePlacer(ConstantInt.of(4), ConstantInt.of(0), ConstantInt.of(5), 0.25F, 0.5F, 1 / 6F, 1 / 3F),
						new TwoLayersFeatureSize(1, 0, 2)).ignoreVines().build();
			}
		};

		/** Straight trunk under a layered flat crown, sized to one tree. */
		record Spruce(int trunkBase, int trunkRandA, int trunkRandB,
		              IntProvider radius, IntProvider offset, IntProvider height) implements TreeShape {
			@Override
			public TreeConfiguration build(BlockStateProvider log, BlockStateProvider leaves) {
				return new TreeConfiguration.TreeConfigurationBuilder(log,
						new StraightTrunkPlacer(trunkBase, trunkRandA, trunkRandB),
						leaves,
						new SpruceFoliagePlacer(radius, offset, height),
						new TwoLayersFeatureSize(2, 0, 2)).ignoreVines().build();
			}
		}
	}

	public enum TreeType {
		// young tree only (4-6 tall): saplings grow the hand-built template trees instead
		BLUE_FIR("blue_fir_tree", () -> GLNaturalBlocks.BLUE_FUR_SET,
				new TreeShape.Spruce(4, 2, 0, UniformInt.of(1, 2), UniformInt.of(0, 1), UniformInt.of(1, 2)),
				MagicalForestBiomeGen.BLUE_FIR_MEDIUM,
				MagicalForestBiomeGen.BLUE_FIR_LARGE,
				MagicalForestBiomeGen.BLUE_FIR_GIANT),
		// the maple keeps the blue fir shape, the cherry and the locust grow like minecraft:cherry
		ORANGE_MAPLE("orange_maple_tree", () -> GLNaturalBlocks.ORANGE_MAPLE_SET,
				new TreeShape.Spruce(6, 2, 0, UniformInt.of(2, 3), UniformInt.of(0, 1), UniformInt.of(2, 3))),
		RED_MAPLE("red_maple_tree", () -> GLNaturalBlocks.RED_MAPLE_SET,
				new TreeShape.Spruce(6, 2, 0, UniformInt.of(2, 3), UniformInt.of(0, 1), UniformInt.of(2, 3))),
		YELLOW_MAPLE("yellow_maple_tree", () -> GLNaturalBlocks.YELLOW_MAPLE_SET,
				new TreeShape.Spruce(6, 2, 0, UniformInt.of(2, 3), UniformInt.of(0, 1), UniformInt.of(2, 3))),
		PINK_CHERRY("pink_cherry_tree", () -> GLNaturalBlocks.PINK_CHERRY_SET, TreeShape.CHERRY),
		PURPLE_CHERRY("purple_cherry_tree", () -> GLNaturalBlocks.PURPLE_CHERRY_SET, TreeShape.CHERRY),
		LOCUST("locust_tree", () -> GLNaturalBlocks.LOCUST_SET, TreeShape.CHERRY),
		;

		public final String id;
		public final TreeShape shape;
		public final Supplier<GLNaturalBlocks.TreeSet> set;
		public final ResourceKey<ConfiguredFeature<?, ?>> cfKey;
		// what a sapling grows: the hand-built templates for 1 / 2x2 / 3x3 saplings, or the tree's own
		// configured feature when it has no templates
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
			return new ConfiguredFeature<>(Feature.TREE,
					shape.build(BlockStateProvider.simple(set.trunk.get()), BlockStateProvider.simple(set.leaves.get())));
		}
	}

}
