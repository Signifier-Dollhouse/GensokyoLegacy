package dev.xkmc.gensokyolegacy.content.dimension;

import com.mojang.datafixers.util.Pair;
import com.tterrag.registrate.providers.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.GLBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * The Gensokyo dimension: overworld sea level and surface rules, flatter land, and a handpicked biome list.
 * <p>
 * The height curve is keyed on continentalness alone, where the overworld also keys it on erosion and weirdness, so
 * there are no river channels and no peaks. The climate grid reads the same four axes, so no biome depends on the two
 * that were dropped. The allowed biomes are mod copies, so vanilla structures never generate here.
 *
 * @see "doc/design/gensokyo_dimension.md"
 */
public class GensokyoDimension {

	public static final ResourceKey<DimensionType> DT_GENSOKYO = ResourceKey.create(Registries.DIMENSION_TYPE, GensokyoLegacy.loc("gensokyo"));
	public static final ResourceKey<NoiseGeneratorSettings> NS_GENSOKYO = ResourceKey.create(Registries.NOISE_SETTINGS, GensokyoLegacy.loc("gensokyo"));
	public static final ResourceKey<LevelStem> LEVEL_GENSOKYO = ResourceKey.create(Registries.LEVEL_STEM, GensokyoLegacy.loc("gensokyo"));
	public static final ResourceKey<Level> GENSOKYO = Registries.levelStemToLevel(LEVEL_GENSOKYO);

	public static final ResourceKey<DensityFunction> DF_OFFSET = df("offset");
	public static final ResourceKey<DensityFunction> DF_FACTOR = df("factor");
	public static final ResourceKey<DensityFunction> DF_DEPTH = df("depth");
	public static final ResourceKey<DensityFunction> DF_SLOPED_CHEESE = df("sloped_cheese");

	/**
	 * The three climate axes, every octave one step finer than vanilla's. The height chain splines this continentalness
	 * too — the biome grid has to read the same field the waterline is drawn on, or `ocean` stops meaning "drowned" —
	 * so the whole dimension is built half as wide rather than just the biomes. Vanilla keeps these parameters private,
	 * so the amplitude lists are copied from {@code minecraft/worldgen/noise/*.json} with {@code firstOctave} raised.
	 * <p>
	 * {@code NoiseRouterData} builds temperature and vegetation inline rather than behind a registry key, so those
	 * two cannot be redirected by {@link FlatTerrainFunctions}; the router is rebuilt field by field instead.
	 *
	 * @see "doc/design/gensokyo_dimension.md" §7
	 */
	public static final ResourceKey<NormalNoise.NoiseParameters> N_CONTINENTS_SMALL = nn("continentalness_small");
	public static final ResourceKey<NormalNoise.NoiseParameters> N_TEMPERATURE_SMALL = nn("temperature_small");
	public static final ResourceKey<NormalNoise.NoiseParameters> N_VEGETATION_SMALL = nn("vegetation_small");
	public static final ResourceKey<DensityFunction> DF_CONTINENTS_SMALL = df("continents_small");
	public static final ResourceKey<DensityFunction> DF_TEMPERATURE_SMALL = df("temperature_small");
	public static final ResourceKey<DensityFunction> DF_VEGETATION_SMALL = df("vegetation_small");

	/** Vanilla keys {@link NoiseRouterData} keeps private, rebuilt here so the overworld router can be redirected. */
	private static final ResourceKey<DensityFunction> VANILLA_SLOPED_CHEESE = vanilla("overworld/sloped_cheese");
	private static final ResourceKey<DensityFunction> VANILLA_BASE_3D_NOISE = vanilla("overworld/base_3d_noise");
	private static final ResourceKey<DensityFunction> VANILLA_SHIFT_X = vanilla("shift_x");
	private static final ResourceKey<DensityFunction> VANILLA_SHIFT_Z = vanilla("shift_z");

