package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlackIronPillarBlock extends Block {

	public static final BooleanProperty TOP = BooleanProperty.create("top");
	public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

	private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

	public BlackIronPillarBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any()
				.setValue(TOP, true)
				.setValue(BOTTOM, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TOP, BOTTOM);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState()
				.setValue(TOP, context.getLevel().getBlockState(context.getClickedPos().above()).isAir())
				.setValue(BOTTOM, context.getLevel().getBlockState(context.getClickedPos().below()).isAir());
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (direction == Direction.UP) {
			return state.setValue(TOP, neighborState.isAir());
		}
		if (direction == Direction.DOWN) {
			return state.setValue(BOTTOM, neighborState.isAir());
		}
		return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
