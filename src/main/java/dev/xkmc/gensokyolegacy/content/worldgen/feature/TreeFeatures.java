package dev.xkmc.gensokyolegacy.content.worldgen.feature;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;

import java.util.function.Supplier;

public class TreeFeatures {

	public enum TreeType {
		// young tree only (4-6 tall): grown blue firs are hand-built templates, see MagicalForestFeatures
		BLUE_FIR("blue_fir_tree",
				() -> GLNaturalBlocks.BLUE_FUR_SET,
				4, 2, 0,
				UniformInt.of(1, 2), UniformInt.of(0, 1), UniformInt.of(1, 2)),
		;

		public final String id;
		public final Supplier<GLNaturalBlocks.TreeSet> set;
		public final int trunkBase;
		public final int trunkRandA;
		public final int trunkRandB;
		public final IntProvider foliageRadius;
		public final IntProvider foliageOffset;
		public final IntProvider foliageHeight;
		public final ResourceKey<ConfiguredFeature<?, ?>> cfKey;

		TreeType(String id,
		         Supplier<GLNaturalBlocks.TreeSet> set,
		         int trunkBase, int trunkRandA, int trunkRandB,
		         IntProvider foliageRadius, IntProvider foliageOffset, IntProvider foliageHeight) {
			this.id = id;
			this.set = set;
			this.trunkBase = trunkBase;
			this.trunkRandA = trunkRandA;
			this.trunkRandB = trunkRandB;
			this.foliageRadius = foliageRadius;
			this.foliageOffset = foliageOffset;
			this.foliageHeight = foliageHeight;
			this.cfKey = ResourceKey.create(Registries.CONFIGURED_FEATURE, GensokyoLegacy.loc(id));
		}

		public ConfiguredFeature<?, ?> createConfiguredFeature() {
			var set = this.set.get();
			return new ConfiguredFeature<>(
					Feature.TREE,
					new TreeConfiguration.TreeConfigurationBuilder(
							BlockStateProvider.simple(set.log.get()),
							new StraightTrunkPlacer(trunkBase, trunkRandA, trunkRandB),
						BlockStateProvider.simple(set.leaves.get()),
						new SpruceFoliagePlacer(foliageRadius, foliageOffset, foliageHeight),
						new TwoLayersFeatureSize(2, 0, 2)
				).ignoreVines().build()
			);
		}
	}

}
