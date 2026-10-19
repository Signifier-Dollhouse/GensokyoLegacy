package dev.xkmc.gensokyolegacy.content.worldgen.feature.dripleaf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * The block the dripleaves are scattered on, and the ground left under them.
 *
 * @see DripleafScatterFeature
 * @see "doc/design/gensokyo_biomes.md" §1
 */
public record DripleafScatterConfig(BlockState stone, BlockState ground) implements FeatureConfiguration {

	public static final Codec<DripleafScatterConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockState.CODEC.fieldOf("stone").forGetter(DripleafScatterConfig::stone),
			BlockState.CODEC.fieldOf("ground").forGetter(DripleafScatterConfig::ground)
	).apply(i, DripleafScatterConfig::new));

}
