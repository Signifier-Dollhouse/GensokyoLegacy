package dev.xkmc.gensokyolegacy.init.data.biome;

import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

/**
 * The handful of custom features that more than one Gensokyo biome places, and nothing else.
 * <p>
 * This is the counterpart to the split, not an exception to it: a feature two biomes lay down is one feature, and
 * splitting it would mean two placed features that happen to be identical. Everything a single biome uses lives in
 * that biome's own class, so editing a biome never reaches this file.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class GensokyoBiomeFeatures {

	/** Loose stone, which eight of the surface biomes scatter on their floor. */
	public static final ResourceKey<PlacedFeature> STONES_PF = AbstractBiomeGen.pf("stones");
	/** Damp moss, which the foothills and the settlement above them both take. */
	public static final ResourceKey<PlacedFeature> FOOTHILL_MOSS_PF = AbstractBiomeGen.pf("foothill_moss");
	/**
	 * Cattails, and the one feature here two biomes reach for in different ways: Misty Lake places it, and the shore
	 * plants the same reeds under a placed feature of its own, because the two are at different densities.
	 */
	public static final ResourceKey<ConfiguredFeature<?, ?>> LAKE_CATTAILS = AbstractBiomeGen.cf("lake_cattails");
	public static final ResourceKey<PlacedFeature> LAKE_CATTAILS_PF = AbstractBiomeGen.pf("lake_cattails");

	private static final ResourceKey<ConfiguredFeature<?, ?>> STONES = AbstractBiomeGen.cf("stones");
	private static final ResourceKey<ConfiguredFeature<?, ?>> FOOTHILL_MOSS = AbstractBiomeGen.cf("foothill_moss");

	/** Called from {@code build}, which owns the single {@link Registries#CONFIGURED_FEATURE} bootstrap. */
	public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, STONES, Feature.RANDOM_PATCH, AbstractBiomeGen.onDirt(18, new AbstractBiomeGen.Palette()
				.add(Blocks.MOSSY_COBBLESTONE, 3).add(Blocks.COBBLESTONE, 2).add(Blocks.STONE_BRICKS, 1).build()));
		FeatureUtils.register(ctx, FOOTHILL_MOSS, Feature.RANDOM_PATCH, AbstractBiomeGen.onDirt(28, new AbstractBiomeGen.Palette()
				.add(Blocks.MOSS_CARPET, 2).add(Blocks.FERN, 2)
				.add(GLNaturalBlocks.BRACKEN, 2).add(Blocks.SHORT_GRASS, 2).build()));
		BlockState cattail = GLNaturalBlocks.FLAME_CATTAIL.get().defaultBlockState();
		// the lakebed is sand and gravel under the surface rule, so these sit on those and not on BlockTags.DIRT
		FeatureUtils.register(ctx, LAKE_CATTAILS, Feature.RANDOM_PATCH, AbstractBiomeGen.onGround(10, new AbstractBiomeGen.Palette()
				.add(cattail, 3).add(Blocks.SUGAR_CANE, 1).build(),
				Blocks.SAND, Blocks.GRAVEL, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.CLAY, Blocks.MUD));
	}

	/** Called from {@code build}, which owns the single {@link Registries#PLACED_FEATURE} bootstrap. */
	public static void placed(AbstractBiomeGen.Registrar reg) {
		reg.cover(STONES_PF, reg.feature(STONES), CountPlacement.of(2));
		reg.cover(FOOTHILL_MOSS_PF, reg.feature(FOOTHILL_MOSS), CountPlacement.of(4));
		reg.cover(LAKE_CATTAILS_PF, reg.feature(LAKE_CATTAILS), RarityFilter.onAverageOnceEvery(3));
	}

}