	private static final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0f, 1.0f);
	private static final float SURFACE_DEPTH = 0.0f;
	private static final float UNDERGROUND_DEPTH = 1.0f;

	// one band per height range of the offset spline: sea floor, strand, plain, upland, mountain, peak;
	// the ocean band ends where the terrain crosses sea level, so no dry ground is labelled ocean
	private static final List<Band> CONTINENTALNESS = List.of(
			new Band(-1.0f, -0.53f), new Band(-0.53f, -0.44f), new Band(-0.44f, -0.118f),
			new Band(-0.118f, 0.329f), new Band(0.329f, 0.595f), new Band(0.595f, 1.0f));
	private static final List<Band> TEMPERATURE = List.of(
			new Band(-1.0f, -0.35f), new Band(-0.35f, 0.35f), new Band(0.35f, 1.0f));
	private static final List<Band> HUMIDITY = List.of(
			new Band(-1.0f, -0.25f), new Band(-0.25f, 0.25f), new Band(0.25f, 1.0f));

	private static final int OCEAN = 0;
	private static final int COAST = 1;
	private static final int UPLAND = 3;
	private static final int MOUNTAIN = 4;
	private static final int PEAK = 5;
	private static final int COLD = 0;
	private static final int TEMPERATE = 1;
	private static final int DRY = 0;
	private static final int NORMAL = 1;

	public static void init(DataProviderInitializer init) {
		// every registration of a registry has to share one bootstrap, so the density functions and the noise
		// settings that reference them are declared here and nowhere else
		init.add(Registries.DENSITY_FUNCTION, GensokyoDimension::densityFunctions);
		init.add(Registries.NOISE_SETTINGS, GensokyoDimension::noiseSettings);
	}

	/**
	 * Called from {@code GLFeatureGen}, which owns the single {@link Registries#NOISE} bootstrap.
	 */
	public static void noises(BootstrapContext<NormalNoise.NoiseParameters> ctx) {
		ctx.register(N_CONTINENTS_SMALL, new NormalNoise.NoiseParameters(-8, 1.0, 1.0, 2.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0));
		ctx.register(N_TEMPERATURE_SMALL, new NormalNoise.NoiseParameters(-9, 1.5, 0.0, 1.0, 0.0, 0.0, 0.0));
		ctx.register(N_VEGETATION_SMALL, new NormalNoise.NoiseParameters(-7, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0));
	}

	private static void noiseSettings(BootstrapContext<NoiseGeneratorSettings> ctx) {
		ctx.register(NS_GENSOKYO, new NoiseGeneratorSettings(
				NoiseSettings.create(-64, 384, 1, 2),
				Blocks.STONE.defaultBlockState(),
				Blocks.WATER.defaultBlockState(),
				smallClimateRouter(new FlatTerrainFunctions(ctx.lookup(Registries.DENSITY_FUNCTION)),
						ctx.lookup(Registries.NOISE), ctx.lookup(Registries.DENSITY_FUNCTION)),
				SurfaceRuleData.overworld(),
				new OverworldBiomeBuilder().spawnTarget(),
				63,
				false, true, true, false
		));
	}

	/**
	 * The overworld router with temperature and vegetation pointed at the finer parameters. Every other field is
	 * carried over unchanged, so only the biome grid sees them.
	 */
	private static NoiseRouter smallClimateRouter(HolderGetter<DensityFunction> densities,
												  HolderGetter<NormalNoise.NoiseParameters> noises,
												  HolderGetter<DensityFunction> registry) {
		NoiseRouter overworld = OverworldNoiseRouter.overworld(densities, noises);
		return new NoiseRouter(
				overworld.barrierNoise(), overworld.fluidLevelFloodednessNoise(), overworld.fluidLevelSpreadNoise(),
				overworld.lavaNoise(),
				reference(registry.getOrThrow(DF_TEMPERATURE_SMALL)),
				reference(registry.getOrThrow(DF_VEGETATION_SMALL)),
				overworld.continents(), overworld.erosion(), overworld.depth(), overworld.ridges(),
				overworld.initialDensityWithoutJaggedness(), overworld.finalDensity(),
				overworld.veinToggle(), overworld.veinRidged(), overworld.veinGap());
	}

	/**
	 * The overworld height chain with erosion and weirdness taken out: the offset spline is a function of
	 * continentalness alone and the jaggedness term is gone, so nothing carves a river or lifts a peak.
	 */
	private static void densityFunctions(BootstrapContext<DensityFunction> ctx) {
		var densities = ctx.lookup(Registries.DENSITY_FUNCTION);
		var noises = ctx.lookup(Registries.NOISE);
		// one continentalness for the whole dimension, a field finer than vanilla's. The height chain splines it and
		// the biome grid reads it, so the ocean band edge still sits on the waterline the same spline draws.
		var continentalness = ctx.register(DF_CONTINENTS_SMALL,
				smallClimate(densities, noises.getOrThrow(N_CONTINENTS_SMALL)));
		ctx.register(DF_TEMPERATURE_SMALL, smallClimate(densities, noises.getOrThrow(N_TEMPERATURE_SMALL)));
		ctx.register(DF_VEGETATION_SMALL, smallClimate(densities, noises.getOrThrow(N_VEGETATION_SMALL)));
		var offset = ctx.register(DF_OFFSET, DensityFunctions.add(DensityFunctions.constant(NoiseRouterData.GLOBAL_OFFSET),
				blended(DensityFunctions.spline(offsetSpline(continentalness)), DensityFunctions.blendOffset())));
		var factor = ctx.register(DF_FACTOR,
				blended(DensityFunctions.spline(factorSpline(continentalness)), DensityFunctions.constant(10.0)));
		var depth = ctx.register(DF_DEPTH, DensityFunctions.add(
				DensityFunctions.yClampedGradient(-64, 320, 1.5, -1.5), reference(offset)));
		ctx.register(DF_SLOPED_CHEESE, DensityFunctions.add(
				DensityFunctions.mul(DensityFunctions.constant(4.0),
						DensityFunctions.mul(reference(depth), reference(factor)).quarterNegative()),
				reference(densities.getOrThrow(VANILLA_BASE_3D_NOISE))));
	}

	/**
	 * One climate axis at the vanilla {@code xz_scale}, on a noise whose every octave is one step finer.
	 * <p>
	 * Deliberately not {@code flat_cache}d: {@code NoiseChunk.FlatCache} indexes its table by quart position and
	 * stores the value at the cell's origin corner, so it snaps these fields to a 4-block grid and the biome edges
	 * come out stepped.
	 */
	private static DensityFunction smallClimate(HolderGetter<DensityFunction> densities,
												Holder<NormalNoise.NoiseParameters> noise) {
		return DensityFunctions.shiftedNoise2d(
				reference(densities.getOrThrow(VANILLA_SHIFT_X)), reference(densities.getOrThrow(VANILLA_SHIFT_Z)),
				0.25, noise);
	}

	/**
	 * Continentalness to offset, splined against {@link #DF_CONTINENTS_SMALL} rather than the vanilla field. The
	 * surface sits at {@code 63.5 + 128 * spline}, and the points were solved against the *vanilla* continentalness
	 * field: every 16-block band from y 64 up carries a point, each at the quantile that gives that band its share of
	 * the map — 30% plain, 22% at 80, 12% at 96, 5% at 112, 3% per band above 128, on a 13% ocean. So the height map
	 * is a choice of shares rather than a curve drawn by eye. The last three points are flat, so the peak holds at 220
	 * however high continentalness climbs.
	 * <p>
	 * The fine field has a different distribution, so these shares are now the *previous* ones, not the current ones —
	 * see {@code doc/design/gensokyo_dimension.md} §7. Re-solve with {@code tools/terrain.py} before trusting them.
	 */
	private static CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> offsetSpline(
			Holder<DensityFunction> continentalness) {
		return CubicSpline.builder(new DensityFunctions.Spline.Coordinate(continentalness))
				.addPoint(-1.10f, -0.1992f)
				.addPoint(-0.80f, -0.1914f)
				.addPoint(-0.70f, -0.1758f)
				.addPoint(-0.65f, -0.1445f)
				.addPoint(-0.60f, -0.0977f)
				.addPoint(-0.56f, -0.0508f)
				.addPoint(-0.53f, -0.0039f)
				.addPoint(-0.50f, 0.0038f)
				.addPoint(-0.118f, 0.1288f)
				.addPoint(0.108f, 0.2538f)
				.addPoint(0.255f, 0.3788f)
				.addPoint(0.329f, 0.5038f)
				.addPoint(0.380f, 0.6288f)
				.addPoint(0.436f, 0.7538f)
				.addPoint(0.505f, 0.8788f)
				.addPoint(0.595f, 1.0038f)
				.addPoint(0.734f, 1.1288f)
				.addPoint(1.05f, 1.2227f)
				.addPoint(1.30f, 1.2227f)
				.addPoint(2.00f, 1.2227f)
				.build();
	}

	/**
	 * Continentalness to vertical gain. It only steepens the slope below the surface, never moves the surface.
	 */
	private static CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> factorSpline(
			Holder<DensityFunction> continentalness) {
		return CubicSpline.builder(new DensityFunctions.Spline.Coordinate(continentalness))
				.addPoint(-0.19f, 3.95f)
				.addPoint(1.00f, 5.00f)
				.build();
	}

	/**
	 * Vanilla's wrapper that lets nearby structures pull the height around. Gensokyo has no vanilla structures, so
	 * the blending terms always collapse onto {@code min}.
	 */
	private static DensityFunction blended(DensityFunction min, DensityFunction max) {
		return DensityFunctions.flatCache(DensityFunctions.cache2d(
				DensityFunctions.lerp(DensityFunctions.blendAlpha(), max, min)));
	}

	private static DensityFunction reference(Holder<DensityFunction> function) {
		return new DensityFunctions.HolderHolder(function);
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
	 * A coarse climate grid, one biome per cell, with erosion and weirdness spanning everything so they cannot pick a
	 * biome. Biome lookup is nearest-cell, so the bands need not tile the space exactly.
	 */
	public static MultiNoiseBiomeSource biomeSource(HolderGetter<Biome> biomes) {
		List<Pair<Climate.ParameterPoint, Holder<Biome>>> values = new ArrayList<>();
		for (int c = 0; c < CONTINENTALNESS.size(); c++) {
			for (int t = 0; t < TEMPERATURE.size(); t++) {
				for (int h = 0; h < HUMIDITY.size(); h++) {
					values.add(cell(biomes, land(c, t, h), c, t, h, SURFACE_DEPTH));
					values.add(cell(biomes, underground(c, t), c, t, h, UNDERGROUND_DEPTH));
				}
			}
		}
		return MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(values));
	}

	private static Pair<Climate.ParameterPoint, Holder<Biome>> cell(HolderGetter<Biome> biomes, ResourceKey<Biome> biome,
																   int c, int t, int h, float depth) {
		// neighbouring bands touch on their shared edge, where the tie falls back to list order
		return Pair.of(new Climate.ParameterPoint(
				TEMPERATURE.get(t).parameter(), HUMIDITY.get(h).parameter(), CONTINENTALNESS.get(c).parameter(),
				FULL_RANGE, Climate.Parameter.point(depth), FULL_RANGE, 0L),
				biomes.getOrThrow(biome));
	}

	private static ResourceKey<Biome> land(int c, int t, int h) {
		if (c == OCEAN) return GLBiomes.OCEAN;
		if (c == COAST) return GLBiomes.BEACH;
		// the high two bands are temperate-and-humid country only; anything else up there stays bare mountain
		if (c == PEAK) return t == TEMPERATE && h == NORMAL ? GLBiomes.PEAK : GLBiomes.MOUNTAIN;
		if (c == MOUNTAIN) return t == TEMPERATE && h == NORMAL ? GLBiomes.GROVE : GLBiomes.MOUNTAIN;
		if (t == COLD) return GLBiomes.SNOWY_PLAINS;
		if (c == UPLAND && t == TEMPERATE) {
			return switch (h) {
				case DRY -> GLBiomes.SUNFLOWER_PLAINS;
				case NORMAL -> GLBiomes.SAKURA_FOREST;
				default -> GLBiomes.MAGICAL_FOREST;
			};
		}
		return switch (h) {
			case DRY -> t == TEMPERATE ? GLBiomes.SUNFLOWER_PLAINS : GLBiomes.DESERT;
			case NORMAL -> t == TEMPERATE ? GLBiomes.PLAINS : GLBiomes.SAVANNA;
			default -> t == TEMPERATE ? GLBiomes.FOREST : GLBiomes.BAMBOO_JUNGLE;
		};
	}

	private static ResourceKey<Biome> underground(int c, int t) {
		if (c <= COAST) return GLBiomes.PLAINS;
		return t == COLD ? GLBiomes.SNOWY_PLAINS : GLBiomes.MAGICAL_FOREST;
	}

	/**
	 * One climate band, kept as a span so the generated json reads like the table it came from.
	 */
	private record Band(float min, float max) {

		Climate.Parameter parameter() {
			return Climate.Parameter.span(min, max);
		}
	}

	/**
	 * Redirects the overworld router to our height chain. The vanilla depth and sloped cheese cannot be rewritten in
	 * place, their sub-trees live in registry entries the overworld shares.
	 */
	private record FlatTerrainFunctions(HolderGetter<DensityFunction> inner) implements HolderGetter<DensityFunction> {

		@Override
		public Optional<Holder.Reference<DensityFunction>> get(ResourceKey<DensityFunction> key) {
			if (key.equals(NoiseRouterData.DEPTH)) return inner.get(DF_DEPTH);
			if (key.equals(NoiseRouterData.FACTOR)) return inner.get(DF_FACTOR);
			if (key.equals(VANILLA_SLOPED_CHEESE)) return inner.get(DF_SLOPED_CHEESE);
			// the biome grid gets the finer continentalness; the height chain above was built from the vanilla one
			if (key.equals(NoiseRouterData.CONTINENTS)) return inner.get(DF_CONTINENTS_SMALL);
			return inner.get(key);
		}

		@Override
		public Optional<HolderSet.Named<DensityFunction>> get(TagKey<DensityFunction> tag) {
			return inner.get(tag);
		}
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

	private static ResourceKey<DensityFunction> df(String id) {
		return ResourceKey.create(Registries.DENSITY_FUNCTION, GensokyoLegacy.loc(id));
	}

	private static ResourceKey<NormalNoise.NoiseParameters> nn(String id) {
		return ResourceKey.create(Registries.NOISE, GensokyoLegacy.loc(id));
	}

	private static ResourceKey<DensityFunction> vanilla(String path) {
		return ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace(path));
	}

}