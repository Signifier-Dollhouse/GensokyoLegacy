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

	/** Vanilla keys {@link NoiseRouterData} keeps private, rebuilt here so the overworld router can be redirected. */
	private static final ResourceKey<DensityFunction> VANILLA_SLOPED_CHEESE = vanilla("overworld/sloped_cheese");
	private static final ResourceKey<DensityFunction> VANILLA_BASE_3D_NOISE = vanilla("overworld/base_3d_noise");

	private static final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0f, 1.0f);
	private static final float SURFACE_DEPTH = 0.0f;
	private static final float UNDERGROUND_DEPTH = 1.0f;

	private static final List<Band> CONTINENTALNESS = List.of(
			new Band(-1.0f, -0.55f), new Band(-0.55f, -0.25f), new Band(-0.25f, -0.02f),
			new Band(-0.02f, 0.2f), new Band(0.2f, 0.55f), new Band(0.55f, 1.0f));
	private static final List<Band> TEMPERATURE = List.of(
			new Band(-1.0f, -0.35f), new Band(-0.35f, 0.35f), new Band(0.35f, 1.0f));
	private static final List<Band> HUMIDITY = List.of(
			new Band(-1.0f, -0.25f), new Band(-0.25f, 0.25f), new Band(0.25f, 1.0f));

	private static final int OCEAN = 1;
	private static final int COAST = 2;
	private static final int UPLAND = 4;
	private static final int HIGHLAND = 5;
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

	private static void noiseSettings(BootstrapContext<NoiseGeneratorSettings> ctx) {
		ctx.register(NS_GENSOKYO, new NoiseGeneratorSettings(
				NoiseSettings.create(-64, 384, 1, 2),
				Blocks.STONE.defaultBlockState(),
				Blocks.WATER.defaultBlockState(),
				OverworldNoiseRouter.overworld(new FlatTerrainFunctions(ctx.lookup(Registries.DENSITY_FUNCTION)),
						ctx.lookup(Registries.NOISE)),
				SurfaceRuleData.overworld(),
				new OverworldBiomeBuilder().spawnTarget(),
				63,
				false, true, true, false
		));
	}

	/**
	 * The overworld height chain with erosion and weirdness taken out: the offset spline is a function of
	 * continentalness alone and the jaggedness term is gone, so nothing carves a river or lifts a peak.
	 */
	private static void densityFunctions(BootstrapContext<DensityFunction> ctx) {
		var densities = ctx.lookup(Registries.DENSITY_FUNCTION);
		var continentalness = densities.getOrThrow(NoiseRouterData.CONTINENTS);
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
	 * Continentalness to offset. The surface sits at {@code 63.5 + 128 * spline}, and the points were solved against
	 * the real continentalness and base_3d_noise fields: ocean floor 29, shoreline at continentalness -0.28 covering
	 * 20% of the map, and peaks pinned at 220 instead of the overworld's 320. The last three points are flat, so the
	 * peak stays at 220 however high continentalness climbs; base_3d_noise then adds about 14 either side of it.
	 */
	private static CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> offsetSpline(
			Holder<DensityFunction> continentalness) {
		return CubicSpline.builder(new DensityFunctions.Spline.Coordinate(continentalness))
				.addPoint(-1.10f, -0.167f)
				.addPoint(-0.80f, -0.167f)
				.addPoint(-0.60f, -0.1419f)
				.addPoint(-0.45f, -0.0718f)
				.addPoint(-0.35f, -0.0217f)
				.addPoint(-0.25f, 0.03f)
				.addPoint(0.05f, 0.05f)
				.addPoint(0.30f, 0.06f)
				.addPoint(0.60f, 0.10f)
				.addPoint(0.70f, 0.3246f)
				.addPoint(0.80f, 0.6053f)
				.addPoint(0.90f, 0.9309f)
				.addPoint(0.97f, 1.2228f)
				.addPoint(1.30f, 1.2228f)
				.addPoint(2.00f, 1.2228f)
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
		if (c <= OCEAN) return GLBiomes.OCEAN;
		if (c == COAST) return GLBiomes.BEACH;
		if (c == HIGHLAND) return GLBiomes.MOUNTAIN;
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

	private static ResourceKey<DensityFunction> vanilla(String path) {
		return ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace(path));
	}

}