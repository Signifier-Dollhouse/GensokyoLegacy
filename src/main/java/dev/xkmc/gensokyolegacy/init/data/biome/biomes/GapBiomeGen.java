package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * The end dimension, the one biome that is not Gensokyo at all: no carvers, no features, no mob, and its own music.
 * <p>
 * Registered with the Gensokyo biomes rather than next to the dimension that uses it, so that every biome the mod
 * registers is in one list.
 */
public class GapBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("gap");

	public GapBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(6840176, new MobSpawnSettings.Builder(), new BiomeGenerationSettings.PlainBuilder(),
				Musics.createGameMusic(SoundEvents.MUSIC_END));
	}

}