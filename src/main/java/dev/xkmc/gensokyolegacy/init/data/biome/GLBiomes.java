package dev.xkmc.gensokyolegacy.init.data.biome;

import com.tterrag.registrate.providers.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.util.Lazy;

import javax.annotation.Nullable;
import java.util.List;
import java.util.List;
import java.util.Map;

public class GLBiomes {

	public static final ResourceKey<Biome> MAGICAL_FOREST = biome("magical_forest");
	public static final ResourceKey<Biome> SAKURA_FOREST = biome("sakura_forest");
	public static final ResourceKey<Biome> MOUNTAIN = biome("mountain");

	/**
	 * Vanilla biomes copied under the mod namespace, byte for byte identical to their vanilla counterpart.
	 * <p>
	 * The Gensokyo dimension uses these instead of the vanilla ones. Structure placement asks the biome whether a
	 * structure may generate ({@code #minecraft:has_structure/*} tags on the structure), so a dimension built on
	 * {@code gensokyolegacy:plains} cannot host {@code minecraft:village_plains}, while the terrain looks the same.
	 */
	public static final ResourceKey<Biome> PLAINS = biome("plains");
	public static final ResourceKey<Biome> BEACH = biome("beach");
	public static final ResourceKey<Biome> OCEAN = biome("ocean");
	public static final ResourceKey<Biome> FOREST = biome("forest");
	public static final ResourceKey<Biome> BAMBOO_JUNGLE = biome("bamboo_jungle");
	public static final ResourceKey<Biome> SUNFLOWER_PLAINS = biome("sunflower_plains");
	public static final ResourceKey<Biome> SAVANNA = biome("savanna");
	public static final ResourceKey<Biome> DESERT = biome("desert");
	public static final ResourceKey<Biome> SNOWY_PLAINS = biome("snowy_plains");

	private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> COPIES = Map.ofEntries(
			Map.entry(Biomes.PLAINS, PLAINS),
			Map.entry(Biomes.BEACH, BEACH),
			Map.entry(Biomes.OCEAN, OCEAN),
			Map.entry(Biomes.FOREST, FOREST),
			Map.entry(Biomes.BAMBOO_JUNGLE, BAMBOO_JUNGLE),
			Map.entry(Biomes.SUNFLOWER_PLAINS, SUNFLOWER_PLAINS),
			Map.entry(Biomes.SAVANNA, SAVANNA),
			Map.entry(Biomes.DESERT, DESERT),
			Map.entry(Biomes.SNOWY_PLAINS, SNOWY_PLAINS)
	);

	/**
	 * Bootstrapped lazily, this builds every vanilla worldgen registry and is far too heavy for class loading.
	 */
	private static final Lazy<HolderLookup.Provider> VANILLA = Lazy.of(VanillaRegistries::createLookup);

