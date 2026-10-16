package dev.xkmc.gensokyolegacy.init.data.biome;

import com.tterrag.registrate.util.entry.BlockEntry;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
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
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;

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
	private static final ResourceKey<PlacedFeature> FIRE_VENT_PF = pf("fire_vent");
	private static final ResourceKey<PlacedFeature> ASH_PATCH_PF = pf("ash_patch");
	private static final ResourceKey<PlacedFeature> OLD_RUBBLE_PF = pf("old_rubble");

	/**
	 * The one order the custom placed features may appear in. A biome's tail must be a subsequence of this list, never
	 * a reordering of it.
	 */
	private static final List<ResourceKey<PlacedFeature>> ORDER = List.of(
			PODZOL_PF, COARSE_DIRT_PF, MUD_PF, RAVINE_MUD_PF,
			STONES_PF, GRAVE_MARKER_PF, GHOST_FLOWERS_PF, VILLAGE_FLOWERS_PF, BARRENS_SCRUB_PF,
			HILL_FLOWERS_PF, BAMBOO_DENSE_PF, BAMBOO_SPARSE_PF, RAVINE_MOSS_PF, FOOTHILL_MOSS_PF,
			SANCTUARY_TREES_PF, SANCTUARY_FLOWERS_PF,
			BLOOD_DEPOSIT_PF, BONE_PILE_PF, DEN_RUBBLE_PF, FUNGUS_PATCH_PF, FIRE_VENT_PF, ASH_PATCH_PF, OLD_RUBBLE_PF);

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
		cover(ctx, HILL_FLOWERS_PF, cf.getOrThrow(HILL_FLOWERS), CountPlacement.of(4));
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