package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

/**
 * The top band of {@code doc/design/gensokyolegacy_fixed_biomes_guide.md}: the summit, the one place the mountain's
 * own rock reaches daylight.
 *
 * @see "doc/design/gensokyo_biomes.md" §7
 */
public class YoukaiMountainSummitBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("youkai_mountain_summit");

	/**
	 * Splits the summit's exposed rock from its scree. Coarse enough that a patch reads as a slope rather than as
	 * speckle, and read per block by the surface rule, so it mottles a cliff face instead of banding it.
	 */
	public static final ResourceKey<NormalNoise.NoiseParameters> SUMMIT_SURFACE =
			ResourceKey.create(Registries.NOISE, GensokyoLegacy.loc("summit_surface"));

	private static final ResourceKey<ConfiguredFeature<?, ?>> SUMMIT_SOIL_DISK = cf("summit_soil_disk");
	private static final ResourceKey<PlacedFeature> SUMMIT_SOIL_PF = pf("summit_soil");
	private static final ResourceKey<ConfiguredFeature<?, ?>> SUMMIT_MOSS = cf("summit_moss");
	private static final ResourceKey<PlacedFeature> SUMMIT_MOSS_PF = pf("summit_moss");
	private static final ResourceKey<ConfiguredFeature<?, ?>> SUMMIT_SCREE = cf("summit_scree");
	private static final ResourceKey<PlacedFeature> SUMMIT_SCREE_PF = pf("summit_scree");
	private static final ResourceKey<PlacedFeature> SUMMIT_TREES_PF = pf("summit_trees");

	public YoukaiMountainSummitBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.4f, 0.3f, 0x3f6f7d, 0xa8b4c4, 0x8a9a72, 0x7a8a64, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, EMERALD))
						.underground(INFESTED)
						.springs(SPRINGS)
						.vegetation(join(List.of(CavePlacements.GLOW_LICHEN),
								List.of(SUMMIT_SOIL_PF, SUMMIT_MOSS_PF, SUMMIT_SCREE_PF, SUMMIT_TREES_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, SUMMIT_MOSS, Feature.RANDOM_PATCH, onStone(20, new Palette()
				.add(Blocks.MOSS_CARPET, 3).add(Blocks.GLOW_LICHEN, 2).build()));
		FeatureUtils.register(ctx, SUMMIT_SCREE, Feature.RANDOM_PATCH, onStone(18, new Palette()
				.add(Blocks.GRAVEL, 3).add(Blocks.ANDESITE, 2).add(Blocks.DIORITE, 1).add(Blocks.COBBLESTONE, 1).build()));
		// halfHeight 0: only the top block of the column turns to grass, so a soil pocket has dirt under it rather
		// than a floating slab. This is the summit's "gentle ledges and soil pockets", since the surface rule leaves
		// the pockets to it rather than spending a second noise on grass
		FeatureUtils.register(ctx, SUMMIT_SOIL_DISK, Feature.DISK, new DiskConfiguration(
				RuleBasedBlockStateProvider.simple(Blocks.GRASS_BLOCK),
				BlockPredicate.matchesTag(BlockTags.BASE_STONE_OVERWORLD), UniformInt.of(2, 4), 0));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.cover(SUMMIT_SOIL_PF, reg.feature(SUMMIT_SOIL_DISK), CountPlacement.of(2));
		reg.cover(SUMMIT_MOSS_PF, reg.feature(SUMMIT_MOSS), CountPlacement.of(4));
		reg.cover(SUMMIT_SCREE_PF, reg.feature(SUMMIT_SCREE), CountPlacement.of(4));
		reg.cover(SUMMIT_TREES_PF, reg.feature(TreeFeatures.PINE), RarityFilter.onAverageOnceEvery(16));
	}

	@Override
	protected void noise(BootstrapContext<NormalNoise.NoiseParameters> ctx) {
		ctx.register(SUMMIT_SURFACE, new NormalNoise.NoiseParameters(-5, 1.0, 1.0));
	}

}