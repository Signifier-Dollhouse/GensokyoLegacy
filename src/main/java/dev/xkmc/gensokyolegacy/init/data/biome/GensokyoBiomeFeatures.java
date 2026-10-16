package dev.xkmc.gensokyolegacy.init.data.biome;

import com.tterrag.registrate.util.entry.BlockEntry;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.lake.LakeConfig;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.NetherFeatures;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

/**
 * The feature tail of the Gensokyo biomes: the ground covers, grave markers and cave deposits that turn a copied
 * vanilla biome into the one the biome guide describes. Every biome is a vanilla base plus a subsequence of
 * {@link #ORDER}.
 * <p>
 * That ordering is load-bearing. {@code FeatureSorter} links biomes by the order features appear in, and a cycle
 * between two of them crashes chunk generation at runtime rather than at datagen. A subsequence of one fixed list can
 * never disagree with itself, and appending the whole tail after the vanilla chain keeps a custom feature from ever
 * landing in the middle of a vanilla one.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class GensokyoBiomeFeatures {

	// ground covers
	private static final ResourceKey<ConfiguredFeature<?, ?>> STONES = cf("stones");
	private static final ResourceKey<ConfiguredFeature<?, ?>> GRAVE_MARKER = cf("grave_marker");
	private static final ResourceKey<ConfiguredFeature<?, ?>> GHOST_FLOWERS = cf("ghost_flowers");
	private static final ResourceKey<ConfiguredFeature<?, ?>> VILLAGE_FLOWERS = cf("village_flowers");
	private static final ResourceKey<ConfiguredFeature<?, ?>> BARRENS_SCRUB = cf("barrens_scrub");
	private static final ResourceKey<ConfiguredFeature<?, ?>> HILL_FLOWERS = cf("hill_of_the_nameless_flowers");
	private static final ResourceKey<ConfiguredFeature<?, ?>> RAVINE_MOSS = cf("ravine_moss");
	private static final ResourceKey<ConfiguredFeature<?, ?>> FOOTHILL_MOSS = cf("foothill_moss");
	private static final ResourceKey<ConfiguredFeature<?, ?>> SANCTUARY_FLOWERS = cf("sanctuary_flowers");
	// soil disks: these replace the ground instead of standing on it
	private static final ResourceKey<ConfiguredFeature<?, ?>> PODZOL_DISK = cf("muenzuka_podzol_disk");
	private static final ResourceKey<ConfiguredFeature<?, ?>> COARSE_DIRT_DISK = cf("barrens_coarse_dirt_disk");
	private static final ResourceKey<ConfiguredFeature<?, ?>> MUD_DISK = cf("bamboo_forest_mud_disk");
	private static final ResourceKey<ConfiguredFeature<?, ?>> RAVINE_MUD_DISK = cf("ravine_mud_disk");
	// cave deposits
	private static final ResourceKey<ConfiguredFeature<?, ?>> BLOOD_DEPOSIT = cf("blood_deposit");
	private static final ResourceKey<ConfiguredFeature<?, ?>> BONE_PILE = cf("bone_pile");
	private static final ResourceKey<ConfiguredFeature<?, ?>> DEN_RUBBLE = cf("den_rubble");
	private static final ResourceKey<ConfiguredFeature<?, ?>> FUNGUS_PATCH = cf("fungus_patch");
	private static final ResourceKey<ConfiguredFeature<?, ?>> FIRE_VENT = cf("fire_vent");
	private static final ResourceKey<ConfiguredFeature<?, ?>> ASH_PATCH = cf("ash_patch");
	private static final ResourceKey<ConfiguredFeature<?, ?>> OLD_RUBBLE = cf("old_rubble");
	// the ruins' standing water and the realm's lava, both in a cavern rather than on the surface
	private static final ResourceKey<ConfiguredFeature<?, ?>> FORMER_HELL_LAKE = cf("former_hell_lake");
	private static final ResourceKey<ConfiguredFeature<?, ?>> ANIMAL_REALM_LAKE = cf("animal_realm_lake");
	// the four fixed biomes: Misty Lake, its shore, the Tengu Settlement and the summit above it
	private static final ResourceKey<ConfiguredFeature<?, ?>> LAKE_CLAY = cf("lake_clay");
	private static final ResourceKey<ConfiguredFeature<?, ?>> LAKE_CATTAILS = cf("lake_cattails");
	private static final ResourceKey<ConfiguredFeature<?, ?>> SHORE_SAND = cf("shore_sand");
	private static final ResourceKey<ConfiguredFeature<?, ?>> SHORE_FLOWERS = cf("shore_flowers");
	private static final ResourceKey<ConfiguredFeature<?, ?>> SETTLEMENT_PATHS = cf("settlement_paths");
	private static final ResourceKey<ConfiguredFeature<?, ?>> CLIFF_SCREE = cf("cliff_scree");
	private static final ResourceKey<ConfiguredFeature<?, ?>> SUMMIT_SOIL_DISK = cf("summit_soil_disk");
	private static final ResourceKey<ConfiguredFeature<?, ?>> SUMMIT_MOSS = cf("summit_moss");
	private static final ResourceKey<ConfiguredFeature<?, ?>> SUMMIT_SCREE = cf("summit_scree");

	// placed features, mirroring the configured ones
	private static final ResourceKey<PlacedFeature> PODZOL_PF = pf("muenzuka_podzol");
	private static final ResourceKey<PlacedFeature> COARSE_DIRT_PF = pf("barrens_coarse_dirt");
	private static final ResourceKey<PlacedFeature> MUD_PF = pf("bamboo_forest_mud");
	private static final ResourceKey<PlacedFeature> RAVINE_MUD_PF = pf("ravine_mud");
	private static final ResourceKey<PlacedFeature> STONES_PF = pf("stones");
	private static final ResourceKey<PlacedFeature> GRAVE_MARKER_PF = pf("grave_marker");
	private static final ResourceKey<PlacedFeature> GHOST_FLOWERS_PF = pf("ghost_flowers");
	private static final ResourceKey<PlacedFeature> VILLAGE_FLOWERS_PF = pf("village_flowers");
	private static final ResourceKey<PlacedFeature> BARRENS_SCRUB_PF = pf("barrens_scrub");
	private static final ResourceKey<PlacedFeature> HILL_FLOWERS_PF = pf("hill_of_the_nameless_flowers");
	private static final ResourceKey<PlacedFeature> BAMBOO_DENSE_PF = pf("bamboo_dense");
	private static final ResourceKey<PlacedFeature> BAMBOO_SPARSE_PF = pf("bamboo_sparse");
	private static final ResourceKey<PlacedFeature> RAVINE_MOSS_PF = pf("ravine_moss");
	private static final ResourceKey<PlacedFeature> FOOTHILL_MOSS_PF = pf("foothill_moss");
	private static final ResourceKey<PlacedFeature> SANCTUARY_TREES_PF = pf("sanctuary_trees");
	private static final ResourceKey<PlacedFeature> SANCTUARY_FLOWERS_PF = pf("sanctuary_flowers");
	private static final ResourceKey<PlacedFeature> BLOOD_DEPOSIT_PF = pf("blood_deposit");
	private static final ResourceKey<PlacedFeature> BONE_PILE_PF = pf("bone_pile");
	private static final ResourceKey<PlacedFeature> DEN_RUBBLE_PF = pf("den_rubble");
	private static final ResourceKey<PlacedFeature> FUNGUS_PATCH_PF = pf("fungus_patch");
	public static final ResourceKey<PlacedFeature> BLAZING_HELL_LAVA_PF = pf("blazing_hell_lava_spring");
	public static final ResourceKey<PlacedFeature> RAVINE_SPRING_WATER_PF = pf("ravine_spring_water");
	private static final ResourceKey<PlacedFeature> FIRE_VENT_PF = pf("fire_vent");
	private static final ResourceKey<PlacedFeature> ASH_PATCH_PF = pf("ash_patch");
	private static final ResourceKey<PlacedFeature> OLD_RUBBLE_PF = pf("old_rubble");
	public static final ResourceKey<PlacedFeature> FORMER_HELL_LAKE_PF = pf("former_hell_lake");
	public static final ResourceKey<PlacedFeature> ANIMAL_REALM_LAKE_PF = pf("animal_realm_lake");
	private static final ResourceKey<PlacedFeature> LAKE_CLAY_PF = pf("lake_clay");
	private static final ResourceKey<PlacedFeature> LAKE_CATTAILS_PF = pf("lake_cattails");
	private static final ResourceKey<PlacedFeature> LILY_PADS_PF = pf("misty_lake_lily_pads");
	private static final ResourceKey<PlacedFeature> SHORE_SAND_PF = pf("shore_sand");
	private static final ResourceKey<PlacedFeature> SHORE_REEDS_PF = pf("shore_reeds");
	private static final ResourceKey<PlacedFeature> SHORE_FLOWERS_PF = pf("shore_flowers");
	private static final ResourceKey<PlacedFeature> SETTLEMENT_PATHS_PF = pf("settlement_paths");
	private static final ResourceKey<PlacedFeature> CLIFF_SCREE_PF = pf("cliff_scree");
	private static final ResourceKey<PlacedFeature> SUMMIT_SOIL_PF = pf("summit_soil");
	private static final ResourceKey<PlacedFeature> SUMMIT_MOSS_PF = pf("summit_moss");
	private static final ResourceKey<PlacedFeature> SUMMIT_SCREE_PF = pf("summit_scree");
	private static final ResourceKey<PlacedFeature> SUMMIT_TREES_PF = pf("summit_trees");

	/**
	 * The one order the custom placed features may appear in. A biome's tail must be a subsequence of this list, never
	 * a reordering of it.
	 */
	private static final List<ResourceKey<PlacedFeature>> ORDER = List.of(
			PODZOL_PF, COARSE_DIRT_PF, MUD_PF, RAVINE_MUD_PF,
			STONES_PF, GRAVE_MARKER_PF, GHOST_FLOWERS_PF, VILLAGE_FLOWERS_PF, BARRENS_SCRUB_PF,
			HILL_FLOWERS_PF, BAMBOO_DENSE_PF, BAMBOO_SPARSE_PF, RAVINE_MOSS_PF, FOOTHILL_MOSS_PF,
			SANCTUARY_TREES_PF, SANCTUARY_FLOWERS_PF,
			BLOOD_DEPOSIT_PF, BONE_PILE_PF, DEN_RUBBLE_PF, FUNGUS_PATCH_PF, FIRE_VENT_PF, ASH_PATCH_PF, OLD_RUBBLE_PF,
			BLAZING_HELL_LAVA_PF, FORMER_HELL_LAKE_PF, ANIMAL_REALM_LAKE_PF,
			LAKE_CLAY_PF, LAKE_CATTAILS_PF, LILY_PADS_PF, SHORE_SAND_PF, SHORE_REEDS_PF, SHORE_FLOWERS_PF,
			SETTLEMENT_PATHS_PF, CLIFF_SCREE_PF,
			SUMMIT_SOIL_PF, SUMMIT_MOSS_PF, SUMMIT_SCREE_PF, SUMMIT_TREES_PF);

	/**
	 * Splits the summit's exposed rock from its scree. Coarse enough that a patch reads as a slope rather than as
	 * speckle, and read per block by the surface rule, so it mottles a cliff face instead of banding it.
	 */
	public static final ResourceKey<NormalNoise.NoiseParameters> SUMMIT_SURFACE =
			ResourceKey.create(Registries.NOISE, GensokyoLegacy.loc("summit_surface"));

	/** One biome's custom features, split by the decoration step each group runs in. */
	public record Tail(List<ResourceKey<PlacedFeature>> vegetation, List<ResourceKey<PlacedFeature>> soil,
					   List<ResourceKey<PlacedFeature>> cave) {}

	public static final Tail MUENZUKA = new Tail(tail(STONES_PF, GRAVE_MARKER_PF, GHOST_FLOWERS_PF), tail(PODZOL_PF), tail());
	public static final Tail HUMAN_VILLAGE = new Tail(tail(VILLAGE_FLOWERS_PF), tail(), tail());
	public static final Tail BARRENS = new Tail(tail(STONES_PF, BARRENS_SCRUB_PF), tail(COARSE_DIRT_PF), tail());
	public static final Tail BAMBOO_FOREST = new Tail(tail(BAMBOO_DENSE_PF, BAMBOO_SPARSE_PF), tail(MUD_PF), tail());
	public static final Tail HILL = new Tail(tail(STONES_PF, HILL_FLOWERS_PF), tail(), tail());
	public static final Tail VALLEY = new Tail(tail(STONES_PF), tail(), tail());
	public static final Tail RAVINE = new Tail(tail(STONES_PF, RAVINE_MOSS_PF), tail(RAVINE_MUD_PF), tail());
	public static final Tail FOOTHILLS = new Tail(tail(STONES_PF, FOOTHILL_MOSS_PF), tail(), tail());
	public static final Tail SANCTUARY = new Tail(tail(SANCTUARY_TREES_PF, SANCTUARY_FLOWERS_PF), tail(), tail());
	public static final Tail BLOOD_POOLS = new Tail(tail(), tail(), tail(BLOOD_DEPOSIT_PF, BONE_PILE_PF));
	public static final Tail ANIMAL_REALM = new Tail(tail(), tail(), tail(DEN_RUBBLE_PF, FUNGUS_PATCH_PF));
	public static final Tail BLAZING_FIRES = new Tail(tail(), tail(), tail(FIRE_VENT_PF, ASH_PATCH_PF));
	public static final Tail FORMER_HELL = new Tail(tail(), tail(), tail(OLD_RUBBLE_PF));
	/** Empty: a hall of bare rock needs nothing laid on it. Was deep dark's sculk once; see {@code GLBiomes}. */
	public static final Tail MISTY_LAKE = new Tail(tail(LAKE_CLAY_PF, LAKE_CATTAILS_PF, LILY_PADS_PF), tail(), tail());
	public static final Tail MISTY_LAKE_SHORE = new Tail(
			tail(STONES_PF, SHORE_SAND_PF, SHORE_REEDS_PF, SHORE_FLOWERS_PF), tail(), tail());
	public static final Tail TENGU_SETTLEMENT = new Tail(tail(FOOTHILL_MOSS_PF, SETTLEMENT_PATHS_PF), tail(), tail());
	/**
	 * The settlement's tail with the paths dropped and scree in place of moss: boulders carry the slope and a sparser
	 * mix than the summit's carries the bare rock, so it reads drier than the town without reading as another summit.
	 */
	public static final Tail SECRET_HEAVEN_CLIFF = new Tail(tail(STONES_PF, CLIFF_SCREE_PF), tail(), tail());
	public static final Tail SUMMIT = new Tail(
			tail(SUMMIT_SOIL_PF, SUMMIT_MOSS_PF, SUMMIT_SCREE_PF, SUMMIT_TREES_PF), tail(), tail());

	/**
	 * The tail of one biome, checked against {@link #ORDER} rather than trusted: a biome that lists its features out of
	 * the shared order is the one way to reintroduce a feature order cycle, and the failure surfaces as a world load
	 * crash rather than as a datagen error.
	 */
	private static List<ResourceKey<PlacedFeature>> tail(ResourceKey<PlacedFeature>... tail) {
		var list = List.of(tail);
		int previous = -1;
		for (var key : list) {
			int index = ORDER.indexOf(key);
			if (index <= previous) throw new IllegalArgumentException("not a subsequence of " + ORDER + ": " + list);
			previous = index;
		}
		return list;
	}

	/** Called from {@code GLFeatureGen}, which owns the single {@link Registries#NOISE} bootstrap. */
	public static void noise(BootstrapContext<NormalNoise.NoiseParameters> ctx) {
		ctx.register(SUMMIT_SURFACE, new NormalNoise.NoiseParameters(-5, 1.0, 1.0));
	}

	public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, STONES, Feature.RANDOM_PATCH, onDirt(18, new Palette()
				.add(Blocks.MOSSY_COBBLESTONE, 3).add(Blocks.COBBLESTONE, 2).add(Blocks.STONE_BRICKS, 1).build()));
		FeatureUtils.register(ctx, GRAVE_MARKER, Feature.RANDOM_PATCH, onDirt(8, new Palette()
				.add(Blocks.COBBLESTONE, 3).add(Blocks.MOSSY_COBBLESTONE, 2).add(Blocks.CRACKED_STONE_BRICKS, 1).build()));
		FeatureUtils.register(ctx, GHOST_FLOWERS, Feature.RANDOM_PATCH, onDirt(24, new Palette()
				.add(Blocks.LILY_OF_THE_VALLEY, 3).add(Blocks.OXEYE_DAISY, 3).add(Blocks.ALLIUM, 2)
				.add(Blocks.DEAD_BUSH, 2).add(GLNaturalBlocks.EUGUNE_GHOST_FIRE, 1).build()));
		FeatureUtils.register(ctx, VILLAGE_FLOWERS, Feature.RANDOM_PATCH, onDirt(32, new Palette()
				.add(Blocks.CORNFLOWER, 3).add(Blocks.POPPY, 3).add(Blocks.ALLIUM, 2)
				.add(Blocks.DANDELION, 2).add(Blocks.WHITE_TULIP, 2).build()));
		FeatureUtils.register(ctx, BARRENS_SCRUB, Feature.RANDOM_PATCH, onDirt(28, new Palette()
				.add(Blocks.DEAD_BUSH, 3).add(Blocks.SHORT_GRASS, 3)
				.add(Blocks.COARSE_DIRT, 2).add(Blocks.GRAVEL, 1).build()));
		FeatureUtils.register(ctx, HILL_FLOWERS, Feature.RANDOM_PATCH, onDirt(32, new Palette()
				.add(Blocks.LILY_OF_THE_VALLEY, 4).add(Blocks.OXEYE_DAISY, 3)
				.add(Blocks.ALLIUM, 2).add(Blocks.TALL_GRASS, 2).build()));
		FeatureUtils.register(ctx, RAVINE_MOSS, Feature.RANDOM_PATCH, onDirt(40, new Palette()
				.add(Blocks.MOSS_CARPET, 3).add(Blocks.LARGE_FERN, 2)
				.add(GLNaturalBlocks.BRACKEN, 2).add(Blocks.FERN, 2).build()));
		FeatureUtils.register(ctx, FOOTHILL_MOSS, Feature.RANDOM_PATCH, onDirt(28, new Palette()
				.add(Blocks.MOSS_CARPET, 2).add(Blocks.FERN, 2)
				.add(GLNaturalBlocks.BRACKEN, 2).add(Blocks.SHORT_GRASS, 2).build()));
		FeatureUtils.register(ctx, SANCTUARY_FLOWERS, Feature.RANDOM_PATCH, onDirt(28, new Palette()
				.add(Blocks.LILY_OF_THE_VALLEY, 3).add(Blocks.OXEYE_DAISY, 3).add(Blocks.WHITE_TULIP, 2)
				.add(GLNaturalBlocks.STAR_FLOWER, 2).add(Blocks.ALLIUM, 1).build()));

		FeatureUtils.register(ctx, PODZOL_DISK, Feature.DISK, new DiskConfiguration(
				RuleBasedBlockStateProvider.simple(Blocks.PODZOL),
				BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK), UniformInt.of(2, 4), 1));
		FeatureUtils.register(ctx, COARSE_DIRT_DISK, Feature.DISK, new DiskConfiguration(
				RuleBasedBlockStateProvider.simple(Blocks.COARSE_DIRT),
				BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK), UniformInt.of(2, 4), 1));
		FeatureUtils.register(ctx, MUD_DISK, Feature.DISK, new DiskConfiguration(
				RuleBasedBlockStateProvider.simple(Blocks.MUD),
				BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK, Blocks.DIRT), UniformInt.of(2, 5), 1));
		FeatureUtils.register(ctx, RAVINE_MUD_DISK, Feature.DISK, new DiskConfiguration(
				RuleBasedBlockStateProvider.simple(Blocks.MUD),
				BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK, Blocks.DIRT), UniformInt.of(3, 6), 1));

		FeatureUtils.register(ctx, BLOOD_DEPOSIT, Feature.RANDOM_PATCH, onStone(20, new Palette()
				.add(Blocks.NETHERRACK, 3).add(Blocks.CRIMSON_NYLIUM, 2).add(Blocks.REDSTONE_ORE, 2).build()));
		FeatureUtils.register(ctx, BONE_PILE, Feature.RANDOM_PATCH, onStone(12, new Palette()
				.add(Blocks.BONE_BLOCK, 3).add(Blocks.SOUL_SAND, 2).add(Blocks.SKELETON_SKULL, 1).build()));
		FeatureUtils.register(ctx, DEN_RUBBLE, Feature.RANDOM_PATCH, onStone(16, new Palette()
				.add(Blocks.BONE_BLOCK, 3).add(Blocks.COBWEB, 2)
				.add(Blocks.COBBLESTONE, 2).add(Blocks.SPRUCE_FENCE, 1).build()));
		FeatureUtils.register(ctx, FUNGUS_PATCH, Feature.RANDOM_PATCH, onStone(24, new Palette()
				.add(GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap, 2).add(GLNaturalBlocks.EUGUNE_GHOST_FIRE, 2)
				.add(GLNaturalBlocks.EUGUNE_BROWN, 2).add(Blocks.CRIMSON_NYLIUM, 2).add(Blocks.GLOW_LICHEN, 2).build()));
		FeatureUtils.register(ctx, FIRE_VENT, Feature.RANDOM_PATCH, onStone(16, new Palette()
				.add(Blocks.MAGMA_BLOCK, 3).add(Blocks.BLACKSTONE, 2).add(Blocks.BASALT, 2).add(Blocks.GRAVEL, 1).build()));
		FeatureUtils.register(ctx, ASH_PATCH, Feature.RANDOM_PATCH, onStone(24, new Palette()
				.add(Blocks.SOUL_SOIL, 3).add(Blocks.BLACKSTONE, 2).add(Blocks.GRAVEL, 1).build()));
		FeatureUtils.register(ctx, OLD_RUBBLE, Feature.RANDOM_PATCH, onStone(16, new Palette()
				.add(Blocks.CRACKED_STONE_BRICKS, 3).add(Blocks.MOSSY_COBBLESTONE, 2)
				.add(Blocks.BRICKS, 2).add(Blocks.GRAVEL, 1).build()));

		// The ruins' standing water, and the only lake in the biome list that is not on the surface. It has to be a
		// separate configured feature from the magical forest's pond because the mode is in the config: this one is
		// poured into a cavern, which means it finds its own floor instead of taking the ground it lands on, keeps
		// five blocks of headroom above the water, and dams itself with a wall of the ruin's own stone brick rather
		// than a rim of turf. The box is 20 wide and 11 tall, which leaves room for 3 deep of water, the air the
		// maker cuts above it and that headroom. The rim is one block thick: a thicker one is a second course of
		// stone brick standing on the cavern floor, and where the floor is thin or the carver has eaten into it that
		// reads as a ledge floating over nothing.
		FeatureUtils.register(ctx, FORMER_HELL_LAKE, GLWorldGen.LAKE.get(), new LakeConfig(
				LakeConfig.Mode.UNDERGROUND, 5,
				new LakeConfig.Shape(4, 20, 11, 3, 4, 6, 1),
				new LakeConfig.Material(Blocks.WATER.defaultBlockState(), Blocks.AIR.defaultBlockState(),
						Blocks.STONE_BRICKS.defaultBlockState(), Blocks.AIR.defaultBlockState()), List.of()));

		// The realm's lava, the same pool in the same caverns as the ruins' water and for the same reason: this is
		// the other cold half of the underground, and a lava pool in a cavern is what says so. Netherrack for the
		// basin, because that is the rock a nether cavern is made of. It is laid after the ruins' pool because that
		// is the order the two hell rows are written in.
		FeatureUtils.register(ctx, ANIMAL_REALM_LAKE, GLWorldGen.LAKE.get(), new LakeConfig(
				LakeConfig.Mode.UNDERGROUND, 5,
				new LakeConfig.Shape(4, 20, 11, 3, 4, 6, 1),
				new LakeConfig.Material(Blocks.LAVA.defaultBlockState(), Blocks.AIR.defaultBlockState(),
						Blocks.NETHERRACK.defaultBlockState(), Blocks.AIR.defaultBlockState()), List.of()));

		var cattail = GLNaturalBlocks.FLAME_CATTAIL.get().defaultBlockState();
		// the lakebed is sand and gravel under the surface rule, so these two sit on those and not on BlockTags.DIRT
		FeatureUtils.register(ctx, LAKE_CLAY, Feature.RANDOM_PATCH, onGround(14, new Palette()
				.add(Blocks.CLAY, 3).add(Blocks.GRAVEL, 2).add(Blocks.MUD, 1).build(),
				Blocks.SAND, Blocks.GRAVEL, Blocks.CLAY, Blocks.DIRT));
		FeatureUtils.register(ctx, LAKE_CATTAILS, Feature.RANDOM_PATCH, onGround(10, new Palette()
				.add(cattail, 3).add(Blocks.SUGAR_CANE, 1).build(),
				Blocks.SAND, Blocks.GRAVEL, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.CLAY, Blocks.MUD));
		FeatureUtils.register(ctx, SHORE_SAND, Feature.RANDOM_PATCH, onGround(18, new Palette()
				.add(Blocks.SAND, 3).add(Blocks.GRAVEL, 2).add(Blocks.MUD, 2).add(Blocks.COARSE_DIRT, 1).build(),
				Blocks.SAND, Blocks.GRAVEL, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.CLAY));
		FeatureUtils.register(ctx, SHORE_FLOWERS, Feature.RANDOM_PATCH, onDirt(24, new Palette()
				.add(GLNaturalBlocks.BRACKEN, 3).add(Blocks.FERN, 2).add(Blocks.LILY_OF_THE_VALLEY, 2)
				.add(Blocks.OXEYE_DAISY, 2).add(Blocks.SHORT_GRASS, 2).build()));
		// paths are a feature and not a surface rule, so that they stay patchy and walkable instead of painting a biome
		FeatureUtils.register(ctx, SETTLEMENT_PATHS, Feature.RANDOM_PATCH, onDirt(14, new Palette()
				.add(Blocks.GRAVEL, 3).add(Blocks.COARSE_DIRT, 3).add(Blocks.COBBLESTONE, 2)
				.add(Blocks.MOSSY_COBBLESTONE, 1).build()));
		// tuff and gravel against a little dry scrub: the cliff is barer than the town beside it, not bare like the
		// summit above it, so the palette keeps a grass and a bush weight and leans on tuff for the rest
		FeatureUtils.register(ctx, CLIFF_SCREE, Feature.RANDOM_PATCH, onDirt(18, new Palette()
				.add(Blocks.GRAVEL, 3).add(Blocks.TUFF, 3).add(Blocks.COARSE_DIRT, 2)
				.add(Blocks.DEAD_BUSH, 1).add(Blocks.SHORT_GRASS, 1).build()));
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

	public static void placed(BootstrapContext<PlacedFeature> ctx) {
		var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);

		cover(ctx, PODZOL_PF, cf.getOrThrow(PODZOL_DISK), CountPlacement.of(3));
		cover(ctx, COARSE_DIRT_PF, cf.getOrThrow(COARSE_DIRT_DISK), CountPlacement.of(4));
		cover(ctx, MUD_PF, cf.getOrThrow(MUD_DISK), CountPlacement.of(3));
		cover(ctx, RAVINE_MUD_PF, cf.getOrThrow(RAVINE_MUD_DISK), CountPlacement.of(3));

		cover(ctx, STONES_PF, cf.getOrThrow(STONES), CountPlacement.of(2));
		cover(ctx, GRAVE_MARKER_PF, cf.getOrThrow(GRAVE_MARKER), RarityFilter.onAverageOnceEvery(4));
		cover(ctx, GHOST_FLOWERS_PF, cf.getOrThrow(GHOST_FLOWERS), CountPlacement.of(3));
		cover(ctx, VILLAGE_FLOWERS_PF, cf.getOrThrow(VILLAGE_FLOWERS), CountPlacement.of(4));
		cover(ctx, BARRENS_SCRUB_PF, cf.getOrThrow(BARRENS_SCRUB), CountPlacement.of(3));
		// denser than the village's four: the hill is the one biome named for being flowered rather than tilled, and
		// meadow's own sparser flower_meadow sits on top of it
		cover(ctx, HILL_FLOWERS_PF, cf.getOrThrow(HILL_FLOWERS), CountPlacement.of(7));
		cover(ctx, RAVINE_MOSS_PF, cf.getOrThrow(RAVINE_MOSS), CountPlacement.of(6));
		cover(ctx, FOOTHILL_MOSS_PF, cf.getOrThrow(FOOTHILL_MOSS), CountPlacement.of(4));
		cover(ctx, SANCTUARY_FLOWERS_PF, cf.getOrThrow(SANCTUARY_FLOWERS), CountPlacement.of(3));
		// two densities, not one: a noise count clumps the stalks into groves, and the rare light pass fills the gaps
		// between them. One count on its own reads as scattered poles rather than a bamboo forest.
		cover(ctx, BAMBOO_DENSE_PF, cf.getOrThrow(VegetationFeatures.BAMBOO_SOME_PODZOL),
				NoiseBasedCountPlacement.of(40, 24.0F, 0.3F));
		cover(ctx, BAMBOO_SPARSE_PF, cf.getOrThrow(VegetationFeatures.BAMBOO_NO_PODZOL),
				RarityFilter.onAverageOnceEvery(4));

		deposit(ctx, BLOOD_DEPOSIT_PF, cf.getOrThrow(BLOOD_DEPOSIT), CountPlacement.of(3));
		// The ravine's springs, over vanilla's own configured feature and its own height range, with the count
		// raised and nothing else touched. It replaces `SPRINGS` rather than joining them: the feature is in the
		// same step, so carrying vanilla's as well would place every spring twice and make the count a lie. The
		// ravine is wet and is left with no lava spring at all, neither its own nor the shared one — the water is
		// the whole of what a ravine has to say about fluid.
		PlacementUtils.register(ctx, RAVINE_SPRING_WATER_PF, cf.getOrThrow(MiscOverworldFeatures.SPRING_WATER),
				CountPlacement.of(75), InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(192)),
				BiomeFilter.biome());

		// A lava spring, and the reason it is its own placed feature rather than vanilla's `spring_lava`. The
		// overworld one will not place a single block in the hell biomes: its `valid_blocks` are stone, granite,
		// diorite, andesite, deepslate, tuff, calcite and dirt, and their floors are netherrack, blackstone, magma
		// and basalt. The nether spring's valid blocks are netherrack, soul sand, gravel, magma and blackstone,
		// which is two thirds of what the blazing fires stand on.
		PlacementUtils.register(ctx, BLAZING_HELL_LAVA_PF, cf.getOrThrow(NetherFeatures.SPRING_LAVA_NETHER),
				CountPlacement.of(6), InSquarePlacement.spread(),
				PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT, BiomeFilter.biome());
		// The pool's own placed feature. No heightmap: a cavern has no surface to place on, and putting it on the
		// world's is exactly the surface lake this is not. One attempt per 6 chunks: a cave system is a lot of
		// columns and the lowest room of each is a site, so an attempt every chunk fills one system with pools. The
		// range is the depth band former hell occupies on both of its rows — the ruins under the mountain and the
		// same ruins in the low ground — so the biome filter spends its attempts inside the biome instead of on the
		// mountain interior above it. The feature walks the column from there.
		PlacementUtils.register(ctx, FORMER_HELL_LAKE_PF, cf.getOrThrow(FORMER_HELL_LAKE),
				RarityFilter.onAverageOnceEvery(6), InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)),
				BiomeFilter.biome());

		// The lava pool's placed feature is the water one with nothing changed: the same one attempt in six chunks,
		// over the same depth band, because the two cold hells sit in the same rows and are cut by the same caves.
		PlacementUtils.register(ctx, ANIMAL_REALM_LAKE_PF, cf.getOrThrow(ANIMAL_REALM_LAKE),
				RarityFilter.onAverageOnceEvery(6), InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)),
				BiomeFilter.biome());
		deposit(ctx, BONE_PILE_PF, cf.getOrThrow(BONE_PILE), RarityFilter.onAverageOnceEvery(6));
		deposit(ctx, DEN_RUBBLE_PF, cf.getOrThrow(DEN_RUBBLE), RarityFilter.onAverageOnceEvery(5));
		deposit(ctx, FUNGUS_PATCH_PF, cf.getOrThrow(FUNGUS_PATCH), CountPlacement.of(4));
		deposit(ctx, FIRE_VENT_PF, cf.getOrThrow(FIRE_VENT), RarityFilter.onAverageOnceEvery(4));
		deposit(ctx, ASH_PATCH_PF, cf.getOrThrow(ASH_PATCH), CountPlacement.of(5));
		deposit(ctx, OLD_RUBBLE_PF, cf.getOrThrow(OLD_RUBBLE), RarityFilter.onAverageOnceEvery(4));

		// the only tree in the tail, and the only one that needs the dry-water check vanilla's own trees carry
		PlacementUtils.register(ctx, SANCTUARY_TREES_PF, cf.getOrThrow(TreeFeatures.CHERRY),
				RarityFilter.onAverageOnceEvery(5), InSquarePlacement.spread(),
				SurfaceWaterDepthFilter.forMaxDepth(0),
				PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());

		cover(ctx, LAKE_CLAY_PF, cf.getOrThrow(LAKE_CLAY), CountPlacement.of(2));
		cover(ctx, LAKE_CATTAILS_PF, cf.getOrThrow(LAKE_CATTAILS), RarityFilter.onAverageOnceEvery(3));
		// vanilla's lily pad *configured* feature, which already knows it needs water under it, behind a placed feature
		// of our own: a vanilla placed feature has a fixed position in vanilla's chains, and appending one to a tail is
		// a cycle waiting for a biome that already uses it mid-chain
		cover(ctx, LILY_PADS_PF, cf.getOrThrow(VegetationFeatures.PATCH_WATERLILY), RarityFilter.onAverageOnceEvery(3));
		cover(ctx, SHORE_SAND_PF, cf.getOrThrow(SHORE_SAND), CountPlacement.of(4));
		cover(ctx, SHORE_REEDS_PF, cf.getOrThrow(LAKE_CATTAILS), CountPlacement.of(3));
		cover(ctx, SHORE_FLOWERS_PF, cf.getOrThrow(SHORE_FLOWERS), CountPlacement.of(3));
		cover(ctx, SETTLEMENT_PATHS_PF, cf.getOrThrow(SETTLEMENT_PATHS), RarityFilter.onAverageOnceEvery(4));
		// sparse, where the settlement's moss covers four times over
		cover(ctx, CLIFF_SCREE_PF, cf.getOrThrow(CLIFF_SCREE), CountPlacement.of(1));
		cover(ctx, SUMMIT_SOIL_PF, cf.getOrThrow(SUMMIT_SOIL_DISK), CountPlacement.of(2));
		cover(ctx, SUMMIT_MOSS_PF, cf.getOrThrow(SUMMIT_MOSS), CountPlacement.of(4));
		cover(ctx, SUMMIT_SCREE_PF, cf.getOrThrow(SUMMIT_SCREE), CountPlacement.of(4));
		cover(ctx, SUMMIT_TREES_PF, cf.getOrThrow(TreeFeatures.PINE), RarityFilter.onAverageOnceEvery(16));
	}

	/**
	 * {@code count} patches of the feature, spread over the chunk and dropped on the terrain surface. The world surface
	 * heightmap is the one vanilla decorates with: it ignores fluids, so a patch never lands on a lily pad.
	 */
	private static void cover(BootstrapContext<PlacedFeature> ctx, ResourceKey<PlacedFeature> key,
							  Holder<ConfiguredFeature<?, ?>> feature, PlacementModifier count) {
		PlacementUtils.register(ctx, key, feature, count, InSquarePlacement.spread(),
				PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
	}

	/**
	 * The same, but scanning down for a cave floor rather than taking the terrain surface, which is how vanilla places
	 * its own cave decoration. The biome filter is what keeps these out of the open world: they only ever see a column
	 * whose biome is one of the underground ones.
	 */
	private static void deposit(BootstrapContext<PlacedFeature> ctx, ResourceKey<PlacedFeature> key,
							   Holder<ConfiguredFeature<?, ?>> feature, PlacementModifier count) {
		PlacementUtils.register(ctx, key, feature, count, InSquarePlacement.spread(),
				PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
				EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(),
						BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
				RandomOffsetPlacement.vertical(ConstantInt.of(1)), BiomeFilter.biome());
	}

	/** A flat patch of the given blocks resting on dirt, for the surface biomes' ground cover. */
	private static RandomPatchConfiguration onDirt(int tries, BlockStateProvider state) {
		return FeatureUtils.simpleRandomPatchConfiguration(tries, PlacementUtils.filtered(Feature.SIMPLE_BLOCK,
				new SimpleBlockConfiguration(state), BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
						BlockPredicate.matchesTag(Direction.DOWN.getNormal(), BlockTags.DIRT))));
	}

	/**
	 * The same, but on a named set of ground blocks. The Misty Lake pair needs it because the lake's surface rule makes
	 * the bed sand and gravel, neither of which is in {@link BlockTags#DIRT}.
	 */
	private static RandomPatchConfiguration onGround(int tries, BlockStateProvider state, Block... ground) {
		return FeatureUtils.simpleRandomPatchConfiguration(tries, PlacementUtils.filtered(Feature.SIMPLE_BLOCK,
				new SimpleBlockConfiguration(state), BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
						BlockPredicate.matchesBlocks(ground))));
	}

	/**
	 * The same on bare stone. Anything solid counts as ground here: a cave wall or a hell palette is not always one of
	 * the vanilla stones, and the biome filter has already ruled out the open world.
	 */
	private static RandomPatchConfiguration onStone(int tries, BlockStateProvider state) {
		return FeatureUtils.simpleRandomPatchConfiguration(tries, PlacementUtils.filtered(Feature.SIMPLE_BLOCK,
				new SimpleBlockConfiguration(state), BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
						BlockPredicate.solid())));
	}

	/**
	 * The weighted block list a ground cover draws from. It takes a plain {@link Block}, one of the mod's
	 * {@link BlockEntry}s, or a state whose properties are already set, so a palette reads as a list of blocks.
	 */
	private static final class Palette {

		private final SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();

		Palette add(Block block, int weight) {
			return add(block.defaultBlockState(), weight);
		}

		Palette add(BlockEntry<? extends Block> entry, int weight) {
			return add(entry.get().defaultBlockState(), weight);
		}

		Palette add(BlockState state, int weight) {
			builder.add(state, weight);
			return this;
		}

		BlockStateProvider build() {
			return new WeightedStateProvider(builder.build());
		}
	}

	private static ResourceKey<ConfiguredFeature<?, ?>> cf(String id) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, GensokyoLegacy.loc("gensokyo_biome/" + id));
	}

	private static ResourceKey<PlacedFeature> pf(String id) {
		return ResourceKey.create(Registries.PLACED_FEATURE, GensokyoLegacy.loc("gensokyo_biome/" + id));
	}

}