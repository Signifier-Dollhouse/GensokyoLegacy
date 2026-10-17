package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;

import java.util.List;

/**
 * The cold dry cell of the mountain band in {@code doc/design/gensokyo_dimension_biome_guide.md}: the sanctuary,
 * a cherry grove in forest ground.
 * <p>
 * Forest rather than meadow: the mountain's own grove wants birch and oak under it and the hill's open flower meadow
 * beside it, and the cherry tail lands on a canopy instead of on bare grass.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class SanctuaryBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("sanctuary");

	private static final ResourceKey<PlacedFeature> SANCTUARY_TREES_PF = pf("sanctuary_trees");
	private static final ResourceKey<ConfiguredFeature<?, ?>> SANCTUARY_FLOWERS = cf("sanctuary_flowers");
	private static final ResourceKey<PlacedFeature> SANCTUARY_FLOWERS_PF = pf("sanctuary_flowers");

	public SanctuaryBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.35f, 0.4f, 0x57a0a6, 0xa8c4d8, 0x8fc06a, 0x82b45e, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, EMERALD))
						.underground(INFESTED)
						.springs(SPRINGS)
						.vegetation(join(FOREST_GROUND, List.of(SANCTUARY_TREES_PF, SANCTUARY_FLOWERS_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, SANCTUARY_FLOWERS, Feature.RANDOM_PATCH, onDirt(28, new Palette()
				.add(Blocks.LILY_OF_THE_VALLEY, 3).add(Blocks.OXEYE_DAISY, 3).add(Blocks.WHITE_TULIP, 2)
				.add(GLNaturalBlocks.STAR_FLOWER, 2).add(Blocks.ALLIUM, 1).build()));
	}

	@Override
	protected void placed(Registrar reg) {
		// the only tree in the tail, and the only one that needs the dry-water check vanilla's own trees carry
		reg.placed(SANCTUARY_TREES_PF, reg.feature(TreeFeatures.CHERRY),
				RarityFilter.onAverageOnceEvery(5), InSquarePlacement.spread(),
				SurfaceWaterDepthFilter.forMaxDepth(0),
				PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
		reg.cover(SANCTUARY_FLOWERS_PF, reg.feature(SANCTUARY_FLOWERS), CountPlacement.of(3));
	}

}