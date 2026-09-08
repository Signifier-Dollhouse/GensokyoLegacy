package dev.xkmc.gensokyolegacy.init.data.biome;

import com.tterrag.registrate.providers.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.*;

import javax.annotation.Nullable;

public class GLBiomes {

	public static final ResourceKey<Biome> MAGICAL_FOREST = biome("magical_forest");

	public static void init(DataProviderInitializer init) {
		init.add(Registries.BIOME, (ctx) -> {
			ctx.register(GLDimensionGen.BIOME_GAP, biome(6840176,
					new MobSpawnSettings.Builder(),
					new BiomeGenerationSettings.PlainBuilder(),
					Musics.createGameMusic(SoundEvents.MUSIC_END)));
			ctx.register(MAGICAL_FOREST, biome(
					new MobSpawnSettings.Builder(),
					new BiomeGenerationSettings.PlainBuilder()));
		});
	}

	private static ResourceKey<Biome> biome(String id) {
		return ResourceKey.create(Registries.BIOME, GensokyoLegacy.loc(id));
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
				null, null, spawns, gen, bgm);
	}

	private static Biome biome(
			MobSpawnSettings.Builder spawns,
			BiomeGenerationSettings.PlainBuilder gen
	) {
		return biome(true, 0.7f, 0.8f, 0xc0d8ff, 0x59c93c, 0x30bb0b, spawns, gen, null);
	}

	private static Biome biome(
			boolean hasPrecipitation, float temperature, float downfall,
			int fogColor, @Nullable Integer grassCol, @Nullable Integer foliageCol,
			MobSpawnSettings.Builder spawns,
			BiomeGenerationSettings.PlainBuilder gen,
			@Nullable Music bgm
	) {
		return biome(hasPrecipitation, temperature, downfall, 4159204, 329011, fogColor,
				grassCol, foliageCol, spawns, gen, bgm);
	}

	private static Biome biome(
			boolean hasPrecipitation, float temperature, float downfall,
			int waterColor, int waterFogColor, int fogColor,
			@Nullable Integer grassCol, @Nullable Integer foliageCol,
			MobSpawnSettings.Builder spawns,
			BiomeGenerationSettings.PlainBuilder gen,
			@Nullable Music bgm
	) {
		BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
				.waterColor(waterColor)
				.waterFogColor(waterFogColor)
				.fogColor(fogColor)
				.skyColor(calculateSkyColor(temperature))
				.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
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
