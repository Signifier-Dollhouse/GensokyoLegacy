package dev.xkmc.gensokyolegacy.content.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

/**
 * A standing deco laid down as the pair of halves a {@code DoubleBlockImpl} block is made of.
 * <p>
 * Vanilla's {@code simple_block} writes the bottom half only, and a top half with nothing under it drops off on the
 * first neighbour update, so a patch of them has to write both or the patch comes apart as it generates.
 *
 * @see "doc/design/gensokyo_biomes.md" §2
 */
public class DoubleBlockFeature extends SimpleBlockFeature {

	public DoubleBlockFeature(Codec<SimpleBlockConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> context) {
		var level = context.level();
		var pos = context.origin();
		BlockState state = context.config().toPlace().getState(context.random(), pos);
		if (!state.hasProperty(BlockStateProperties.HALF) || !state.canSurvive(level, pos)
				|| !level.isEmptyBlock(pos.above())) {
			return false;
		}
		level.setBlock(pos, state, 2);
		level.setBlock(pos.above(), state.setValue(BlockStateProperties.HALF, Half.TOP), 2);
		return true;
	}

}