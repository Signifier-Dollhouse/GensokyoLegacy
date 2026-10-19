package dev.xkmc.gensokyolegacy.content.worldgen.feature.dripleaf;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Dripleaves scattered on a surface of one block, each with a patch of its own ground under it.
 * <p>
 * A dripleaf will not live on bare stone: its stem wants a support it can root in, and the stone a floor is made of is
 * not one. So the block under the plant is replaced with ground that is — mud, in the ravine — and the dripleaf grows
 * out of that.
 * <p>
 * The plant is the big dripleaf, in the shape vanilla's own has it: a stem of up to four blocks and the leaf above it.
 * The small one is not used, because it lives on clay or moss block only and would not take to mud.
 *
 * @see DripleafScatterConfig
 * @see "doc/design/gensokyo_biomes.md" §1
 */
public class DripleafScatterFeature extends Feature<DripleafScatterConfig> {

	public DripleafScatterFeature(Codec<DripleafScatterConfig> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<DripleafScatterConfig> ctx) {
		WorldGenLevel level = ctx.level();
		DripleafScatterConfig config = ctx.config();
		BlockPos origin = ctx.origin().below();
		if (!level.getBlockState(origin).is(config.stone().getBlock())) {
			return false;
		}
		for (var dir : Direction.Plane.HORIZONTAL) {
			BlockPos side = origin.relative(dir);
			BlockState state = level.getBlockState(side);
			if (state.getFluidState().is(FluidTags.WATER)) {
				return false;
			}
		}
		level.setBlock(origin, config.ground(), 2);
		placeDripleaf(level, origin.above(), ctx.random());
		return true;
	}

	/**
	 * The stem and the leaf above it, in the shape and the proportions vanilla's own dripleaf has them: two thirds of
	 * the time a stem of up to four blocks, a third of the time none at all, and a facing of its own.
	 */
	private void placeDripleaf(WorldGenLevel level, BlockPos base, RandomSource random) {
		Direction facing = Direction.from2DDataValue(random.nextInt(4));
		int height = random.nextInt(3) == 0 ? 0 : random.nextInt(5);
		BlockPos.MutableBlockPos pos = base.mutable();
		BlockState stem = Blocks.BIG_DRIPLEAF_STEM.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		for (int i = 0; i < height; i++) {
			level.setBlock(pos, stem, 2);
			pos.move(Direction.UP);
		}
		level.setBlock(pos, Blocks.BIG_DRIPLEAF.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
				.setValue(BlockStateProperties.TILT, Tilt.NONE), 2);
	}

}
