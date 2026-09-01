package dev.xkmc.gensokyolegacy.content.block.nature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import static dev.xkmc.gensokyolegacy.init.registrate.GLNaturalBlocks.EVERGREEN_VINE_PLANT;

public class EvergreenVineHeadBlock extends Block {

	public EvergreenVineHeadBlock(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (random.nextInt(10) == 0) {
			BlockPos belowPos = pos.below();
			if (level.isEmptyBlock(belowPos) && defaultBlockState().canSurvive(level, belowPos)) {
				level.setBlockAndUpdate(belowPos, EVERGREEN_VINE_PLANT.getDefaultState());
			}
		}
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (direction == Direction.UP && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState above = level.getBlockState(pos.above());
		return above.isFaceSturdy(level, pos.above(), Direction.DOWN)
				|| above.getBlock() instanceof LeavesBlock
				|| above.getBlock() instanceof EvergreenVineHeadBlock
				|| above.getBlock() instanceof EvergreenVineBodyBlock;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		BlockPos abovePos = pos.above();
		BlockState aboveState = level.getBlockState(abovePos);
		if (aboveState.getBlock() instanceof EvergreenVineHeadBlock) {
			level.setBlockAndUpdate(abovePos, EVERGREEN_VINE_PLANT.getDefaultState());
		}
	}
}
