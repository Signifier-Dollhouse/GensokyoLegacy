package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures.TreeType;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.lake.LakeConfig;
import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLFurniture;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

/**
 * The cold dry cell of the lower ground in {@code doc/design/gensokyo_dimension_biome_guide.md}: the scrubby barrens
 * the temple sits in, coarse dirt underfoot and a little extra gold in it.
 * <p>
 * What the badlands base has no room for, it takes back itself: six locust a chunk over the dry waste, a row of the
 * four jizo in every chunk, and a small wasteland gravestone every third. A temple does not leave its grounds tidy.
 * <p>
 * The pools are the one thing the dry waste has that the badlands base does not: a temple keeps its water, and the
 * lilies in it are the mod's own.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class MyourenTempleBarrensBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("myouren_temple_barrens");

	private static final ResourceKey<ConfiguredFeature<?, ?>> COARSE_DIRT_DISK = cf("barrens_coarse_dirt_disk");
	private static final ResourceKey<PlacedFeature> COARSE_DIRT_PF = pf("barrens_coarse_dirt");
	private static final ResourceKey<ConfiguredFeature<?, ?>> BARRENS_SCRUB = cf("barrens_scrub");
	private static final ResourceKey<PlacedFeature> BARRENS_SCRUB_PF = pf("barrens_scrub");
	private static final ResourceKey<ConfiguredFeature<?, ?>> GRAVESTONE = cf("barrens_gravestone");
	private static final ResourceKey<PlacedFeature> GRAVESTONE_PF = pf("barrens_gravestone");
	private static final ResourceKey<ConfiguredFeature<?, ?>> JIZO = cf("barrens_jizo");
	private static final ResourceKey<PlacedFeature> JIZO_PF = pf("barrens_jizo");
	private static final ResourceKey<PlacedFeature> LOCUST_TREES_PF = pf("barrens_locust_trees");

	/**
	 * The barrens' own pools, dug into the waste the way the magical forest's pond is dug into its floor.
	 * <p>
	 * A dry place with standing water in it is odd, and that is the point: a temple's grounds keep their water, and
	 * the lilies in it are the mod's own.
	 */
	private static final ResourceKey<ConfiguredFeature<?, ?>> LAKE = cf("barrens_lake");
	private static final ResourceKey<PlacedFeature> LAKE_PF = pf("barrens_lake");

	public MyourenTempleBarrensBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(false, 0.2f, 0.2f, 0x6b5a35, 0xc0a878, 0x9a8f5c, 0x8a7f50, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(join(LAVA_LAKES, List.of(LAKE_PF)))
						.ores(join(OVERWORLD_ORES, List.of(OrePlacements.ORE_GOLD_EXTRA), OVERWORLD_DISKS,
								List.of(COARSE_DIRT_PF)))
						.springs(SPRINGS)
						.vegetation(join(BADLANDS_GROUND, List.of(LOCUST_TREES_PF, GensokyoBiomeFeatures.STONES_PF,
								BARRENS_SCRUB_PF, JIZO_PF, GRAVESTONE_PF)))
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
		// one marker standing up, left out here as it is on the valley floor
		FeatureUtils.register(ctx, GRAVESTONE, Feature.RANDOM_PATCH, patch(8, 7, 0,
				new Palette().add(GLFurniture.WASTELAND_GRAVESTONE_SMALL, 1)
						.add(GLFurniture.STONE_LANTERN, 1)
						.build(), GLWorldGen.DOUBLE_BLOCK.get()));
		// all four colourways in one patch, so a clump of them is a row of a village rather than four of one kind
		FeatureUtils.register(ctx, JIZO, Feature.RANDOM_PATCH, patch(16, 6, 0, new Palette()
				.add(GLFurniture.JIZO, 1).add(GLFurniture.JIZO_MYOUREN, 1).add(GLFurniture.JIZO_NARUMI, 1)
				.add(GLFurniture.JIZO_STRAW_HAT, 1).build(), GLWorldGen.DOUBLE_BLOCK.get()));
		// the pool: a small one dug into the waste, with the mod's lilies on it
		FeatureUtils.register(ctx, LAKE, GLWorldGen.LAKE.get(), new LakeConfig(
				LakeConfig.Mode.SURFACE, 0,
				new LakeConfig.Shape(3, 18, 6, 2, 4, 6, 2),
				new LakeConfig.Material(Blocks.WATER.defaultBlockState(), Blocks.CLAY.defaultBlockState(),
						Blocks.COARSE_DIRT.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState()),
				List.of(new LakeConfig.Deco(LakeConfig.Target.FLOATING,
						GensokyoBiomeFeatures.lilies(), Blocks.AIR.defaultBlockState(), 0.15f, BlockTags.DIRT))));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.cover(COARSE_DIRT_PF, reg.feature(COARSE_DIRT_DISK), CountPlacement.of(4));
		reg.trees(LOCUST_TREES_PF, reg.feature(TreeType.LOCUST.cfKey), CountPlacement.of(2));
		reg.cover(BARRENS_SCRUB_PF, reg.feature(BARRENS_SCRUB), CountPlacement.of(3));
		reg.floor(JIZO_PF, reg.feature(JIZO), CountPlacement.of(1));
		reg.floor(GRAVESTONE_PF, reg.feature(GRAVESTONE), CountPlacement.of(1));
		// the pool, dug into the waste: a rarer pond than the magical forest's, because the barrens is a wide dry place
		reg.placed(LAKE_PF, reg.feature(LAKE), RarityFilter.onAverageOnceEvery(16), InSquarePlacement.spread(),
				HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
				SurfaceWaterDepthFilter.forMaxDepth(0), BiomeFilter.biome());
	}

}