package dev.xkmc.gensokyolegacy.init.data.biome;

import dev.xkmc.gensokyolegacy.content.block.nature.CedarFallenLeavesBlock;
import dev.xkmc.gensokyolegacy.content.block.nature.WaterloggedBushBlock;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.lake.MagicalForestLakeFeature;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.template.TemplateFeatureConfig;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.AvoidStructuresFilter;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.CanopyFilter;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.GridExclusionFilter;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.GridExclusionFilter.Entry;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.GridLayer;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.JitteredGridPlacement;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.NoiseBandFilter;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SmallDripleafBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.ArrayList;
import java.util.List;

/**
 * Vegetation of the magical forest, built from hand-made templates (see
 * doc/design/magical_forest_vegetation.md). Four canopy layers placed top-down, then ground cover:
 * giant trees, large trees, medium trees and huge mushrooms, bushes and small mushrooms.
 * Ponds ({@code magical_forest/lake}) run ahead of all that in the LAKES step.
 * <p>
 * Giant and large trees sit on seed-pure jittered grids, lower layers keep their distance from
 * those grids. A low frequency noise splits the biome into forest types and a mid frequency
 * noise opens clearings:
 * <pre>
 * FOREST_TYPE &lt; GLADE          mushroom glade: huge mushrooms on a grid, few giants, no large trees
 * GLADE .. OLD_GROWTH           mixed forest: oaks and blue firs
 * FOREST_TYPE &gt;= OLD_GROWTH    blue fir old growth: firs only, denser large trees
 * CLEARING &gt;= CLEARING_MIN     clearing: no tree above bush size
 * </pre>
 * Thresholds are quantiles measured on NormalNoise: about 17% glade, 25% old growth, 7% clearing.
 * Grids, spacings and counts were tuned with tools/template_export/simulate_layout.py, which
 * mirrors the constants below: change both together.
 */
public class MagicalForestFeatures {

	public static final ResourceKey<NormalNoise.NoiseParameters> FOREST_TYPE = ResourceKey.create(Registries.NOISE, GensokyoLegacy.loc("magical_forest_type"));
	public static final ResourceKey<NormalNoise.NoiseParameters> CLEARING = ResourceKey.create(Registries.NOISE, GensokyoLegacy.loc("magical_forest_clearing"));

	private static final double GLADE = -0.28, OLD_GROWTH = 0.2, CLEARING_MIN = 0.48;

	// layers sharing cell, margin and salt are nested: the sparse one is a subset of the dense one.
	// ~78% canopy cover outside glades, giant trunks >= 10 apart (mean 22)
	private static final GridLayer GIANT = new GridLayer(28, 5, 7310001, 0.85f);
	private static final GridLayer GIANT_SPARSE = new GridLayer(28, 5, 7310001, 0.4f);
	private static final GridLayer LARGE = new GridLayer(18, 3, 7310002, 0.8f);
	private static final GridLayer LARGE_DENSE = new GridLayer(18, 3, 7310002, 0.95f);
	private static final GridLayer GLADE_MUSHROOM = new GridLayer(12, 2, 7310003, 0.7f);

	// XZ distance a lower layer keeps from the grid points (trunks) of the layers above it
	private static final GridExclusionFilter LARGE_SPACING = spacing(new Entry(GIANT, 12));
	private static final GridExclusionFilter MEDIUM_SPACING = spacing(new Entry(GIANT, 9), new Entry(LARGE_DENSE, 8));
	private static final GridExclusionFilter MUSHROOM_SPACING = spacing(new Entry(GIANT, 7), new Entry(LARGE_DENSE, 5));
	private static final GridExclusionFilter UNDERSTORY_SPACING = spacing(new Entry(GIANT, 5), new Entry(LARGE_DENSE, 4));

	// blocks kept free around the listed buildings, by canopy size
	private static final int GIANT_CLEARANCE = 13, LARGE_CLEARANCE = 11, MEDIUM_CLEARANCE = 6, SMALL_CLEARANCE = 4, GROUND_CLEARANCE = 2;
	private static final int LAKE_CLEARANCE = 11;
	private static final List<String> STRUCTURES = List.of("marisa_house", "morichika_shop", "hakurei_shrine");

