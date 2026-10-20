package dev.xkmc.gensokyolegacy.init.data.biome;

import dev.xkmc.gensokyolegacy.content.block.nature.LargeWaterLilyBlock;
import dev.xkmc.gensokyolegacy.content.block.nature.WaterLilyBlock;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures.TreeType;
import dev.xkmc.gensokyolegacy.init.data.biome.biomes.GenbuRavineBiomeGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

/**
 * The handful of custom features that more than one Gensokyo biome places, and nothing else.
 * <p>
 * This is the counterpart to the split, not an exception to it: a feature two biomes lay down is one feature, and
 * splitting it would mean two placed features that happen to be identical. Everything a single biome uses lives in
 * that biome's own class, so editing a biome never reaches this file. One placed feature does split: the ravine
 * places the loose stone through a placed feature of its own, because it lays its floor first and the shared one
 * sits in seven other biomes' tails.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class GensokyoBiomeFeatures {

	/** Loose stone, which seven of the surface biomes scatter on their floor. */
	public static final ResourceKey<PlacedFeature> STONES_PF = AbstractBiomeGen.pf("stones");
	/**
	 * The loose stone's configured feature, which the ravine places through a placed feature of its own.
	 *
	 * @see GenbuRavineBiomeGen
	 */
	public static final ResourceKey<ConfiguredFeature<?, ?>> STONES = AbstractBiomeGen.cf("stones");
	/** Damp moss, which the foothills and the settlement above them both take. */
	public static final ResourceKey<PlacedFeature> FOOTHILL_MOSS_PF = AbstractBiomeGen.pf("foothill_moss");
	/**
	 * Cattails, and the one feature here two biomes reach for in different ways: Misty Lake places it, and the shore
	 * plants the same reeds under a placed feature of its own, because the two are at different densities.
	 */
	public static final ResourceKey<ConfiguredFeature<?, ?>> LAKE_CATTAILS = AbstractBiomeGen.cf("lake_cattails");
	public static final ResourceKey<PlacedFeature> LAKE_CATTAILS_PF = AbstractBiomeGen.pf("lake_cattails");
	/**
	 * The three maples, which the mountain band's three groves plant in place of the vanilla trees they sit on the
	 * ground cover of. The <em>count</em> is left to each biome, which puts it in a placed feature of its own.
	 */
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAPLE_TREES = AbstractBiomeGen.cf("maple_trees");
	/** The carpet of fallen maple leaves that goes with them, and the one litter all three groves take. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAPLE_FALLEN_LEAVES = AbstractBiomeGen.cf("maple_fallen_leaves");
	public static final ResourceKey<PlacedFeature> MAPLE_FALLEN_LEAVES_PF = AbstractBiomeGen.pf("maple_fallen_leaves");
	/**
	 * The mod's own two water lilies, which every Gensokyo lake grows in place of the vanilla lily pad.
	 * <p>
	 * The count is left to each biome, as it is for the loose stone: the lake it grows in decides how many.
	 */
	public static final ResourceKey<ConfiguredFeature<?, ?>> LILIES = AbstractBiomeGen.cf("lilies");

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

		// one tree per placement call, so a biome's own CountPlacement is the whole of its density
		HolderGetter<ConfiguredFeature<?, ?>> trees = ctx.lookup(Registries.CONFIGURED_FEATURE);
		// The selector tests its entries in order and stops at the first that fires, so an entry's share is its own
		// chance times every chance that missed before it. A third then a half is what makes the three come out
		// even: a third, then the two thirds that are left taking half of what is left, and the default the rest.
		FeatureUtils.register(ctx, MAPLE_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
				List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(
						trees.getOrThrow(TreeType.RED_MAPLE.cfKey)), 1.0F / 3),
						new WeightedPlacedFeature(PlacementUtils.inlinePlaced(
								trees.getOrThrow(TreeType.ORANGE_MAPLE.cfKey)), 0.5F)),
				PlacementUtils.inlinePlaced(trees.getOrThrow(TreeType.YELLOW_MAPLE.cfKey))));
		FeatureUtils.register(ctx, MAPLE_FALLEN_LEAVES, Feature.RANDOM_PATCH,
				AbstractBiomeGen.fallenLeaves(40, 6, GLNaturalBlocks.MIXED_MAPLE_FALLEN_LEAVES));
		// The mod's own lilies, in place of the vanilla lily pad: a patch of them on the water, the small pad and
		// the large one in each of its looks.
		FeatureUtils.register(ctx, LILIES, Feature.RANDOM_PATCH, new RandomPatchConfiguration(
				10, 7, 3, PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(lilies()))));
	}

	/**
	 * The mix a Gensokyo lake grows: the small pad, and the large one in each of its looks, so a lake is a field of
	 * pads with the large ones among them and not a field of the same pad.
	 * <p>
	 * The large lily's looks are the block's own — a bare pad, a bloom, a bud, a seed pod — and only a player placing
	 * one picks a look at random. A lake grows them all, so the looks are enumerated here rather than left to the
	 * one the block comes up with.
	 * <p>
	 * The lilies are not symmetric, so every one of them has a facing, and a lake that grows them all one way round
	 * reads as a field of arrows. Each facing is in the mix, and the large one for every three small ones.
	 */
	public static BlockStateProvider lilies() {
		var small = GLNaturalBlocks.SMALL_WATER_LILY.get().defaultBlockState();
		var large = GLNaturalBlocks.LARGE_WATER_LILY.get().defaultBlockState();
		var palette = new AbstractBiomeGen.Palette();
		for (var dir : Direction.Plane.HORIZONTAL) {
			palette.add(small.setValue(WaterLilyBlock.FACING, dir), 3);
		}
		for (var dir : Direction.Plane.HORIZONTAL) {
			for (int i = 0; i < LargeWaterLilyBlock.VARIANTS; i++) {
				palette.add(large.setValue(WaterLilyBlock.FACING, dir).setValue(LargeWaterLilyBlock.VARIANT, i), 1);
			}
		}
		return palette.build();
	}

	/** Called from {@code build}, which owns the single {@link Registries#PLACED_FEATURE} bootstrap. */
	public static void placed(AbstractBiomeGen.Registrar reg) {
		reg.cover(STONES_PF, reg.feature(STONES), CountPlacement.of(2));
		reg.cover(FOOTHILL_MOSS_PF, reg.feature(FOOTHILL_MOSS), CountPlacement.of(4));
		reg.cover(LAKE_CATTAILS_PF, reg.feature(LAKE_CATTAILS), RarityFilter.onAverageOnceEvery(3));
		reg.litter(MAPLE_FALLEN_LEAVES_PF, reg.feature(MAPLE_FALLEN_LEAVES), CountPlacement.of(2));
	}

}