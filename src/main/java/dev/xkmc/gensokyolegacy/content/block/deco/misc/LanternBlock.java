package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 灯笼方块。文字面（北面）放置时朝向玩家；当下方也是任意一种灯笼时使用连接材质，否则使用未连接材质。
 */
public class LanternBlock extends Block {

	public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public LanternBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(CONNECTED, false));
	}

	public static boolean isLantern(BlockState state) {
		return state.getBlock() instanceof LanternBlock;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, CONNECTED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState()
				.setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(CONNECTED, isLantern(context.getLevel().getBlockState(context.getClickedPos().below())));
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (direction == Direction.DOWN) {
			return state.setValue(CONNECTED, isLantern(neighborState));
		}
		return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}
}