	/**
	 * Terrain tolerance of a template size: sample radius, max height difference, root extension.
	 */
	private record Footprint(int radius, int maxSlope, int rootDepth) {
	}

	private static final Footprint GIANT_FOOTPRINT = new Footprint(4, 4, 6);
	private static final Footprint LARGE_FOOTPRINT = new Footprint(3, 4, 5);
	private static final Footprint MEDIUM_FOOTPRINT = new Footprint(2, 3, 4);
	private static final Footprint SMALL_FOOTPRINT = new Footprint(1, 2, 3);
	private static final Footprint BUSH_FOOTPRINT = new Footprint(1, 2, 2);
	private static final Footprint TINY_FOOTPRINT = new Footprint(0, 0, 2);

	// configured features
	private static final ResourceKey<ConfiguredFeature<?, ?>> OAK_GIANT = cf("oak_giant");
	// blue fir template trees, also reused for sapling growth (1 / 2x2 / 3x3 saplings)
	public static final ResourceKey<ConfiguredFeature<?, ?>> BLUE_FIR_GIANT = cf("blue_fir_giant");
	private static final ResourceKey<ConfiguredFeature<?, ?>> GIANT_MIXED = cf("giant_tree_mixed");
	private static final ResourceKey<ConfiguredFeature<?, ?>> OAK_LARGE = cf("oak_large");
	public static final ResourceKey<ConfiguredFeature<?, ?>> BLUE_FIR_LARGE = cf("blue_fir_large");
	private static final ResourceKey<ConfiguredFeature<?, ?>> LARGE_MIXED = cf("large_tree_mixed");
	public static final ResourceKey<ConfiguredFeature<?, ?>> BLUE_FIR_MEDIUM = cf("blue_fir_medium");
	private static final ResourceKey<ConfiguredFeature<?, ?>> AZALEA_BUSH = cf("azalea_bush");
	// per-type template trees for bonemeal growth (small / medium / large per type)
	public static final ResourceKey<ConfiguredFeature<?, ?>> GHOST_FIRE_SMALL = cf("ghost_fire_small");
	public static final ResourceKey<ConfiguredFeature<?, ?>> GHOST_FIRE_MEDIUM = cf("ghost_fire_medium");
	public static final ResourceKey<ConfiguredFeature<?, ?>> GHOST_FIRE_LARGE = cf("ghost_fire_large");
	public static final ResourceKey<ConfiguredFeature<?, ?>> DREAM_SMALL = cf("dream_small");
	public static final ResourceKey<ConfiguredFeature<?, ?>> DREAM_MEDIUM = cf("dream_medium");
	public static final ResourceKey<ConfiguredFeature<?, ?>> DREAM_LARGE = cf("dream_large");
	public static final ResourceKey<ConfiguredFeature<?, ?>> DEMONIC_MIASMA_SMALL = cf("demonic_miasma_small");
	public static final ResourceKey<ConfiguredFeature<?, ?>> DEMONIC_MIASMA_MEDIUM = cf("demonic_miasma_medium");
	public static final ResourceKey<ConfiguredFeature<?, ?>> DEMONIC_MIASMA_LARGE = cf("demonic_miasma_large");
	private static final ResourceKey<ConfiguredFeature<?, ?>> MUSHROOM_LARGE = cf("mushroom_large");
	private static final ResourceKey<ConfiguredFeature<?, ?>> MUSHROOM_MEDIUM_TEMPLATE = cf("mushroom_medium_template");
	private static final ResourceKey<ConfiguredFeature<?, ?>> MUSHROOM_MEDIUM = cf("mushroom_medium");
	private static final ResourceKey<ConfiguredFeature<?, ?>> MUSHROOM_SMALL = cf("mushroom_small");
	private static final ResourceKey<ConfiguredFeature<?, ?>> MUSHROOM_SMALL_CLUSTER = cf("mushroom_small_cluster");
	private static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_LEAVES = cf("fallen_leaves");
	private static final ResourceKey<ConfiguredFeature<?, ?>> MOSS_CARPET = cf("moss_carpet");
	private static final ResourceKey<ConfiguredFeature<?, ?>> LAKE = cf("lake");

