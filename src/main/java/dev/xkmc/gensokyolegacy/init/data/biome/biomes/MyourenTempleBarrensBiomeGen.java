package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.OrePlacements;
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
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

/**
 * The cold dry cell of the lower ground in {@code doc/design/gensokyo_dimension_biome_guide.md}: the scrubby barrens
 * the temple sits in, coarse dirt underfoot and a little extra gold in it.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class MyourenTempleBarrensBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("myouren_temple_barrens");

	private static final ResourceKey<ConfiguredFeature<?, ?>> COARSE_DIRT_DISK = cf("barrens_coarse_dirt_disk");
	private static final ResourceKey<PlacedFeature> COARSE_DIRT_PF = pf("barrens_coarse_dirt");
	private static final ResourceKey<ConfiguredFeature<?, ?>> BARRENS_SCRUB = cf("barrens_scrub");
	private static final ResourceKey<PlacedFeature> BARRENS_SCRUB_PF = pf("barrens_scrub");

	public MyourenTempleBarrensBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(false, 0.2f, 0.2f, 0x6b5a35, 0xc0a878, 0x9a8f5c, 0x8a7f50, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, List.of(OrePlacements.ORE_GOLD_EXTRA), OVERWORLD_DISKS,
								List.of(COARSE_DIRT_PF)))
						.springs(SPRINGS)
						.vegetation(join(BADLANDS_GROUND,
								List.of(GensokyoBiomeFeatures.STONES_PF, BARRENS_SCRUB_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, COARSE_DIRT_DISK, Feature.DISK, new DiskConfiguration(
				RuleBasedBlockStateProvider.simple(Blocks.COARSE_DIRT),
				BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK), UniformInt.of(2, 4), 1));
		FeatureUtils.register(ctx, BARRENS_SCRUB, Feature.RANDOM_PATCH, onDirt(28, new Palette()
				.add(Blocks.DEAD_BUSH, 3).add(Blocks.SHORT_GRASS, 3)
				.add(Blocks.COARSE_DIRT, 2).add(Blocks.GRAVEL, 1).build()));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.cover(COARSE_DIRT_PF, reg.feature(COARSE_DIRT_DISK), CountPlacement.of(4));
		reg.cover(BARRENS_SCRUB_PF, reg.feature(BARRENS_SCRUB), CountPlacement.of(3));
	}

}