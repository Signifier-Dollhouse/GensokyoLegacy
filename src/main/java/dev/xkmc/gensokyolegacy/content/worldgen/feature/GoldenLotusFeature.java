package dev.xkmc.gensokyolegacy.content.worldgen.feature;

import com.mojang.serialization.Codec;
import dev.xkmc.gensokyolegacy.content.block.nature.GoldenLotusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

/**
 * The golden lotus, whose largest size is a double plant. Its halves are {@code part}, not {@code half}, so
 * {@code double_block} cannot place it.
 *
 * @see "doc/design/gensokyo_biomes.md" §2.3
 */
public class GoldenLotusFeature extends SimpleBlockFeature {

	public GoldenLotusFeature(Codec<SimpleBlockConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos pos = context.origin();
		BlockState state = context.config().toPlace().getState(context.random(), pos);
		if (!state.canSurvive(level, pos)) return false;
		if (state.getValue(GoldenLotusBlock.SIZE) == GoldenLotusBlock.LARGE) {
			if (!level.isEmptyBlock(pos.above())) return false;
			level.setBlock(pos, state.setValue(GoldenLotusBlock.PART, GoldenLotusBlock.Part.LOWER), 2);
			level.setBlock(pos.above(), state.setValue(GoldenLotusBlock.PART, GoldenLotusBlock.Part.UPPER), 2);
		} else {
			level.setBlock(pos, state, 2);
		}
		return true;
	}

}
