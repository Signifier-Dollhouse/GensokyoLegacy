package dev.xkmc.gensokyolegacy.init.data.biome;

import com.tterrag.registrate.providers.DataProviderInitializer;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.xkmc.gensokyolegacy.content.block.nature.FallenLeavesBlock;
import dev.xkmc.gensokyolegacy.content.dimension.GensokyoDimension;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.CanopyFilter;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.NetherPlacements;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Music;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One Gensokyo biome's whole registration — its key, its features and its climate — in one class of its own.
 * <p>
 * A biome is a subclass of this and nothing else, and {@link #build} registers every instance in one go, the way
 * {@code QuestDialogData} does for dialogs. Nothing a single biome owns lives here: two branches editing two different
 * biomes touch two different files and neither ever sees the other's diff.
 * <p>
 * What does live here is what several biomes share, and nothing else. The vanilla runs are named by what they put
 * down, {@link Layers} writes a biome's steps out, the {@link Registrar} is where a biome's placed features go, and
 * the biome factory overloads build the {@link Biome} itself. A biome's own feature tail is named in the biome.
 * <p>
 * A biome's features are <em>written out</em>, step by step, in the class that registers it. Nothing is copied from a
 * vanilla biome any more: that used to mean a sculk vein and a monster room arrived wholesale in a Gensokyo biome
 * without anyone asking for them, and the only way to read or change a biome's list was to know which vanilla biome it
 * happened to be standing on. A vanilla name now appears in a comment as the reason for a choice, not as the source
 * of the list.
 * <p>
 * No biome spawns a mob. Natural spawning in Gensokyo belongs to the mod's own entities, added by biome modifier, not
 * to a vanilla spawn table copied in beside them.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public abstract class AbstractBiomeGen {

	/**
	 * The two carvers every Gensokyo biome carves with, which are the overworld's own, unchanged. {@code minecraft:canyon}
	 * is in none of them: a canyon is a surface landform, and Gensokyo's ravine and valley are already its own thing.
	 */
	protected static final List<ResourceKey<ConfiguredWorldCarver<?>>> CARVERS =
			List.of(Carvers.CAVE, Carvers.CAVE_EXTRA_UNDERGROUND);

	/**
	 * The overworld ore set, in vanilla's order: the dirt and gravel, the three stone mixes and tuff, then everything
	 * worth digging for. Every Gensokyo biome carries it, underground ones included, because a dimension whose only
	 * ore is what the nether places is not a dimension anyone can mine.
	 */
	protected static final List<ResourceKey<PlacedFeature>> OVERWORLD_ORES = List.of(
			OrePlacements.ORE_DIRT, OrePlacements.ORE_GRAVEL,
			OrePlacements.ORE_GRANITE_UPPER, OrePlacements.ORE_GRANITE_LOWER,
			OrePlacements.ORE_DIORITE_UPPER, OrePlacements.ORE_DIORITE_LOWER,
			OrePlacements.ORE_ANDESITE_UPPER, OrePlacements.ORE_ANDESITE_LOWER,
			OrePlacements.ORE_TUFF, OrePlacements.ORE_COAL_UPPER, OrePlacements.ORE_COAL_LOWER,
			OrePlacements.ORE_IRON_UPPER, OrePlacements.ORE_IRON_MIDDLE, OrePlacements.ORE_IRON_SMALL,
			OrePlacements.ORE_GOLD, OrePlacements.ORE_GOLD_LOWER,
			OrePlacements.ORE_REDSTONE, OrePlacements.ORE_REDSTONE_LOWER,
			OrePlacements.ORE_DIAMOND, OrePlacements.ORE_DIAMOND_MEDIUM, OrePlacements.ORE_DIAMOND_LARGE,
			OrePlacements.ORE_DIAMOND_BURIED, OrePlacements.ORE_LAPIS, OrePlacements.ORE_LAPIS_BURIED,
			OrePlacements.ORE_COPPER, CavePlacements.UNDERWATER_MAGMA);

	/** The three soil disks the surface biomes lay on their floor, which is why Gensokyo has sand, clay and gravel. */
	protected static final List<ResourceKey<PlacedFeature>> OVERWORLD_DISKS = List.of(
			MiscOverworldPlacements.DISK_SAND, MiscOverworldPlacements.DISK_CLAY, MiscOverworldPlacements.DISK_GRAVEL);

	/** Lava lakes. The one piece of overworld surface furniture the mountain's own rock keeps. */
	protected static final List<ResourceKey<PlacedFeature>> LAVA_LAKES = List.of(
			MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND, MiscOverworldPlacements.LAKE_LAVA_SURFACE);

	/** Water and lava in the rock, which is how every Gensokyo cave gets a pool to walk down to. */
	protected static final List<ResourceKey<PlacedFeature>> SPRINGS = List.of(
			MiscOverworldPlacements.SPRING_WATER, MiscOverworldPlacements.SPRING_LAVA);

	/** Emerald only spawns in a mountain, so only the mountain's biomes get it. */
	protected static final List<ResourceKey<PlacedFeature>> EMERALD = List.of(OrePlacements.ORE_EMERALD);

	/** Infested stone, the other half of what makes the mountain's rock read as a mountain and not as a cave. */
	protected static final List<ResourceKey<PlacedFeature>> INFESTED = List.of(OrePlacements.ORE_INFESTED);

	/** The snow line is nowhere near any of these, but the step runs anyway and vanilla's rule is the rule. */
	protected static final List<ResourceKey<PlacedFeature>> FREEZE_TOP_LAYER =
			List.of(MiscOverworldPlacements.FREEZE_TOP_LAYER);

	/**
	 * The six features every nether biome shares, and the whole of what the four underground biomes agree on: fire,
	 * soul fire, two glowstone runs and the two nether mushrooms.
	 * <p>
	 * Deliberately absent from all of them are the nether <em>ores</em> — magma, blackstone, quartz, ancient debris.
	 * Their placement is written against the nether's own 0…128 column, so copied verbatim they land at y 5…41 and
	 * miss the underground biomes entirely, which is not where the rock is. The overworld ore set above replaces them.
	 * Also absent are the nether springs, which flood a cave system with lava.
	 */
	protected static final List<ResourceKey<PlacedFeature>> NETHER_COMMON = List.of(
			NetherPlacements.PATCH_FIRE, NetherPlacements.PATCH_SOUL_FIRE,
			NetherPlacements.GLOWSTONE_EXTRA, NetherPlacements.GLOWSTONE,
			VegetationPlacements.BROWN_MUSHROOM_NETHER, VegetationPlacements.RED_MUSHROOM_NETHER);

	/** The ground cover the overworld plains and forests put on top of Gensokyo's grass, which the tail then adds to. */
	protected static final List<ResourceKey<PlacedFeature>> PLAINS_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.PATCH_TALL_GRASS_2, VegetationPlacements.TREES_PLAINS,
			VegetationPlacements.FLOWER_PLAINS, VegetationPlacements.PATCH_GRASS_PLAIN,
			VegetationPlacements.BROWN_MUSHROOM_NORMAL, VegetationPlacements.RED_MUSHROOM_NORMAL,
			VegetationPlacements.PATCH_SUGAR_CANE, VegetationPlacements.PATCH_PUMPKIN);

	protected static final List<ResourceKey<PlacedFeature>> FOREST_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.FOREST_FLOWERS,
			VegetationPlacements.TREES_BIRCH_AND_OAK, VegetationPlacements.FLOWER_DEFAULT,
			VegetationPlacements.PATCH_GRASS_FOREST, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
			VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE,
			VegetationPlacements.PATCH_PUMPKIN);

	protected static final List<ResourceKey<PlacedFeature>> TAIGA_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.PATCH_LARGE_FERN, VegetationPlacements.TREES_TAIGA,
			VegetationPlacements.FLOWER_DEFAULT, VegetationPlacements.PATCH_GRASS_TAIGA_2,
			VegetationPlacements.BROWN_MUSHROOM_TAIGA, VegetationPlacements.RED_MUSHROOM_TAIGA,
			VegetationPlacements.PATCH_SUGAR_CANE, VegetationPlacements.PATCH_PUMPKIN);

	protected static final List<ResourceKey<PlacedFeature>> MEADOW_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.PATCH_TALL_GRASS_2,
			VegetationPlacements.PATCH_GRASS_PLAIN, VegetationPlacements.FLOWER_MEADOW,
			VegetationPlacements.TREES_MEADOW);

	protected static final List<ResourceKey<PlacedFeature>> BADLANDS_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.PATCH_GRASS_BADLANDS,
			VegetationPlacements.PATCH_DEAD_BUSH_BADLANDS, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
			VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE_BADLANDS,
			VegetationPlacements.PATCH_PUMPKIN, VegetationPlacements.PATCH_CACTUS_DECORATED);

	protected static final List<ResourceKey<PlacedFeature>> SWAMP_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.TREES_SWAMP, VegetationPlacements.FLOWER_SWAMP,
			VegetationPlacements.PATCH_GRASS_NORMAL, VegetationPlacements.PATCH_DEAD_BUSH,
			VegetationPlacements.PATCH_WATERLILY, VegetationPlacements.BROWN_MUSHROOM_SWAMP,
			VegetationPlacements.RED_MUSHROOM_SWAMP, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
			VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE_SWAMP,
			VegetationPlacements.PATCH_PUMPKIN, AquaticPlacements.SEAGRASS_SWAMP);

	/**
	 * Lush caves' seven, in {@code BiomeDefaultFeatures.addLushCavesVegetationFeatures}'s order, which is the order the
	 * feature sorter reads. {@code genbu_ravine} is the one surface biome that takes them, and it takes them whole —
	 * the floor growth, the two vine sets, the ceiling growth and the rooted azalea all hang off the same cave
	 * surfaces, so taking some of the seven would be taking a fragment of one decoration rather than a style.
	 *
	 * @see "doc/design/gensokyo_biomes.md" §4
	 */
	protected static final List<ResourceKey<PlacedFeature>> LUSH_CAVES = List.of(
			CavePlacements.LUSH_CAVES_CEILING_VEGETATION, CavePlacements.CAVE_VINES,
			CavePlacements.LUSH_CAVES_CLAY, CavePlacements.LUSH_CAVES_VEGETATION,
			CavePlacements.ROOTED_AZALEA_TREE, CavePlacements.SPORE_BLOSSOM, CavePlacements.CLASSIC_VINES);

	/**
	 * Lush caves' one special ore, {@code BiomeDefaultFeatures.addLushCavesSpecialOres}. It goes in the soft-ore step
	 * between the default ore set and the soft disks, which is where vanilla writes it.
	 */
	protected static final List<ResourceKey<PlacedFeature>> LUSH_CAVES_ORES = List.of(OrePlacements.ORE_CLAY);

	/** The windswept pair, which the cliff and the foothills both take; the badlands ground is all either of them has. */
	protected static final List<ResourceKey<PlacedFeature>> CLIFF_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.TREES_WINDSWEPT_HILLS,
			VegetationPlacements.FLOWER_DEFAULT, VegetationPlacements.PATCH_GRASS_BADLANDS,
			VegetationPlacements.BROWN_MUSHROOM_NORMAL, VegetationPlacements.RED_MUSHROOM_NORMAL,
			VegetationPlacements.PATCH_SUGAR_CANE, VegetationPlacements.PATCH_PUMPKIN);

	/**
	 * The same three runs with their tree placement taken back out, which is what the maple groves take: the mod's
	 * own canopy is not an accent on top of the vanilla one but the reason the biome reads as a maple grove at all.
	 * How many maples it gets is the biome's business and goes in its own placed feature.
	 */
	protected static final List<ResourceKey<PlacedFeature>> FOREST_FLOOR =
			noTrees(FOREST_GROUND, VegetationPlacements.TREES_BIRCH_AND_OAK);
	protected static final List<ResourceKey<PlacedFeature>> TAIGA_FLOOR =
			noTrees(TAIGA_GROUND, VegetationPlacements.TREES_TAIGA);
	protected static final List<ResourceKey<PlacedFeature>> CLIFF_FLOOR =
			noTrees(CLIFF_GROUND, VegetationPlacements.TREES_WINDSWEPT_HILLS);

	/**
	 * The snow line, which nothing here is allowed to reach: {@link Biome#shouldSnow} and {@link Biome#shouldFreeze}
	 * both ask for {@code getTemperature(pos) >= 0.15}, and {@code getTemperature} subtracts up to 0.19 above y 80 for
	 * altitude. Every biome registered from this class clears 0.15 after that.
	 */
	public static final float coldEnoughToSnow = 0.15f;

	/** The key this biome registers under, and the one the climate grid, the tags and the structures ask for. */
	public final ResourceKey<Biome> key;

	/** The biome's placed features in the order it registered them; see {@link Registrar}. */
	private final List<ResourceKey<PlacedFeature>> placed = new ArrayList<>();

	protected AbstractBiomeGen(ResourceKey<Biome> key) {
		this.key = key;
	}

	/**
	 * Registers every instance's biome, features and noise. One bootstrap per registry, as vanilla does, so the whole
	 * set is visible in {@code GensokyoLegacy#gatherData} as one list of biomes.
	 */
	public static void build(DataProviderInitializer init, AbstractBiomeGen... all) {
		init.add(Registries.BIOME, ctx -> {
			for (var biome : all) {
				ctx.register(biome.key, biome.build(ctx));
			}
		});
		init.add(Registries.CONFIGURED_FEATURE, ctx -> {
			// the mod's own trees and mushrooms first: the magical forest's templates and the bamboo are what grow them
			VegetationFeatureGen.configured(ctx);
			GensokyoBiomeFeatures.configured(ctx);
			for (var biome : all) {
				biome.configured(ctx);
			}
		});
		init.add(Registries.PLACED_FEATURE, ctx -> {
			var features = ctx.lookup(Registries.CONFIGURED_FEATURE);
			var orders = new ArrayList<List<ResourceKey<PlacedFeature>>>();
			var shared = new ArrayList<ResourceKey<PlacedFeature>>();
			orders.add(shared);
			GensokyoBiomeFeatures.placed(new Registrar(ctx, features, shared));
			for (var biome : all) {
				biome.placed(new Registrar(ctx, features, biome.placed));
				orders.add(biome.placed);
			}
			checkOrder(orders);
		});
		// GensokyoDimension reuses this one for its finer climate noise parameters
		init.add(Registries.NOISE, ctx -> {
			for (var biome : all) {
				biome.noise(ctx);
			}
			GensokyoDimension.noises(ctx);
		});
	}

	/** The biome itself, written out step by step. */
	protected abstract Biome build(BootstrapContext<Biome> ctx);

	/** The biome's own configured features. Anything several biomes place lives in {@link GensokyoBiomeFeatures}. */
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {}

	/** The biome's own placed features, in the order they must generate. */
	protected void placed(Registrar reg) {}

	/** The biome's own noise, for its surface rules and its custom placement. */
	protected void noise(BootstrapContext<NormalNoise.NoiseParameters> ctx) {}

	protected Layers layers(BootstrapContext<Biome> ctx) {
		return new Layers(ctx.lookup(Registries.PLACED_FEATURE), ctx.lookup(Registries.CONFIGURED_CARVER));
	}

	/**
	 * The same lookups without the two carvers and without the step names, for the two biomes that take vanilla's own
	 * {@code addDefaultOres} chain instead of writing their steps out.
	 */
	protected BiomeGenerationSettings.Builder raw(BootstrapContext<Biome> ctx) {
		return new BiomeGenerationSettings.Builder(ctx.lookup(Registries.PLACED_FEATURE),
				ctx.lookup(Registries.CONFIGURED_CARVER));
	}

	/**
	 * Checks that the per-biome orders can all be read as a subsequence of one order, which is what a shared feature
	 * being placed consistently across biomes comes down to.
	 * <p>
	 * {@code FeatureSorter} links biomes by the order features appear in, and a cycle between two of them crashes
	 * chunk generation at runtime rather than at datagen. A biome never lists a vanilla placed feature in its own
	 * order at all — those keep vanilla's own position — so this covers the custom features, where appending a
	 * whole tail after the vanilla chain is what keeps a custom feature from landing mid-vanilla-chain. Two biomes
	 * disagreeing about the order of a feature they share is the one way left to reintroduce a cycle.
	 * <p>
	 * The merged order itself is not used; that one exists is the whole of what this asserts.
	 */
	private static void checkOrder(List<List<ResourceKey<PlacedFeature>>> orders) {
		Map<ResourceKey<PlacedFeature>, Set<ResourceKey<PlacedFeature>>> after = new LinkedHashMap<>();
		for (var order : orders) {
			for (var key : order) {
				after.computeIfAbsent(key, k -> new LinkedHashSet<>());
			}
			for (int i = 0; i + 1 < order.size(); i++) {
				after.get(order.get(i)).add(order.get(i + 1));
			}
		}
		var remaining = new LinkedHashMap<>(after);
		var merged = new LinkedHashSet<ResourceKey<PlacedFeature>>();
		while (!remaining.isEmpty()) {
			List<ResourceKey<PlacedFeature>> ready = new ArrayList<>();
			for (var key : remaining.keySet()) {
				if (merged.containsAll(after.get(key))) {
					ready.add(key);
				}
			}
			if (ready.isEmpty()) {
				throw new IllegalArgumentException("feature order cycle among " + remaining.keySet());
			}
			merged.addAll(ready);
			remaining.keySet().removeAll(ready);
		}
	}

	/**
	 * Where a biome's placed features go. Every one of them is recorded in the order it was registered, which is the
	 * only record {@link #checkOrder} has of the order a biome's features must appear in.
	 */
	public static final class Registrar {

		private final BootstrapContext<PlacedFeature> ctx;
		private final HolderGetter<ConfiguredFeature<?, ?>> configured;
		private final List<ResourceKey<PlacedFeature>> order;

		private Registrar(BootstrapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> configured,
						  List<ResourceKey<PlacedFeature>> order) {
			this.ctx = ctx;
			this.configured = configured;
			this.order = order;
		}

		public Holder<ConfiguredFeature<?, ?>> feature(ResourceKey<ConfiguredFeature<?, ?>> key) {
			return configured.getOrThrow(key);
		}

		public void placed(ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> feature,
						   PlacementModifier... modifiers) {
			placed(key, feature, List.of(modifiers));
		}

		public void placed(ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> feature,
						   List<PlacementModifier> modifiers) {
			PlacementUtils.register(ctx, key, feature, modifiers);
			order.add(key);
		}

		/** The bootstrap context the features are registered into, for the few lookups a placement modifier needs. */
		public BootstrapContext<PlacedFeature> ctx() {
			return ctx;
		}

		/**
		 * {@code count} patches of the feature, spread over the chunk and dropped on the terrain surface. The world
		 * surface heightmap is the one vanilla decorates with: it ignores fluids, so a patch never lands on a lily pad.
		 */
		public void cover(ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> feature,
						  PlacementModifier count) {
			placed(key, feature, count, InSquarePlacement.spread(),
					PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
		}

		/**
		 * Ground that goes under a canopy rather than on it. {@code HEIGHTMAP_WORLD_SURFACE} counts leaves, so once
		 * the biome's trees are up it returns the crown and anything placed with it lands on the roof; the no-leaves
		 * heightmap returns the floor instead, which is both where a standing deco belongs and the only height at
		 * which the block underneath is the ground the biome's surface rule actually laid down.
		 */
		public void floor(ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> feature,
						  PlacementModifier count) {
			placed(key, feature, count, InSquarePlacement.spread(),
					HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES), BiomeFilter.biome());
		}

		/**
		 * A tree run: {@code count} of the feature's trees, spread over the chunk and rooted on the terrain surface.
		 * The water filter is the one vanilla's own tree placements carry, and it is what keeps a grove out of a lake.
		 */
		public void trees(ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> feature,
						  PlacementModifier count) {
			placed(key, feature, count, InSquarePlacement.spread(),
					SurfaceWaterDepthFilter.forMaxDepth(0), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
		}

		/**
		 * A carpet of fallen leaves on the floor under a canopy. The no-leaves heightmap is what puts it under the
		 * crowns rather than on top of them, and the canopy filter drops the positions that turned out to be open sky.
		 */
		public void litter(ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> feature,
						   PlacementModifier count) {
			placed(key, feature, count, InSquarePlacement.spread(), new CanopyFilter(4, true),
					HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES), BiomeFilter.biome());
		}

		/**
		 * The same, but scanning down for a cave floor rather than taking the terrain surface, which is how vanilla
		 * places its own cave decoration. The biome filter is what keeps these out of the open world: they only ever
		 * see a column whose biome is one of the underground ones.
		 */
		public void deposit(ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> feature,
							PlacementModifier count) {
			placed(key, feature, count, InSquarePlacement.spread(),
					PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
					EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(),
							BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
					RandomOffsetPlacement.vertical(ConstantInt.of(1)), BiomeFilter.biome());
		}

		/** A feature placed inline by a configured feature, which the sorter never sees and so never orders. */
		public <FC extends FeatureConfiguration> Holder<PlacedFeature> inline(Feature<FC> feature, FC config) {
			return PlacementUtils.inlinePlaced(feature, config);
		}
	}

	/**
	 * One biome's features, written out step by step.
	 * <p>
	 * Nothing is inherited, so this is the whole of what a biome generates: the two carvers are the only thing set up
	 * for it, and every other step is added by name, in the order the step should generate. A step left alone is a
	 * step that does not happen.
	 */
	public static class Layers extends BiomeGenerationSettings.Builder {

		private Layers(HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
			super(pf, carvers);
			for (var carver : CARVERS) {
				addCarver(GenerationStep.Carving.AIR, carver);
			}
		}

		private Layers add(GenerationStep.Decoration step, List<ResourceKey<PlacedFeature>> features) {
			for (var feature : features) {
				addFeature(step, feature);
			}
			return this;
		}

		public Layers localMods(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.LOCAL_MODIFICATIONS, features);
		}

		public Layers lakes(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.LAKES, features);
		}

		public Layers structures(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.UNDERGROUND_STRUCTURES, features);
		}

		public Layers surfaceStructures(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.SURFACE_STRUCTURES, features);
		}

		public Layers ores(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.UNDERGROUND_ORES, features);
		}

		public Layers underground(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.UNDERGROUND_DECORATION, features);
		}

		public Layers springs(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.FLUID_SPRINGS, features);
		}

		public Layers vegetation(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.VEGETAL_DECORATION, features);
		}

		public Layers topLayer(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, features);
		}
	}

	/** A flat patch of the given blocks resting on dirt, for the surface biomes' ground cover. */
	protected static RandomPatchConfiguration onDirt(int tries, BlockStateProvider state) {
		return patch(tries, 7, 3, state, Feature.SIMPLE_BLOCK);
	}

	/**
	 * The same, drawn by {@code feature} rather than by vanilla's own simple block — which is what the standing decos
	 * need, since each of them is two blocks tall.
	 * <p>
	 * The y spread wants naming, and the standing decos want it at 0. Vanilla's 3 is the difference of two rolls over
	 * {@code y_spread + 1}, so only a quarter of the tries land back on the terrain surface; the rest are spent at a
	 * height where the {@code BlockTags.DIRT} below is not the block the heightmap promised, and the try is thrown
	 * away. A patch of 24 grass can afford that. A patch of two gravestones cannot.
	 * <p>
	 * The decos keep the dirt tag on purpose rather than widening it to any solid block. Leaves are not in
	 * {@code BlockTags.DIRT}, so a deco placed after the biome's trees is simply refused on a canopy instead of
	 * standing on top of one — which is what the tag is for here.
	 */
	protected static RandomPatchConfiguration patch(int tries, int spread, int ySpread, BlockStateProvider state,
													Feature<SimpleBlockConfiguration> feature) {
		return new RandomPatchConfiguration(tries, spread, ySpread, PlacementUtils.filtered(feature,
				new SimpleBlockConfiguration(state), BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
						BlockPredicate.matchesTag(Direction.DOWN.getNormal(), BlockTags.DIRT))));
	}

	/**
	 * A carpet of fallen leaves: every species listed, one layer three times as often as two, on dirt.
	 * <p>
	 * Weighted towards the single layer because a carpet that is two blocks tall everywhere reads as snow rather than
	 * as litter, and a patch places a lot of them side by side. Each cell also draws its own facing, so a carpet reads
	 * as litter rather than as one model tiled.
	 */
	protected static RandomPatchConfiguration fallenLeaves(int tries, int spread,
														   BlockEntry<FallenLeavesBlock>... species) {
		var leaves = SimpleWeightedRandomList.<BlockState>builder();
		for (var entry : species) {
			var state = entry.get().defaultBlockState();
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				var turned = state.setValue(FallenLeavesBlock.FACING, facing);
				leaves.add(turned.setValue(FallenLeavesBlock.LAYERS, 1), 3);
				leaves.add(turned.setValue(FallenLeavesBlock.LAYERS, 2), 1);
			}
		}
		return patch(tries, spread, 3, new WeightedStateProvider(leaves.build()), Feature.SIMPLE_BLOCK);
	}

	/**
	 * The same, but on a named set of ground blocks. The Misty Lake pair needs it because the lake's surface rule makes
	 * the bed sand and gravel, neither of which is in {@link BlockTags#DIRT}.
	 */
	protected static RandomPatchConfiguration onGround(int tries, BlockStateProvider state, Block... ground) {
		return FeatureUtils.simpleRandomPatchConfiguration(tries, PlacementUtils.filtered(Feature.SIMPLE_BLOCK,
				new SimpleBlockConfiguration(state), BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
						BlockPredicate.matchesBlocks(ground))));
	}

	/**
	 * The same on bare stone. Anything solid counts as ground here: a cave wall or a hell palette is not always one of
	 * the vanilla stones, and the biome filter has already ruled out the open world.
	 */
	protected static RandomPatchConfiguration onStone(int tries, BlockStateProvider state) {
		return FeatureUtils.simpleRandomPatchConfiguration(tries, PlacementUtils.filtered(Feature.SIMPLE_BLOCK,
				new SimpleBlockConfiguration(state), BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
						BlockPredicate.solid())));
	}

	/**
	 * The weighted block list a ground cover draws from. It takes a plain {@link Block}, one of the mod's
	 * {@link BlockEntry}s, or a state whose properties are already set, so a palette reads as a list of blocks.
	 */
	protected static final class Palette {

		private final SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();

		public Palette() {}

		public Palette add(Block block, int weight) {
			return add(block.defaultBlockState(), weight);
		}

		public Palette add(BlockEntry<? extends Block> entry, int weight) {
			return add(entry.get().defaultBlockState(), weight);
		}

		public Palette add(BlockState state, int weight) {
			builder.add(state, weight);
			return this;
		}

		public BlockStateProvider build() {
			return new WeightedStateProvider(builder.build());
		}
	}

	/** {@code List.of} chains, for the places a biome takes a shared run and then adds its own to it. */
	@SafeVarargs
	protected static <T> List<T> join(List<T>... lists) {
		return Arrays.stream(lists).flatMap(List::stream).toList();
	}

	/** A shared ground-cover run with the tree placements named taken back out, for a biome that plants its own. */
	private static List<ResourceKey<PlacedFeature>> noTrees(List<ResourceKey<PlacedFeature>> ground,
			ResourceKey<PlacedFeature>... trees) {
		var drop = List.of(trees);
		return ground.stream().filter(k -> !drop.contains(k)).toList();
	}

	protected static ResourceKey<Biome> biome(String id) {
		return ResourceKey.create(Registries.BIOME, GensokyoLegacy.loc(id));
	}

	protected static ResourceKey<ConfiguredFeature<?, ?>> cf(String id) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, GensokyoLegacy.loc("gensokyo_biome/" + id));
	}

	protected static ResourceKey<PlacedFeature> pf(String id) {
		return ResourceKey.create(Registries.PLACED_FEATURE, GensokyoLegacy.loc("gensokyo_biome/" + id));
	}

	protected static BiomeGenerationSettings.Builder addDefaultOres(BiomeGenerationSettings.Builder builder) {
		BiomeDefaultFeatures.addDefaultOres(builder);
		return builder;
	}

	/**
	 * The one every surface and underground biome uses: water colour, fog colour and the two grass overrides.
	 * The special effects are otherwise ours, which is why temperature alone sets the sky.
	 */
	protected static Biome biome(boolean hasPrecipitation, float temperature, float downfall,
							   int waterColor, int fogColor,
							   @Nullable Integer grassCol, @Nullable Integer foliageCol,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen,
							   @Nullable Music bgm
	) {
		return biome(hasPrecipitation, temperature, downfall, waterColor, 329011, fogColor,
				null, grassCol, foliageCol, spawns, gen, bgm);
	}

	/** The same without the music, which is every Gensokyo biome: its soundtrack is the dimension's business. */
	protected static Biome biome(boolean hasPrecipitation, float temperature, float downfall,
							   int waterColor, int fogColor,
							   @Nullable Integer grassCol, @Nullable Integer foliageCol,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen
	) {
		return biome(hasPrecipitation, temperature, downfall, waterColor, 329011, fogColor,
				null, grassCol, foliageCol, spawns, gen, null);
	}

	/**
	 * The same with the sky colour spelled out, for {@code sunflower_plains} alone. Every other biome lets the
	 * temperature set the sky, which is why this one is the only caller: it is the biome that keeps vanilla's own
	 * numbers rather than being a Gensokyo biome that happens to be grassy.
	 */
	protected static Biome biome(boolean hasPrecipitation, float temperature, float downfall,
							   int waterColor, int fogColor, int skyColor,
							   @Nullable Integer grassCol, @Nullable Integer foliageCol,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen
	) {
		return biome(hasPrecipitation, temperature, downfall, waterColor, 329011, fogColor,
				skyColor, grassCol, foliageCol, spawns, gen, null);
	}

	/** The end dimension, which is the one biome that is not Gensokyo at all. */
	protected static Biome biome(int fogColor,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen,
							   @Nullable Music bgm
	) {
		return biome(false, 2, 0, fogColor, spawns, gen, bgm);
	}

	protected static Biome biome(boolean hasPrecipitation, float temperature, float downfall, int fogColor,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen,
							   @Nullable Music bgm
	) {
		return biome(hasPrecipitation, temperature, downfall, 4159204, 329011, fogColor,
				null, null, null, spawns, gen, bgm);
	}

	/** The magical forest's own climate: the only Gensokyo biome that names no guide cell. */
	protected static Biome biome(MobSpawnSettings.Builder spawns, BiomeGenerationSettings.PlainBuilder gen) {
		return biome(true, 0.7f, 0.8f, 0x4b6fad, 329011, 0x8facf3, 0x4770c1, 0x418e63, 0x4e9465, spawns, gen, null);
	}

	protected static Biome biome(boolean hasPrecipitation, float temperature, float downfall,
							   int fogColor, @Nullable Integer grassCol, @Nullable Integer foliageCol,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen,
							   @Nullable Music bgm
	) {
		return biome(hasPrecipitation, temperature, downfall, 4159204, 329011, fogColor,
				null, grassCol, foliageCol, spawns, gen, bgm);
	}

	protected static Biome biome(boolean hasPrecipitation, float temperature, float downfall,
							   int waterColor, int waterFogColor, int fogColor,
							   @Nullable Integer skyCol,
							   @Nullable Integer grassCol, @Nullable Integer foliageCol,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen,
							   @Nullable Music bgm
	) {
		BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
				.waterColor(waterColor)
				.waterFogColor(waterFogColor)
				.fogColor(fogColor);
		if (skyCol != null) {
			effects.skyColor(skyCol);
		} else {
			effects.skyColor(calculateSkyColor(temperature));
		}
		effects.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
				.backgroundMusic(bgm);
		if (grassCol != null) {
			effects.grassColorOverride(grassCol);
		}
		if (foliageCol != null) {
			effects.foliageColorOverride(foliageCol);
		}
		return new Biome.BiomeBuilder()
				.hasPrecipitation(hasPrecipitation)
				.temperature(temperature)
				.downfall(downfall)
				.specialEffects(effects.build())
				.mobSpawnSettings(spawns.build())
				.generationSettings(gen.build())
				.build();
	}

	protected static int calculateSkyColor(float temperature) {
		float f = Mth.clamp(temperature / 3.0F, -1.0F, 1.0F);
		return Mth.hsvToRgb(0.62222224F - f * 0.05F, 0.5F + f * 0.1F, 1.0F);
	}

}