package dev.xkmc.gensokyolegacy.content.worldgen.feature.ravine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * The shape of one ravine chamber and what it is made of.
 * <p>
 * {@code gap} is how far the wall's inside slice has to stand above its outside slice for the
 * site to be a wall at all. {@code width} is how wide the mouth is along the cliff face,
 * {@code height} how tall the dome may stand and {@code depth} how far the chamber reaches into the
 * rock. The height is not free: the dome has to fit under the cliff top with a roof of rock over
 * it, so the cliff decides the height and this is only the ceiling on it.
 * <p>
 * {@code floor} is the block the chamber's floor is laid in and {@code pool} the block that fills
 * the middle of it.
 *
 * @see RavineCaveFeature
 * @see "doc/design/genbu_ravine_cave.md"
 */
public record RavineCaveConfig(int gap, int width, int height, int depth, BlockState floor, BlockState pool)
		implements FeatureConfiguration {

	public static final Codec<RavineCaveConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.intRange(0, 16).fieldOf("gap").forGetter(RavineCaveConfig::gap),
			Codec.intRange(3, 15).fieldOf("width").forGetter(RavineCaveConfig::width),
			Codec.intRange(2, 12).fieldOf("height").forGetter(RavineCaveConfig::height),
			Codec.intRange(2, 12).fieldOf("depth").forGetter(RavineCaveConfig::depth),
			BlockState.CODEC.fieldOf("floor").forGetter(RavineCaveConfig::floor),
			BlockState.CODEC.optionalFieldOf("pool", Blocks.AIR.defaultBlockState())
					.forGetter(RavineCaveConfig::pool)
	).apply(i, RavineCaveConfig::new));

}
