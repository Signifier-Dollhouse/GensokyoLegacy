package dev.xkmc.gensokyolegacy.content.dimension;

import com.mojang.datafixers.util.Pair;
import com.tterrag.registrate.providers.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.GLBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * The Gensokyo dimension: an overworld clone that only grows a handpicked set of biomes.
 * <p>
 * Terrain comes from {@code gensokyolegacy:gensokyo}, a copy of the overworld noise settings, so heightmaps, caves,
 * ore veins, sea level and surface rules stay identical to the overworld. Only the biome source differs: it is the
 * vanilla overworld climate grid with every biome swapped for an allowed one, which keeps the continental / erosion /
 * weirdness layout recognizable while dropping biomes that do not belong here.
 */
public class GensokyoDimension {

	public static final ResourceKey<DimensionType> DT_GENSOKYO = ResourceKey.create(Registries.DIMENSION_TYPE, GensokyoLegacy.loc("gensokyo"));
	public static final ResourceKey<NoiseGeneratorSettings> NS_GENSOKYO = ResourceKey.create(Registries.NOISE_SETTINGS, GensokyoLegacy.loc("gensokyo"));
	public static final ResourceKey<LevelStem> LEVEL_GENSOKYO = ResourceKey.create(Registries.LEVEL_STEM, GensokyoLegacy.loc("gensokyo"));
	public static final ResourceKey<Level> GENSOKYO = Registries.levelStemToLevel(LEVEL_GENSOKYO);

	/**
	 * Vanilla overworld biome -> biome used in Gensokyo. Every overworld biome needs an entry.
	 */
	private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> BIOMES = new LinkedHashMap<>();

	/**
	 * How much a too wide climate cell is penalized when picking the cell that represents a biome.
	 */
	private static final double WIDTH_PENALTY = 1.0;

	static {
		// water
		allow(Biomes.OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_OCEAN,
				Biomes.DEEP_LUKEWARM_OCEAN, Biomes.FROZEN_OCEAN, Biomes.COLD_OCEAN, Biomes.OCEAN,
				Biomes.LUKEWARM_OCEAN, Biomes.WARM_OCEAN);
		allow(Biomes.BEACH, Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE);
		allow(Biomes.PLAINS, Biomes.PLAINS, Biomes.RIVER, Biomes.DRIPSTONE_CAVES);
		allow(Biomes.SNOWY_PLAINS, Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.FROZEN_RIVER, Biomes.SNOWY_TAIGA,
				Biomes.GROVE);
		// dry
		allow(Biomes.DESERT, Biomes.DESERT, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);
		allow(Biomes.SAVANNA, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA);
		// temperate
		allow(Biomes.SUNFLOWER_PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.FLOWER_FOREST);
		allow(Biomes.FOREST, Biomes.FOREST, Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA);
		allow(GLBiomes.SAKURA_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.CHERRY_GROVE);
		allow(GLBiomes.MAGICAL_FOREST, Biomes.DARK_FOREST, Biomes.SWAMP, Biomes.MUSHROOM_FIELDS,
				Biomes.LUSH_CAVES, Biomes.DEEP_DARK);
		allow(Biomes.BAMBOO_JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE,
				Biomes.MANGROVE_SWAMP);
		// high & exposed
		allow(GLBiomes.MOUNTAIN, Biomes.STONY_PEAKS, Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS,
				Biomes.SNOWY_SLOPES, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS,
				Biomes.WINDSWEPT_FOREST);
	}

	/**
	 * Registers {@code gensokyolegacy:gensokyo} noise settings, a copy of {@code minecraft:overworld}.
	 */
	public static void noiseSettings(DataProviderInitializer init) {
		init.add(Registries.NOISE_SETTINGS, ctx -> ctx.register(NS_GENSOKYO, new NoiseGeneratorSettings(
				NoiseSettings.create(-64, 384, 1, 2),
				Blocks.STONE.defaultBlockState(),
				Blocks.WATER.defaultBlockState(),
				OverworldNoiseRouter.overworld(ctx.lookup(Registries.DENSITY_FUNCTION), ctx.lookup(Registries.NOISE)),
				SurfaceRuleData.overworld(),
				new OverworldBiomeBuilder().spawnTarget(),
				63,
				false, true, true, false
		)));
	}

