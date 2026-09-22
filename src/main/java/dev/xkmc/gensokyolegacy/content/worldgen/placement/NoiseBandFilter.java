package dev.xkmc.gensokyolegacy.content.worldgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * Passes where a seeded 2D noise lies in [min, max). Several features filtering on bands of one
 * low frequency noise split a biome into forest types without registering sub-biomes; the feature
 * scale comes from the noise parameters (first octave).
 */
public class NoiseBandFilter extends PlacementFilter {

	public static final MapCodec<NoiseBandFilter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ResourceKey.codec(Registries.NOISE).fieldOf("noise").forGetter(e -> e.noise),
			Codec.DOUBLE.optionalFieldOf("min", -Double.MAX_VALUE).forGetter(e -> e.min),
			Codec.DOUBLE.optionalFieldOf("max", Double.MAX_VALUE).forGetter(e -> e.max)
	).apply(i, NoiseBandFilter::new));

	private final ResourceKey<NormalNoise.NoiseParameters> noise;
	private final double min, max;

	public NoiseBandFilter(ResourceKey<NormalNoise.NoiseParameters> noise, double min, double max) {
		this.noise = noise;
		this.min = min;
		this.max = max;
	}

	public static NoiseBandFilter below(ResourceKey<NormalNoise.NoiseParameters> noise, double max) {
		return new NoiseBandFilter(noise, -Double.MAX_VALUE, max);
	}

	public static NoiseBandFilter atLeast(ResourceKey<NormalNoise.NoiseParameters> noise, double min) {
		return new NoiseBandFilter(noise, min, Double.MAX_VALUE);
	}

	@Override
	protected boolean shouldPlace(PlacementContext ctx, RandomSource random, BlockPos pos) {
		// RandomState caches the instance per key
		NormalNoise sampler = ctx.getLevel().getLevel().getChunkSource().randomState().getOrCreateNoise(noise);
		double value = sampler.getValue(pos.getX(), 0, pos.getZ());
		return value >= min && value < max;
	}

	@Override
	public PlacementModifierType<?> type() {
		return GLWorldGen.NOISE_BAND.get();
	}

}