	// placed features
	private static final ResourceKey<PlacedFeature> LAKE_PF = pf("lake");
	private static final ResourceKey<PlacedFeature> GIANT_MIXED_PF = pf("giant_tree_mixed");
	private static final ResourceKey<PlacedFeature> GIANT_FIR_PF = pf("giant_tree_old_growth");
	private static final ResourceKey<PlacedFeature> GIANT_GLADE_PF = pf("giant_tree_glade");
	private static final ResourceKey<PlacedFeature> LARGE_MIXED_PF = pf("large_tree_mixed");
	private static final ResourceKey<PlacedFeature> LARGE_FIR_PF = pf("large_tree_old_growth");
	private static final ResourceKey<PlacedFeature> MUSHROOM_LARGE_GLADE_PF = pf("mushroom_large_glade");
	private static final ResourceKey<PlacedFeature> MUSHROOM_LARGE_PF = pf("mushroom_large");
	private static final ResourceKey<PlacedFeature> MEDIUM_TREE_PF = pf("medium_tree");
	private static final ResourceKey<PlacedFeature> MUSHROOM_MEDIUM_GLADE_PF = pf("mushroom_medium_glade");
	private static final ResourceKey<PlacedFeature> MUSHROOM_MEDIUM_PF = pf("mushroom_medium");
	private static final ResourceKey<PlacedFeature> BUSH_PF = pf("bush");
	private static final ResourceKey<PlacedFeature> MUSHROOM_SMALL_GLADE_PF = pf("mushroom_small_glade");
	private static final ResourceKey<PlacedFeature> MUSHROOM_SMALL_PF = pf("mushroom_small");
	private static final ResourceKey<PlacedFeature> FALLEN_LEAVES_PF = pf("fallen_leaves");
	private static final ResourceKey<PlacedFeature> MOSS_CARPET_PF = pf("moss_carpet");
	private static final ResourceKey<PlacedFeature> GRASS_PF = pf("grass");
	private static final ResourceKey<PlacedFeature> GRASS_FOREST_PF = pf("grass_forest");
	private static final ResourceKey<PlacedFeature> GRASS_TAIGA_PF = pf("grass_taiga");
	private static final ResourceKey<PlacedFeature> LARGE_FERN_PF = pf("large_fern");
	private static final ResourceKey<PlacedFeature> FLOWERS_PF = pf("flowers");
	private static final ResourceKey<PlacedFeature> MUSHROOMS_PF = pf("mushrooms");

	/**
	 * Generation order: canopy layers top-down, then ground cover.
	 */
	private static final List<ResourceKey<PlacedFeature>> ORDER = List.of(
			GIANT_MIXED_PF, GIANT_FIR_PF, GIANT_GLADE_PF,
			LARGE_MIXED_PF, LARGE_FIR_PF,
			MUSHROOM_LARGE_GLADE_PF, MUSHROOM_LARGE_PF,
			MEDIUM_TREE_PF, MUSHROOM_MEDIUM_GLADE_PF, MUSHROOM_MEDIUM_PF,
			BUSH_PF, MUSHROOM_SMALL_GLADE_PF, MUSHROOM_SMALL_PF,
			FALLEN_LEAVES_PF, MOSS_CARPET_PF, GRASS_PF, GRASS_FOREST_PF, GRASS_TAIGA_PF, LARGE_FERN_PF, FLOWERS_PF, MUSHROOMS_PF
	);

