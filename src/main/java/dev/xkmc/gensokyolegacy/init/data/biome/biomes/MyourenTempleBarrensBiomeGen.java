package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures.TreeType;
import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLFurniture;
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
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

/**
 * The cold dry cell of the lower ground in {@code doc/design/gensokyo_dimension_biome_guide.md}: the scrubby barrens
 * the temple sits in, coarse dirt underfoot and a little extra gold in it.
 * <p>
 * What the badlands base has no room for, it takes back itself: six locust a chunk over the dry waste, a clump of
 * the four jizo every third chunk, and a small wasteland gravestone every sixth. A temple does not leave its grounds
 * tidy.
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
		FeatureUtils.register(ctx, GRAVESTONE, Feature.RANDOM_PATCH, patch(1, 7, 0,
				new Palette().add(GLFurniture.WASTELAND_GRAVESTONE_SMALL, 1).build(), GLWorldGen.DOUBLE_BLOCK.get()));
		// all four colourways in one patch, so a clump of them is a row of a village rather than four of one kind
		FeatureUtils.register(ctx, JIZO, Feature.RANDOM_PATCH, patch(8, 6, 0, new Palette()
				.add(GLFurniture.JIZO, 1).add(GLFurniture.JIZO_MYOUREN, 1).add(GLFurniture.JIZO_NARUMI, 1)
				.add(GLFurniture.JIZO_STRAW_HAT, 1).build(), GLWorldGen.DOUBLE_BLOCK.get()));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.cover(COARSE_DIRT_PF, reg.feature(COARSE_DIRT_DISK), CountPlacement.of(4));
		reg.trees(LOCUST_TREES_PF, reg.feature(TreeType.LOCUST.cfKey), CountPlacement.of(6));
		reg.cover(BARRENS_SCRUB_PF, reg.feature(BARRENS_SCRUB), CountPlacement.of(3));
		reg.cover(JIZO_PF, reg.feature(JIZO), RarityFilter.onAverageOnceEvery(3));
		reg.cover(GRAVESTONE_PF, reg.feature(GRAVESTONE), RarityFilter.onAverageOnceEvery(6));
	}

}