	public static void init(DataProviderInitializer init) {
		init.add(Registries.BIOME, (ctx) -> {
			var pf = ctx.lookup(Registries.PLACED_FEATURE);
			var carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
			ctx.register(GLDimensionGen.BIOME_GAP, biome(6840176,
					new MobSpawnSettings.Builder(),
					new BiomeGenerationSettings.PlainBuilder(),
					Musics.createGameMusic(SoundEvents.MUSIC_END)));
			var magicalForest = addDefaultOres(new BiomeGenerationSettings.Builder(pf, carvers))
					.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GLFeatureGen.MAGICAL_FOREST_DISK_COARSE_DIRT_PLACED)
					.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GLFeatureGen.MAGICAL_FOREST_DISK_PODZOL_PLACED)
					.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GLFeatureGen.MAGICAL_FOREST_DISK_MYCELIUM_PLACED);
			MagicalForestFeatures.addLake(magicalForest);
			MagicalForestFeatures.addVegetation(magicalForest);
			ctx.register(MAGICAL_FOREST, biome(new MobSpawnSettings.Builder(), magicalForest));
			ctx.register(SAKURA_FOREST, biome(true, 0.5f, 0.8f, 0xc0d8ff, 11983713, 11983713,
					new MobSpawnSettings.Builder(),
					addDefaultOres(new BiomeGenerationSettings.Builder(pf, carvers))
							.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_PLAIN)
							.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_CHERRY)
							.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_CHERRY),
					null
			));
		var mountainSpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.farmAnimals(mountainSpawns);
		BiomeDefaultFeatures.oceanSpawns(mountainSpawns, 10, 8, 3);
		mountainSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 7, 1, 3));
		var mountain = new BiomeGenerationSettings.Builder(pf, carvers);
		addCarversAndLakes(mountain);
		BiomeDefaultFeatures.addDefaultCrystalFormations(mountain);
		BiomeDefaultFeatures.addDefaultMonsterRoom(mountain);
		BiomeDefaultFeatures.addDefaultUndergroundVariety(mountain);
		BiomeDefaultFeatures.addDefaultOres(mountain);
		BiomeDefaultFeatures.addDefaultSoftDisks(mountain);
		BiomeDefaultFeatures.addExtraEmeralds(mountain);
		BiomeDefaultFeatures.addInfestedStone(mountain);
		BiomeDefaultFeatures.addDefaultSprings(mountain);
		// order matters: the sorter links biomes by feature order, so this has to match the vanilla
		// flower_default -> patch_grass_badlands -> patch_dead_bush_badlands order
		BiomeDefaultFeatures.addDefaultFlowers(mountain);
		BiomeDefaultFeatures.addBadlandGrass(mountain);
		BiomeDefaultFeatures.addDefaultMushrooms(mountain);
		BiomeDefaultFeatures.addDefaultExtraVegetation(mountain);
		ctx.register(MOUNTAIN, biome(true, 0.2f, 0.3f, 0x3f76e4, 0x050533, 0xc0d8ff,
				null, null, null, mountainSpawns, mountain, null));
		var vanilla = VANILLA.get().lookupOrThrow(Registries.BIOME);
		for (var copy : COPIES.entrySet()) {
			ctx.register(copy.getValue(), copyOf(vanilla.getOrThrow(copy.getKey()).value(), pf, carvers));
		}
		});

		init.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ctx -> {
			ctx.register(biomeMod("plain_fairy"), new BiomeModifiers.AddSpawnsBiomeModifier(
					ctx.lookup(Registries.BIOME).getOrThrow(GLStructureTagGen.PLAIN_FAIRY_SPAWN),
					List.of(new MobSpawnSettings.SpawnerData(GLEntities.PLAIN_FAIRY.get(), 10, 3, 3))
			));
		});
	}

	/**
	 * The overworld carvers without {@code minecraft:canyon}, which ravines far deeper than Gensokyo wants, plus the
	 * lava lakes that {@link BiomeDefaultFeatures#addDefaultCarversAndLakes} bundles in with them.
	 */
	private static void addCarversAndLakes(BiomeGenerationSettings.Builder builder) {
		builder.addCarver(GenerationStep.Carving.AIR, Carvers.CAVE);
		builder.addCarver(GenerationStep.Carving.AIR, Carvers.CAVE_EXTRA_UNDERGROUND);
		builder.addFeature(GenerationStep.Decoration.LAKES, MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND);
		builder.addFeature(GenerationStep.Decoration.LAKES, MiscOverworldPlacements.LAKE_LAVA_SURFACE);
	}

	/**
	 * Rebuilds a vanilla biome from its registry keys, so the copy serializes with our own holder lookups.
	 */
	private static Biome copyOf(Biome source, HolderGetter<PlacedFeature> placedFeatures,
								HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		var climate = source.getModifiedClimateSettings();
		var generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
		List<HolderSet<PlacedFeature>> features = source.getGenerationSettings().features();
		GenerationStep.Decoration[] steps = GenerationStep.Decoration.values();
		for (int step = 0; step < features.size(); step++) {
			for (var feature : features.get(step)) {
				generation.addFeature(steps[step], key(feature));
			}
		}
		for (GenerationStep.Carving carving : source.getGenerationSettings().getCarvingStages()) {
			for (var carver : source.getGenerationSettings().getCarvers(carving)) {
				if (!key(carver).equals(Carvers.CANYON)) generation.addCarver(carving, key(carver));
			}
		}
		return new Biome.BiomeBuilder()
				.hasPrecipitation(climate.hasPrecipitation())
				.temperature(climate.temperature())
				.temperatureAdjustment(climate.temperatureModifier())
				.downfall(climate.downfall())
				.specialEffects(source.getModifiedSpecialEffects())
				.mobSpawnSettings(source.getMobSettings())
				.generationSettings(generation.build())
				.build();
	}

	private static <T> ResourceKey<T> key(Holder<T> holder) {
		return holder.unwrapKey().orElseThrow(() ->
				new IllegalStateException("Vanilla biome entry " + holder + " is not a registry reference"));
	}

	private static ResourceKey<Biome> biome(String id) {
		return ResourceKey.create(Registries.BIOME, GensokyoLegacy.loc(id));
	}

	private static ResourceKey<BiomeModifier> biomeMod(String id) {
		return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, GensokyoLegacy.loc(id));
	}

	private static BiomeGenerationSettings.Builder addDefaultOres(BiomeGenerationSettings.Builder builder) {
		BiomeDefaultFeatures.addDefaultOres(builder);
		return builder;
	}

	private static Biome biome(int fogColor,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen,
							   @Nullable Music bgm
	) {
		return biome(false, 2, 0, fogColor, spawns, gen, bgm);
	}

	private static Biome biome(boolean hasPrecipitation, float temperature, float downfall, int fogColor,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen,
							   @Nullable Music bgm
	) {
		return biome(hasPrecipitation, temperature, downfall, 4159204, 329011, fogColor,
				null, null, null, spawns, gen, bgm);
	}

	private static Biome biome(
			MobSpawnSettings.Builder spawns,
			BiomeGenerationSettings.PlainBuilder gen
	) {
		return biome(true, 0.7f, 0.8f, 0x4b6fad, 329011, 0x8facf3, 0x4770c1, 0x418e63, 0x4e9465, spawns, gen, null);
	}

	private static Biome biome(
			boolean hasPrecipitation, float temperature, float downfall,
			int fogColor, @Nullable Integer grassCol, @Nullable Integer foliageCol,
			MobSpawnSettings.Builder spawns,
			BiomeGenerationSettings.PlainBuilder gen,
			@Nullable Music bgm
	) {
		return biome(hasPrecipitation, temperature, downfall, 4159204, 329011, fogColor,
				null, grassCol, foliageCol, spawns, gen, bgm);
	}

	private static Biome biome(
			boolean hasPrecipitation, float temperature, float downfall,
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
