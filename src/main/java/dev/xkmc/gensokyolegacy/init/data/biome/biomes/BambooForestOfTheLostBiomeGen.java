package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

/**
 * The cold humid cell of the upper ground in {@code doc/design/gensokyo_dimension_biome_guide.md}: the bamboo grove,
 * mud underfoot.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class BambooForestOfTheLostBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("bamboo_forest_of_the_lost");

	private static final ResourceKey<ConfiguredFeature<?, ?>> MUD_DISK = cf("bamboo_forest_mud_disk");
	private static final ResourceKey<PlacedFeature> MUD_PF = pf("bamboo_forest_mud");
	private static final ResourceKey<PlacedFeature> BAMBOO_DENSE_PF = pf("bamboo_dense");
	private static final ResourceKey<PlacedFeature> BAMBOO_SPARSE_PF = pf("bamboo_sparse");

	public BambooForestOfTheLostBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.3f, 0.9f, 0x3f6f6a, 0x7d94a0, 0x6f8f52, 0x5f7f4a, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, List.of(MUD_PF)))
						.springs(SPRINGS)
						.vegetation(join(TAIGA_GROUND, List.of(VegetationPlacements.PATCH_BERRY_RARE),
								List.of(BAMBOO_DENSE_PF, BAMBOO_SPARSE_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, MUD_DISK, Feature.DISK, new DiskConfiguration(
				RuleBasedBlockStateProvider.simple(Blocks.MUD),
				BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK, Blocks.DIRT), UniformInt.of(2, 5), 1));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.cover(MUD_PF, reg.feature(MUD_DISK), CountPlacement.of(3));
		// two densities, not one: a noise count clumps the stalks into groves, and the rare light pass fills the gaps
		// between them. One count on its own reads as scattered poles rather than a bamboo forest.
		reg.cover(BAMBOO_DENSE_PF, reg.feature(VegetationFeatures.BAMBOO_SOME_PODZOL),
				NoiseBasedCountPlacement.of(40, 24.0F, 0.3F));
		reg.cover(BAMBOO_SPARSE_PF, reg.feature(VegetationFeatures.BAMBOO_NO_PODZOL),
				RarityFilter.onAverageOnceEvery(4));
	}

}