package dev.xkmc.gensokyolegacy.init.data.biome;

import com.tterrag.registrate.providers.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
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

import javax.annotation.Nullable;

public class GLBiomes {

	public static final ResourceKey<Biome> MAGICAL_FOREST = biome("magical_forest");
	public static final ResourceKey<Biome> SAKURA_FOREST = biome("sakura_forest");
	public static final ResourceKey<Biome> MOUNTAIN = biome("mountain");

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
		BiomeDefaultFeatures.addDefaultCarversAndLakes(mountain);
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
		});
	}

	private static ResourceKey<Biome> biome(String id) {
		return ResourceKey.create(Registries.BIOME, GensokyoLegacy.loc(id));
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
