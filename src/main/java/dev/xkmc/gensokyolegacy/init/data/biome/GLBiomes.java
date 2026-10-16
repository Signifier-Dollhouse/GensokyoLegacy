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
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every biome the Gensokyo dimension may select, and how each one is put together.
 * <p>
 * The Touhou biomes are a vanilla base plus a custom feature tail: the base keeps the carvers, ores, lakes and
 * vanilla decoration in the exact order vanilla put them in, and {@link GensokyoBiomeFeatures#tail} appends after
 * all of it. Never interleave the two — that is what a feature order cycle looks like, and it only fails at runtime.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class GLBiomes {

	public static final ResourceKey<Biome> MAGICAL_FOREST = biome("magical_forest");
	public static final ResourceKey<Biome> SAKURA_FOREST = biome("sakura_forest");
	// lower ground
	public static final ResourceKey<Biome> MUENZUKA = biome("muenzuka");
	public static final ResourceKey<Biome> HUMAN_VILLAGE = biome("human_village");
	public static final ResourceKey<Biome> MYOUREN_TEMPLE_BARRENS = biome("myouren_temple_barrens");
	// upper ground
	public static final ResourceKey<Biome> BAMBOO_FOREST_OF_THE_LOST = biome("bamboo_forest_of_the_lost");
	public static final ResourceKey<Biome> HILL_OF_THE_NAMELESS = biome("hill_of_the_nameless");
	// mountain
	public static final ResourceKey<Biome> UNTRODDEN_VALLEY = biome("untrodden_valley");
	public static final ResourceKey<Biome> GENBU_RAVINE = biome("genbu_ravine");
	public static final ResourceKey<Biome> YOUKAI_MOUNTAIN_FOOTHILLS = biome("youkai_mountain_foothills");
	public static final ResourceKey<Biome> SANCTUARY = biome("sanctuary");
	// underground
	public static final ResourceKey<Biome> HELL_OF_BLOOD_POOLS = biome("hell_of_blood_pools");
	public static final ResourceKey<Biome> ANIMAL_REALM = biome("animal_realm");
	public static final ResourceKey<Biome> HELL_OF_BLAZING_FIRES = biome("hell_of_blazing_fires");
	public static final ResourceKey<Biome> FORMER_HELL = biome("former_hell");

	/**
	 * Vanilla biomes copied under the mod namespace, byte for byte identical to their vanilla counterpart.
	 * <p>
	 * The Gensokyo dimension uses these instead of the vanilla ones. Structure placement asks the biome whether a
	 * structure may generate ({@code #minecraft:has_structure/*} tags on the structure), so a dimension built on
	 * {@code gensokyolegacy:plains} cannot host {@code minecraft:village_plains}, while the terrain looks the same.
	 */
	public static final ResourceKey<Biome> BEACH = biome("beach");
	public static final ResourceKey<Biome> OCEAN = biome("ocean");
	public static final ResourceKey<Biome> SUNFLOWER_PLAINS = biome("sunflower_plains");
	public static final ResourceKey<Biome> GROVE = biome("grove");

	/**
	 * Not a straight copy: {@link Biomes#STONY_PEAKS} supplies the climate, effects and spawns and
	 * {@link Biomes#STONY_SHORE} is unioned in for its features and carvers. Not in {@link #COPIES}.
	 */
	public static final ResourceKey<Biome> PEAK = biome("peak");

	private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> COPIES = Map.ofEntries(
			Map.entry(Biomes.BEACH, BEACH),
			Map.entry(Biomes.OCEAN, OCEAN),
			Map.entry(Biomes.SUNFLOWER_PLAINS, SUNFLOWER_PLAINS),
			Map.entry(Biomes.GROVE, GROVE)
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
			var vanilla = VANILLA.get().lookupOrThrow(Registries.BIOME);
			registerTouhouBiomes(ctx, vanilla, pf, carvers);
			for (var copy : COPIES.entrySet()) {
				ctx.register(copy.getValue(), copyOf(vanilla.getOrThrow(copy.getKey()).value(), null, pf, carvers));
			}
			ctx.register(PEAK, copyOf(vanilla.getOrThrow(Biomes.STONY_PEAKS).value(),
					vanilla.getOrThrow(Biomes.STONY_SHORE).value(), pf, carvers));
		});

		init.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ctx -> {
			ctx.register(biomeMod("plain_fairy"), new BiomeModifiers.AddSpawnsBiomeModifier(
					ctx.lookup(Registries.BIOME).getOrThrow(GLStructureTagGen.PLAIN_FAIRY_SPAWN),
					List.of(new MobSpawnSettings.SpawnerData(GLEntities.PLAIN_FAIRY.get(), 10, 3, 3))
			));
		});
	}

	/**
	 * The twelve biomes of {@code doc/design/gensokyo_dimension_biome_guide.md}, each a vanilla base chosen for its
	 * terrain and decoration, plus our own climate, colours, spawns and feature tail. The base is read through
	 * {@link #VANILLA} rather than reused, so the copy serializes under our own holder lookups.
	 * <p>
	 * Only the base's <em>features</em> are borrowed. Its climate and colours are the vanilla ones, which is why each
	 * call spells out its own.
	 * <p>
	 * The four calls per band are in the guide's order — hot humid, cold humid, hot dry, cold dry — so the declared
	 * temperature and downfall agree with the climate cell the grid picks the biome in. The one exception is
	 * {@link #MYOUREN_TEMPLE_BARRENS}: it is the cold dry corner and cold enough to sit under the snow line, so it
	 * turns precipitation off rather than snow over the coarse dirt it is made of.
	 */
	private static void registerTouhouBiomes(BootstrapContext<Biome> ctx, HolderGetter<Biome> vanilla,
											HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		var muenzukaSpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.commonSpawns(muenzukaSpawns);
		muenzukaSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 4, 2, 4));
		ctx.register(MUENZUKA, touhou(vanilla.getOrThrow(Biomes.FOREST).value(), pf, carvers,
				GensokyoBiomeFeatures.MUENZUKA, muenzukaSpawns, true, 0.2f, 0.9f, 0x38605c, 0x3f4a46, 0x6a7058, 0x5a604e));

		var villageSpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.commonSpawns(villageSpawns);
		BiomeDefaultFeatures.farmAnimals(villageSpawns);
		villageSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.HORSE, 5, 2, 6))
				.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.DONKEY, 1, 1, 3));
		ctx.register(HUMAN_VILLAGE, touhou(vanilla.getOrThrow(Biomes.PLAINS).value(), pf, carvers,
				GensokyoBiomeFeatures.HUMAN_VILLAGE, villageSpawns, true, 0.8f, 0.5f, 0x4f8f7a, 0x8fb7d6, 0x7db356, 0x6f9c4a));

		// a temple ruin is a quiet place: the creature rate is cut the way badlands cuts it
		var barrensSpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.commonSpawns(barrensSpawns);
		barrensSpawns.creatureGenerationProbability(0.05f);
		barrensSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.LLAMA, 4, 2, 6));
		ctx.register(MYOUREN_TEMPLE_BARRENS, touhou(vanilla.getOrThrow(Biomes.BADLANDS).value(), pf, carvers,
				GensokyoBiomeFeatures.BARRENS, barrensSpawns, false, -0.3f, 0.2f, 0x6b5a35, 0xc0a878, 0x9a8f5c, 0x8a7f50));

		var bambooSpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.commonSpawns(bambooSpawns);
		bambooSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PANDA, 20, 2, 4))
				.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));
		ctx.register(BAMBOO_FOREST_OF_THE_LOST, touhou(vanilla.getOrThrow(Biomes.SNOWY_TAIGA).value(), pf, carvers,
				GensokyoBiomeFeatures.BAMBOO_FOREST, bambooSpawns, true, -0.2f, 0.9f, 0x3f6f6a, 0x7d94a0, 0x6f8f52, 0x5f7f4a));

		var hillSpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.commonSpawns(hillSpawns);
		hillSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SHEEP, 2, 2, 4))
				.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));
		ctx.register(HILL_OF_THE_NAMELESS, touhou(vanilla.getOrThrow(Biomes.MEADOW).value(), pf, carvers,
				GensokyoBiomeFeatures.HILL, hillSpawns, true, 0.2f, 0.4f, 0x4a7a86, 0x8aa0b0, 0x87a06a, 0x7a9260));

		var valleySpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.commonSpawns(valleySpawns);
		valleySpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3))
				.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 5, 4, 4));
		ctx.register(UNTRODDEN_VALLEY, touhou(vanilla.getOrThrow(Biomes.TAIGA).value(), pf, carvers,
				GensokyoBiomeFeatures.VALLEY, valleySpawns, true, 0.4f, 0.8f, 0x3c6f7d, 0x7d8f9c, 0x6b8560, 0x5d7854));

		var ravineSpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.commonSpawns(ravineSpawns);
		ravineSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FROG, 10, 2, 5))
				.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 15, 1, 5));
		ctx.register(GENBU_RAVINE, touhou(vanilla.getOrThrow(Biomes.SWAMP).value(), pf, carvers,
				GensokyoBiomeFeatures.RAVINE, ravineSpawns, true, 0.2f, 0.9f, 0x39604f, 0x5f7a6a, 0x5a7a5c, 0x4e6e52));

		var foothillsSpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.commonSpawns(foothillsSpawns);
		BiomeDefaultFeatures.farmAnimals(foothillsSpawns);
		foothillsSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3))
				.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.LLAMA, 4, 2, 6));
		ctx.register(YOUKAI_MOUNTAIN_FOOTHILLS, touhou(vanilla.getOrThrow(Biomes.WINDSWEPT_HILLS).value(), pf, carvers,
				GensokyoBiomeFeatures.FOOTHILLS, foothillsSpawns, true, 0.5f, 0.5f, 0x46707d, 0x8496a8, 0x6f8a58, 0x617c4e));

		var sanctuarySpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.commonSpawns(sanctuarySpawns);
		sanctuarySpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 2, 2, 6))
				.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SHEEP, 2, 2, 4));
		ctx.register(SANCTUARY, touhou(vanilla.getOrThrow(Biomes.MEADOW).value(), pf, carvers,
				GensokyoBiomeFeatures.SANCTUARY, sanctuarySpawns, true, 0.2f, 0.4f, 0x57a0a6, 0xa8c4d8, 0x8fc06a, 0x82b45e));

		var bloodSpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.caveSpawns(bloodSpawns);
		BiomeDefaultFeatures.monsters(bloodSpawns, 95, 5, 100, false);
		bloodSpawns.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.COD, 15, 3, 6));
		ctx.register(HELL_OF_BLOOD_POOLS, touhou(vanilla.getOrThrow(Biomes.SWAMP).value(), pf, carvers,
				GensokyoBiomeFeatures.BLOOD_POOLS, bloodSpawns, true, 1.2f, 0.0f, 0x6d1420, 0x5c1414, 0x7a2a2a, 0x6a2424));

		var animalSpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.commonSpawns(animalSpawns);
		animalSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 2, 4))
				.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.SPIDER, 8, 2, 4));
		ctx.register(ANIMAL_REALM, touhou(vanilla.getOrThrow(Biomes.LUSH_CAVES).value(), pf, carvers,
				GensokyoBiomeFeatures.ANIMAL_REALM, animalSpawns, true, 0.7f, 0.8f, 0x2f4a3a, 0x2c3a30, 0x5a6b45, 0x4e5c3c));

		var fireSpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.caveSpawns(fireSpawns);
		BiomeDefaultFeatures.monsters(fireSpawns, 95, 5, 100, false);
		fireSpawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.MAGMA_CUBE, 4, 2, 4));
		ctx.register(HELL_OF_BLAZING_FIRES, touhou(vanilla.getOrThrow(Biomes.BADLANDS).value(), pf, carvers,
				GensokyoBiomeFeatures.BLAZING_FIRES, fireSpawns, true, 1.5f, 0.0f, 0xc2410a, 0x8c2f0e, 0x6b4030, 0x5a3528));

		var formerHellSpawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.caveSpawns(formerHellSpawns);
		// the ruin still has people in it: a heavier zombie villager weight than the other underground biomes
		BiomeDefaultFeatures.monsters(formerHellSpawns, 90, 25, 90, false);
		ctx.register(FORMER_HELL, touhou(vanilla.getOrThrow(Biomes.DRIPSTONE_CAVES).value(), pf, carvers,
				GensokyoBiomeFeatures.FORMER_HELL, formerHellSpawns, true, 0.8f, 0.4f, 0x5a4a3a, 0x6a5a4a, 0x7a6b52, 0x6b5c46));
	}

	/**
	 * One guide biome: a vanilla base for its features, and our own everything else.
	 */
	private static Biome touhou(Biome base, HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers,
								GensokyoBiomeFeatures.Tail tail, MobSpawnSettings.Builder spawns,
								boolean hasPrecipitation, float temperature, float downfall, int waterColor, int fogColor,
								@Nullable Integer grassCol, @Nullable Integer foliageCol) {
		var generation = generationOf(base, pf, carvers, null);
		for (var key : tail.vegetation()) generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, key);
		for (var key : tail.soil()) generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, key);
		for (var key : tail.cave()) generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, key);
		return biome(hasPrecipitation, temperature, downfall, waterColor, 329011, fogColor, null, grassCol, foliageCol,
				spawns, generation, null);
	}

	/**
	 * Rebuilds a vanilla biome from its registry keys, so the copy serializes with our own holder lookups.
	 * {@code extra} contributes its features and carvers on top, for a biome assembled from two vanilla sources.
	 * {@code minecraft:canyon} is dropped from both.
	 */
	private static Biome copyOf(Biome source, @Nullable Biome extra, HolderGetter<PlacedFeature> placedFeatures,
								HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		return new Biome.BiomeBuilder()
				.hasPrecipitation(source.getModifiedClimateSettings().hasPrecipitation())
				.temperature(source.getModifiedClimateSettings().temperature())
				.temperatureAdjustment(source.getModifiedClimateSettings().temperatureModifier())
				.downfall(source.getModifiedClimateSettings().downfall())
				.specialEffects(source.getModifiedSpecialEffects())
				.mobSpawnSettings(source.getMobSettings())
				.generationSettings(generationOf(source, placedFeatures, carvers, extra).build())
				.build();
	}

	/** The features and carvers of one vanilla biome, rebuilt on our lookups and in vanilla's own order. */
	private static BiomeGenerationSettings.Builder generationOf(Biome source, HolderGetter<PlacedFeature> placedFeatures,
															  HolderGetter<ConfiguredWorldCarver<?>> carvers,
															  @Nullable Biome extra) {
		var generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
		GenerationStep.Decoration[] steps = GenerationStep.Decoration.values();
		var byStep = new LinkedHashMap<GenerationStep.Decoration, Set<ResourceKey<PlacedFeature>>>();
		var byStage = new LinkedHashMap<GenerationStep.Carving, Set<ResourceKey<ConfiguredWorldCarver<?>>>>();
		collect(source, steps, byStep, byStage);
		if (extra != null) collect(extra, steps, byStep, byStage);
		for (var entry : byStep.entrySet()) {
			for (var feature : entry.getValue()) generation.addFeature(entry.getKey(), feature);
		}
		for (var entry : byStage.entrySet()) {
			for (var carver : entry.getValue()) generation.addCarver(entry.getKey(), carver);
		}
		return generation;
	}

	/** Linked sets keep vanilla's own ordering, which {@code FeatureSorter} reads as feature order. */
	private static void collect(Biome biome, GenerationStep.Decoration[] steps,
								Map<GenerationStep.Decoration, Set<ResourceKey<PlacedFeature>>> byStep,
								Map<GenerationStep.Carving, Set<ResourceKey<ConfiguredWorldCarver<?>>>> byStage) {
		List<HolderSet<PlacedFeature>> features = biome.getGenerationSettings().features();
		for (int step = 0; step < features.size(); step++) {
			for (var feature : features.get(step)) {
				byStep.computeIfAbsent(steps[step], k -> new LinkedHashSet<>()).add(key(feature));
			}
		}
		for (GenerationStep.Carving carving : biome.getGenerationSettings().getCarvingStages()) {
			for (var carver : biome.getGenerationSettings().getCarvers(carving)) {
				if (!key(carver).equals(Carvers.CANYON)) {
					byStage.computeIfAbsent(carving, k -> new LinkedHashSet<>()).add(key(carver));
				}
			}
		}
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