package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 黑铁栅栏。连接逻辑同原版铁栏杆；顶端（或底端）没有黑铁栅栏时端面显示专用截面材质，
 * 有黑铁栅栏时显示普通材质以无缝衔接。
 */
public class BlackIronFenceBlock extends IronBarsBlock {

	public static final BooleanProperty TOP = BooleanProperty.create("top");
	public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

	public BlackIronFenceBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
				.setValue(TOP, true)
				.setValue(BOTTOM, true));
	}

	public static boolean isFence(BlockState state) {
		return state.getBlock() instanceof BlackIronFenceBlock;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(TOP, BOTTOM);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context)
				.setValue(TOP, !isFence(context.getLevel().getBlockState(context.getClickedPos().above())))
				.setValue(BOTTOM, !isFence(context.getLevel().getBlockState(context.getClickedPos().below())));
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (direction == Direction.UP) {
			state = state.setValue(TOP, !isFence(neighborState));
		} else if (direction == Direction.DOWN) {
			state = state.setValue(BOTTOM, !isFence(neighborState));
		}
		return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}
}
