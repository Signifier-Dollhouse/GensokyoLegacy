package dev.xkmc.gensokyolegacy.content.worldgen.feature.stair;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * What a stair is made of, what the pool cut into it is filled with, how far the pool runs, and which heightmap the
 * surface is read from.
 *
 * @see StairPoolFeature
 * @see "doc/design/gensokyo_biomes.md" §1
 */
public record StairPoolConfig(BlockState stair, BlockState fill, int reach, Heightmap.Types heightmap)
		implements FeatureConfiguration {

	public static final Codec<StairPoolConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockState.CODEC.fieldOf("stair").forGetter(StairPoolConfig::stair),
			BlockState.CODEC.fieldOf("fill").forGetter(StairPoolConfig::fill),
			Codec.intRange(1, 16).fieldOf("reach").forGetter(StairPoolConfig::reach),
			Heightmap.Types.CODEC.fieldOf("heightmap").forGetter(StairPoolConfig::heightmap)
	).apply(i, StairPoolConfig::new));

}