	public static void addVegetation(BiomeGenerationSettings.Builder builder) {
		for (var key : ORDER) {
			builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, key);
		}
	}

	/**
	 * Ponds carved in the LAKES step, before vegetation: {@code TemplateFeature} rejects wet
	 * ground, so giant and large trees keep off the water on their own and never intersect a lake.
	 */
	public static void addLake(BiomeGenerationSettings.Builder builder) {
		builder.addFeature(GenerationStep.Decoration.LAKES, LAKE_PF);
	}

	public static void noise(BootstrapContext<NormalNoise.NoiseParameters> ctx) {
		ctx.register(FOREST_TYPE, new NormalNoise.NoiseParameters(-7, 1.0, 0.5));
		ctx.register(CLEARING, new NormalNoise.NoiseParameters(-5, 1.0));
	}

	public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
		var template = GLWorldGen.TEMPLATE.get();

		// ground patches around big trunks, placed by the template feature at the trunk base
		var fallenLeaves = fallenLeavesPatch(64, 7);
		FeatureUtils.register(ctx, FALLEN_LEAVES, Feature.RANDOM_PATCH, fallenLeavesPatch(40, 6));
		FeatureUtils.register(ctx, MOSS_CARPET, Feature.RANDOM_PATCH, groundPatch(24, 4,
				BlockStateProvider.simple(Blocks.MOSS_CARPET)));
		var oakFloor = List.of(
				floorDisk(new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
						.add(Blocks.PODZOL.defaultBlockState(), 3)
						.add(Blocks.COARSE_DIRT.defaultBlockState(), 2)
						.add(Blocks.ROOTED_DIRT.defaultBlockState(), 2)
						.add(Blocks.MOSS_BLOCK.defaultBlockState(), 1)
						.build()), 3, 5),
				PlacementUtils.inlinePlaced(cf.getOrThrow(MOSS_CARPET)));
		var firFloor = List.of(
				floorDisk(new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
						.add(Blocks.PODZOL.defaultBlockState(), 4)
						.add(Blocks.COARSE_DIRT.defaultBlockState(), 1)
						.add(Blocks.ROOTED_DIRT.defaultBlockState(), 1)
						.build()), 3, 5),
				PlacementUtils.inlinePlaced(Feature.RANDOM_PATCH, fallenLeaves));
		var mushroomFloor = List.of(floorDisk(BlockStateProvider.simple(Blocks.MYCELIUM), 2, 3));

		FeatureUtils.register(ctx, OAK_GIANT, template, vegetation(GIANT_FOOTPRINT, oakFloor,
				"tree/oak_giant_1", "tree/oak_giant_2", "tree/oak_giant_3", "tree/oak_giant_4"));
		FeatureUtils.register(ctx, BLUE_FIR_GIANT, template, vegetation(GIANT_FOOTPRINT, firFloor,
				"tree/blue_fir_giant_1", "tree/blue_fir_giant_2"));
		FeatureUtils.register(ctx, OAK_LARGE, template, vegetation(LARGE_FOOTPRINT, oakFloor,
				"tree/oak_large_1"));
		FeatureUtils.register(ctx, BLUE_FIR_LARGE, template, vegetation(LARGE_FOOTPRINT, firFloor,
				"tree/blue_fir_large_1", "tree/blue_fir_large_2"));
		FeatureUtils.register(ctx, BLUE_FIR_MEDIUM, template, vegetation(MEDIUM_FOOTPRINT, List.of(),
				"tree/blue_fir_medium_1", "tree/blue_fir_medium_2"));
		FeatureUtils.register(ctx, AZALEA_BUSH, template, vegetation(BUSH_FOOTPRINT, List.of(),
				"bush/azalea_1", "bush/azalea_2", "bush/azalea_3"));
		FeatureUtils.register(ctx, MUSHROOM_LARGE, template, vegetation(MEDIUM_FOOTPRINT, mushroomFloor,
				"mushroom/ghost_fire_large_1", "mushroom/ghost_fire_large_2", "mushroom/ghost_fire_large_3",
				"mushroom/demonic_miasma_large_1", "mushroom/demonic_miasma_large_2", "mushroom/dream_large_1"));
		FeatureUtils.register(ctx, MUSHROOM_MEDIUM_TEMPLATE, template, vegetation(SMALL_FOOTPRINT, mushroomFloor,
				"mushroom/ghost_fire_medium_1", "mushroom/ghost_fire_medium_2",
				"mushroom/demonic_miasma_medium_1", "mushroom/demonic_miasma_medium_2",
				"mushroom/dream_medium_1", "mushroom/dream_medium_2"));
		FeatureUtils.register(ctx, MUSHROOM_SMALL, template, vegetation(TINY_FOOTPRINT, List.of(),
				"mushroom/ghost_fire_small_1", "mushroom/ghost_fire_small_2",
				"mushroom/demonic_miasma_small_1", "mushroom/demonic_miasma_small_2",
				"mushroom/dream_small_1", "mushroom/dream_small_2", "mushroom/dream_small_3",
				"mushroom/dream_small_4", "mushroom/dream_small_5"));

		// growth-only variants split by type so a mushroom always grows into its own kind;
		// no floor: bonemeal growth should not repaint the surrounding terrain
		FeatureUtils.register(ctx, GHOST_FIRE_SMALL, template, vegetation(TINY_FOOTPRINT, List.of(),
				"mushroom/ghost_fire_small_1", "mushroom/ghost_fire_small_2"));
		FeatureUtils.register(ctx, GHOST_FIRE_MEDIUM, template, vegetation(SMALL_FOOTPRINT, List.of(),
				"mushroom/ghost_fire_medium_1", "mushroom/ghost_fire_medium_2"));
		FeatureUtils.register(ctx, GHOST_FIRE_LARGE, template, vegetation(MEDIUM_FOOTPRINT, List.of(),
				"mushroom/ghost_fire_large_1", "mushroom/ghost_fire_large_2", "mushroom/ghost_fire_large_3"));
		FeatureUtils.register(ctx, DREAM_SMALL, template, vegetation(TINY_FOOTPRINT, List.of(),
				"mushroom/dream_small_1", "mushroom/dream_small_2", "mushroom/dream_small_3",
				"mushroom/dream_small_4", "mushroom/dream_small_5"));
		FeatureUtils.register(ctx, DREAM_MEDIUM, template, vegetation(SMALL_FOOTPRINT, List.of(),
				"mushroom/dream_medium_1", "mushroom/dream_medium_2"));
		FeatureUtils.register(ctx, DREAM_LARGE, template, vegetation(MEDIUM_FOOTPRINT, List.of(),
				"mushroom/dream_large_1"));
		FeatureUtils.register(ctx, DEMONIC_MIASMA_SMALL, template, vegetation(TINY_FOOTPRINT, List.of(),
				"mushroom/demonic_miasma_small_1", "mushroom/demonic_miasma_small_2"));
		FeatureUtils.register(ctx, DEMONIC_MIASMA_MEDIUM, template, vegetation(SMALL_FOOTPRINT, List.of(),
				"mushroom/demonic_miasma_medium_1", "mushroom/demonic_miasma_medium_2"));
		FeatureUtils.register(ctx, DEMONIC_MIASMA_LARGE, template, vegetation(MEDIUM_FOOTPRINT, List.of(),
				"mushroom/demonic_miasma_large_1", "mushroom/demonic_miasma_large_2"));

		// oak : blue fir = 1 : 1
		FeatureUtils.register(ctx, GIANT_MIXED, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
				new WeightedPlacedFeature(PlacementUtils.inlinePlaced(cf.getOrThrow(OAK_GIANT)), 0.5F)
		), PlacementUtils.inlinePlaced(cf.getOrThrow(BLUE_FIR_GIANT))));
		FeatureUtils.register(ctx, LARGE_MIXED, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
				new WeightedPlacedFeature(PlacementUtils.inlinePlaced(cf.getOrThrow(OAK_LARGE)), 0.5F)
		), PlacementUtils.inlinePlaced(cf.getOrThrow(BLUE_FIR_LARGE))));
		// vanilla huge mushrooms stay as a rare accent
		FeatureUtils.register(ctx, MUSHROOM_MEDIUM, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
				new WeightedPlacedFeature(PlacementUtils.inlinePlaced(cf.getOrThrow(TreeFeatures.HUGE_BROWN_MUSHROOM)), 0.08F),
				new WeightedPlacedFeature(PlacementUtils.inlinePlaced(cf.getOrThrow(TreeFeatures.HUGE_RED_MUSHROOM)), 0.08F)
		), PlacementUtils.inlinePlaced(cf.getOrThrow(MUSHROOM_MEDIUM_TEMPLATE))));
		FeatureUtils.register(ctx, MUSHROOM_SMALL_CLUSTER, Feature.RANDOM_PATCH, new RandomPatchConfiguration(
				5, 4, 2, PlacementUtils.inlinePlaced(cf.getOrThrow(MUSHROOM_SMALL))));
		// pond 6-9 blocks in radius: 24 wide footprint, 3-5 blobs of component radius 8
		BlockState cattailWet = GLNaturalBlocks.FLAME_CATTAIL.get().defaultBlockState()
				.setValue(WaterloggedBushBlock.WATERLOGGED, true);
		BlockState cattailDry = GLNaturalBlocks.FLAME_CATTAIL.get().defaultBlockState()
				.setValue(WaterloggedBushBlock.WATERLOGGED, false);
		BlockState dripLower = Blocks.SMALL_DRIPLEAF.defaultBlockState()
				.setValue(SmallDripleafBlock.HALF, DoubleBlockHalf.LOWER)
				.setValue(BlockStateProperties.WATERLOGGED, true)
				.setValue(SmallDripleafBlock.FACING, Direction.NORTH);
		BlockState dripUpper = Blocks.SMALL_DRIPLEAF.defaultBlockState()
				.setValue(SmallDripleafBlock.HALF, DoubleBlockHalf.UPPER)
				.setValue(BlockStateProperties.WATERLOGGED, false)
				.setValue(SmallDripleafBlock.FACING, Direction.NORTH);
		FeatureUtils.register(ctx, LAKE, GLWorldGen.LAKE.get(), new MagicalForestLakeFeature.Data(
				Blocks.WATER.defaultBlockState(), Blocks.CLAY.defaultBlockState(),
				Blocks.DIRT.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState(),
				4, 24, 8, 3, 5, 8, 3, List.of(
				new MagicalForestLakeFeature.Deco(MagicalForestLakeFeature.Target.FLOATING,
						Blocks.LILY_PAD.defaultBlockState(), Blocks.AIR.defaultBlockState(), 0.15f, BlockTags.DIRT),
				new MagicalForestLakeFeature.Deco(MagicalForestLakeFeature.Target.SHALLOW,
						cattailWet, Blocks.AIR.defaultBlockState(), 0.25f, BlockTags.DIRT),
				new MagicalForestLakeFeature.Deco(MagicalForestLakeFeature.Target.SHALLOW,
						dripLower, dripUpper, 0.12f, BlockTags.SMALL_DRIPLEAF_PLACEABLE),
				new MagicalForestLakeFeature.Deco(MagicalForestLakeFeature.Target.SHORE,
						cattailDry, Blocks.AIR.defaultBlockState(), 0.08f, BlockTags.DIRT))));
	}

	public static void placed(BootstrapContext<PlacedFeature> ctx) {
		var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
		var structures = structures(ctx.lookup(Registries.STRUCTURE));
		var noClearing = NoiseBandFilter.below(CLEARING, CLEARING_MIN);
		var glade = NoiseBandFilter.below(FOREST_TYPE, GLADE);
		var noGlade = NoiseBandFilter.atLeast(FOREST_TYPE, GLADE);
		var mixed = new NoiseBandFilter(FOREST_TYPE, GLADE, OLD_GROWTH);
		var oldGrowth = NoiseBandFilter.atLeast(FOREST_TYPE, OLD_GROWTH);

		// ponds run in the LAKES step, ahead of the trees below; on dry ground only, never on ocean
		PlacementUtils.register(ctx, LAKE_PF, cf.getOrThrow(LAKE),
				RarityFilter.onAverageOnceEvery(6), InSquarePlacement.spread(),
				avoid(structures, LAKE_CLEARANCE), FLOOR, SurfaceWaterDepthFilter.forMaxDepth(0), BiomeFilter.biome());

		// L1 giant trees: one grid, density and species by forest type
		tree(ctx, GIANT_MIXED_PF, cf.getOrThrow(GIANT_MIXED), new JitteredGridPlacement(GIANT),
				mixed, noClearing, avoid(structures, GIANT_CLEARANCE));
		tree(ctx, GIANT_FIR_PF, cf.getOrThrow(BLUE_FIR_GIANT), new JitteredGridPlacement(GIANT),
				oldGrowth, noClearing, avoid(structures, GIANT_CLEARANCE));
		tree(ctx, GIANT_GLADE_PF, cf.getOrThrow(BLUE_FIR_GIANT), new JitteredGridPlacement(GIANT_SPARSE),
				glade, noClearing, avoid(structures, GIANT_CLEARANCE));

		// L2 large trees
		tree(ctx, LARGE_MIXED_PF, cf.getOrThrow(LARGE_MIXED), new JitteredGridPlacement(LARGE),
				mixed, noClearing, LARGE_SPACING, avoid(structures, LARGE_CLEARANCE));
		tree(ctx, LARGE_FIR_PF, cf.getOrThrow(BLUE_FIR_LARGE), new JitteredGridPlacement(LARGE_DENSE),
				oldGrowth, noClearing, LARGE_SPACING, avoid(structures, LARGE_CLEARANCE));

		// huge mushrooms: the main cast of the glade, a rare sight elsewhere
		tree(ctx, MUSHROOM_LARGE_GLADE_PF, cf.getOrThrow(MUSHROOM_LARGE), new JitteredGridPlacement(GLADE_MUSHROOM),
				glade, MUSHROOM_SPACING, avoid(structures, MEDIUM_CLEARANCE));
		tree(ctx, MUSHROOM_LARGE_PF, cf.getOrThrow(MUSHROOM_LARGE), RarityFilter.onAverageOnceEvery(6),
				InSquarePlacement.spread(), noGlade, MUSHROOM_SPACING, avoid(structures, MEDIUM_CLEARANCE));

		// L3 medium trees stand at the rim of the big crowns
		tree(ctx, MEDIUM_TREE_PF, cf.getOrThrow(BLUE_FIR_MEDIUM), CountPlacement.of(3), InSquarePlacement.spread(),
				noGlade, noClearing, MEDIUM_SPACING, avoid(structures, MEDIUM_CLEARANCE));
		tree(ctx, MUSHROOM_MEDIUM_GLADE_PF, cf.getOrThrow(MUSHROOM_MEDIUM), CountPlacement.of(2),
				InSquarePlacement.spread(), glade, UNDERSTORY_SPACING, avoid(structures, SMALL_CLEARANCE));
		tree(ctx, MUSHROOM_MEDIUM_PF, cf.getOrThrow(MUSHROOM_MEDIUM), RarityFilter.onAverageOnceEvery(3),
				InSquarePlacement.spread(), noGlade, UNDERSTORY_SPACING, avoid(structures, SMALL_CLEARANCE));

		// L4 understory: bushes and small mushrooms, under the crowns as well
		tree(ctx, BUSH_PF, cf.getOrThrow(AZALEA_BUSH), CountPlacement.of(4), InSquarePlacement.spread(),
				UNDERSTORY_SPACING, avoid(structures, GROUND_CLEARANCE));
		tree(ctx, MUSHROOM_SMALL_GLADE_PF, cf.getOrThrow(MUSHROOM_SMALL_CLUSTER), CountPlacement.of(2),
				InSquarePlacement.spread(), glade, avoid(structures, GROUND_CLEARANCE));
		tree(ctx, MUSHROOM_SMALL_PF, cf.getOrThrow(MUSHROOM_SMALL_CLUSTER), RarityFilter.onAverageOnceEvery(2),
				InSquarePlacement.spread(), noGlade, avoid(structures, GROUND_CLEARANCE));

		// L5 ground cover: FLOOR looks through the canopies that now cover most of the biome
		cover(ctx, FALLEN_LEAVES_PF, cf.getOrThrow(FALLEN_LEAVES), CountPlacement.of(2), new CanopyFilter(4, true));
		cover(ctx, MOSS_CARPET_PF, cf.getOrThrow(MOSS_CARPET), CountPlacement.of(1), new CanopyFilter(4, true));
		// placement tries per chunk (patch count x 32): short grass ~310, fern ~230; custom patch 2 x 24: broom grass 16, bracken 32
		cover(ctx, GRASS_PF, cf.getOrThrow(GLFeatureGen.MAGICAL_FOREST_GRASS), CountPlacement.of(2));
		cover(ctx, LARGE_FERN_PF, cf.getOrThrow(VegetationFeatures.PATCH_LARGE_FERN), RarityFilter.onAverageOnceEvery(5));
		cover(ctx, GRASS_FOREST_PF, cf.getOrThrow(VegetationFeatures.PATCH_GRASS), CountPlacement.of(8));
		cover(ctx, GRASS_TAIGA_PF, cf.getOrThrow(VegetationFeatures.PATCH_TAIGA_GRASS), CountPlacement.of(9));
		cover(ctx, FLOWERS_PF, cf.getOrThrow(GLFeatureGen.MAGICAL_FOREST_FLOWERS), CountPlacement.of(3));
		cover(ctx, MUSHROOMS_PF, cf.getOrThrow(GLFeatureGen.MAGICAL_FOREST_MUSHROOMS), CountPlacement.of(2));
	}

	private static final PlacementModifier FLOOR = HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);

	/**
	 * A tree sized feature: the given modifiers, then the forest floor. Water and slope are
	 * checked by the template feature itself (a surface water depth filter would also reject
	 * columns under hanging vines), the heightmap only has to start above the ground.
	 */
	private static void tree(BootstrapContext<PlacedFeature> ctx, ResourceKey<PlacedFeature> key,
							 Holder<ConfiguredFeature<?, ?>> feature, PlacementModifier... modifiers) {
		List<PlacementModifier> list = new ArrayList<>(List.of(modifiers));
		list.add(FLOOR);
		list.add(BiomeFilter.biome());
		PlacementUtils.register(ctx, key, feature, list);
	}

	/**
	 * Ground cover: {@code count} patches spread over the chunk, on the forest floor.
	 */
	private static void cover(BootstrapContext<PlacedFeature> ctx, ResourceKey<PlacedFeature> key,
							  Holder<ConfiguredFeature<?, ?>> feature, PlacementModifier count, PlacementModifier... filters) {
		List<PlacementModifier> list = new ArrayList<>();
		list.add(count);
		list.add(InSquarePlacement.spread());
		list.addAll(List.of(filters));
		list.add(FLOOR);
		list.add(BiomeFilter.biome());
		PlacementUtils.register(ctx, key, feature, list);
	}

	private static GridExclusionFilter spacing(Entry... entries) {
		return new GridExclusionFilter(List.of(entries));
	}

	private static AvoidStructuresFilter avoid(HolderSet<Structure> structures, int clearance) {
		return new AvoidStructuresFilter(structures, clearance);
	}

	private static TemplateFeatureConfig vegetation(Footprint footprint, List<Holder<PlacedFeature>> floor, String... templates) {
		var list = SimpleWeightedRandomList.<ResourceLocation>builder();
		for (var id : templates) {
			list.add(GensokyoLegacy.loc("magical_forest/" + id), 1);
		}
		return new TemplateFeatureConfig(list.build(), TemplateFeatureConfig.NO_PROCESSORS, BlockTags.DIRT,
				footprint.radius(), footprint.maxSlope(), 0, footprint.rootDepth(), floor);
	}

	private static Holder<PlacedFeature> floorDisk(BlockStateProvider state, int minRadius, int maxRadius) {
		return PlacementUtils.inlinePlaced(Feature.DISK, new DiskConfiguration(
				new RuleBasedBlockStateProvider(state, List.of()),
				BlockPredicate.matchesBlocks(List.of(Blocks.GRASS_BLOCK, Blocks.DIRT)),
				UniformInt.of(minRadius, maxRadius), 2));
	}

	private static RandomPatchConfiguration groundPatch(int tries, int spread, BlockStateProvider state) {
		return new RandomPatchConfiguration(tries, spread, 3, PlacementUtils.filtered(Feature.SIMPLE_BLOCK,
				new SimpleBlockConfiguration(state), BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
						BlockPredicate.matchesTag(Direction.DOWN.getNormal(), BlockTags.DIRT))));
	}

	private static RandomPatchConfiguration fallenLeavesPatch(int tries, int spread) {
		BlockState leaves = GLNaturalBlocks.CEDAR_FALLEN_LEAVES.get().defaultBlockState();
		return groundPatch(tries, spread, new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
				.add(leaves.setValue(CedarFallenLeavesBlock.LAYERS, 1), 3)
				.add(leaves.setValue(CedarFallenLeavesBlock.LAYERS, 2), 1)
				.build()));
	}

	private static HolderSet<Structure> structures(HolderGetter<Structure> lookup) {
		List<Holder<Structure>> list = new ArrayList<>();
		for (var id : STRUCTURES) {
			list.add(lookup.getOrThrow(ResourceKey.create(Registries.STRUCTURE, GensokyoLegacy.loc(id))));
		}
		return HolderSet.direct(list);
	}

	private static ResourceKey<ConfiguredFeature<?, ?>> cf(String id) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, GensokyoLegacy.loc("magical_forest/" + id));
	}

	private static ResourceKey<PlacedFeature> pf(String id) {
		return ResourceKey.create(Registries.PLACED_FEATURE, GensokyoLegacy.loc("magical_forest/" + id));
	}

}