	/**
	 * The dimension type, identical to the overworld one.
	 */
	public static DimensionType dimensionType() {
		return new DimensionType(
				OptionalLong.empty(),
				true, false, false, true,
				1.0, true, false,
				-64, 384, 384,
				BlockTags.INFINIBURN_OVERWORLD,
				BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0f,
				new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)
		);
	}

	/**
	 * Overworld terrain driven by the restricted biome list.
	 */
	public static NoiseBasedChunkGenerator chunkGenerator(BootstrapContext<LevelStem> ctx) {
		return new NoiseBasedChunkGenerator(biomeSource(ctx.lookup(Registries.BIOME)),
				ctx.lookup(Registries.NOISE_SETTINGS).getOrThrow(NS_GENSOKYO));
	}

	/**
	 * One climate point per allowed biome, taken from the overworld cell closest to the middle of everything that biome
	 * replaced. Biome lookup is nearest-neighbour, so dropping the other cells only merges them into their closest
	 * neighbour instead of changing the terrain.
	 */
	public static MultiNoiseBiomeSource biomeSource(HolderGetter<Biome> biomes) {
		Map<ResourceKey<Biome>, List<Climate.ParameterPoint>> cells = new LinkedHashMap<>();
		for (var cell : overworldGrid().values()) {
			cells.computeIfAbsent(allowed(cell.getSecond()), k -> new ArrayList<>()).add(cell.getFirst());
		}
		List<Pair<Climate.ParameterPoint, Holder<Biome>>> values = cells.entrySet().stream()
				.map(e -> Pair.of(representativeOf(e.getValue()), (Holder<Biome>) biomes.getOrThrow(e.getKey())))
				.toList();
		return MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(values));
	}

	/**
	 * One of the overworld cells that becomes the single climate point of a Gensokyo biome. Underground cells are skipped,
	 * since the biome a player sees is always the one at the surface.
	 */
	private static Climate.ParameterPoint representativeOf(List<Climate.ParameterPoint> cells) {
		return cells.stream().anyMatch(GensokyoDimension::isSurface)
				? middleOf(cells.stream().filter(GensokyoDimension::isSurface).toList())
				: middleOf(cells);
	}

	private static boolean isSurface(Climate.ParameterPoint cell) {
		return cell.depth().min() <= 0;
	}

	/**
	 * The cell closest to the average middle of the given cells. Wide cells are penalized so a biome is never
	 * represented by a cell that spans everything, which would swallow the whole world.
	 */
	private static Climate.ParameterPoint middleOf(List<Climate.ParameterPoint> cells) {
		double[] average = new double[7];
		for (var cell : cells) {
			var middle = middleOf(cell);
			for (int i = 0; i < average.length; i++) {
				average[i] += middle[i] / cells.size();
			}
		}
		Climate.ParameterPoint center = cells.get(0);
		double best = Double.MAX_VALUE;
		for (var cell : cells) {
			double[] middle = middleOf(cell);
			double score = 0;
			for (int i = 0; i < 6; i++) {
				score += (middle[i] - average[i]) * (middle[i] - average[i])
						+ WIDTH_PENALTY * (middle[i + 6] - middle[i]);
			}
			score += (middle[6] - average[6]) * (middle[6] - average[6]);
			if (score < best) {
				best = score;
				center = cell;
			}
		}
		return center;
	}

	/**
	 * The middle and the width of every climate parameter, width after the middle.
	 */
	private static double[] middleOf(Climate.ParameterPoint cell) {
		double[] values = new double[14];
		Climate.Parameter[] parameters = {
			cell.temperature(), cell.humidity(), cell.continentalness(), cell.erosion(),
			cell.depth(), cell.weirdness()
		};
		for (int i = 0; i < parameters.length; i++) {
			values[i] = ((double) parameters[i].min() + parameters[i].max()) / 20000.0;
			values[i + 6] = (parameters[i].max() - parameters[i].min()) / 20000.0;
		}
		values[12] = Climate.unquantizeCoord(cell.offset());
		return values;
	}

	private static Climate.ParameterList<ResourceKey<Biome>> overworldGrid() {
		return MultiNoiseBiomeSourceParameterList.knownPresets().entrySet().stream()
				.filter(e -> e.getKey().id().equals(MultiNoiseBiomeSourceParameterLists.OVERWORLD.location()))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Missing minecraft:overworld biome preset"));
	}

	private static void allow(ResourceKey<Biome> allowed, ResourceKey<Biome>... vanilla) {
		for (var biome : vanilla) {
			BIOMES.put(biome, allowed);
		}
	}

	private static ResourceKey<Biome> allowed(ResourceKey<Biome> biome) {
		var allowed = BIOMES.get(biome);
		if (allowed == null) {
			throw new IllegalStateException("Overworld biome " + biome.location() + " is not mapped, "
					+ "every overworld biome needs a replacement in " + GensokyoDimension.class.getSimpleName());
		}
		return allowed;
	}

	/**
	 * {@link NoiseRouterData#overworld} is protected, so this subclass is the only way to rebuild the overworld noise
	 * router from holder getters owned by the datagen lookup, which is what keeps the result serializable.
	 */
	private static class OverworldNoiseRouter extends NoiseRouterData {

		private static NoiseRouter overworld(HolderGetter<DensityFunction> densities,
											HolderGetter<NormalNoise.NoiseParameters> noises) {
			return NoiseRouterData.overworld(densities, noises, false, false);
		}
	}

}