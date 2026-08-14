package dev.xkmc.gensokyolegacy.content.worldgen.feature;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLDecoBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;

import java.util.function.Supplier;

public class TreeFeatures {

    public enum TreeType {
        BLUE_FIR("blue_fir_tree",
                () -> GLDecoBlocks.BLUE_FUR_SET,
                4, 2, 0, 2, 0, 3),
        ;

        public final String id;
        public final Supplier<GLDecoBlocks.TreeSet> set;
        public final int trunkBase;
        public final int trunkRandA;
        public final int trunkRandB;
        public final int foliageRadius;
        public final int foliageOffset;
        public final int foliageHeight;
        public final ResourceKey<ConfiguredFeature<?, ?>> cfKey;

        TreeType(String id,
                 Supplier<GLDecoBlocks.TreeSet> set,
                 int trunkBase, int trunkRandA, int trunkRandB,
                 int foliageRadius, int foliageOffset, int foliageHeight) {
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
                            new BlobFoliagePlacer(ConstantInt.of(foliageRadius), ConstantInt.of(foliageOffset), foliageHeight),
                            new TwoLayersFeatureSize(1, 0, 1)
                    ).build()
            );
        }
    }

}
