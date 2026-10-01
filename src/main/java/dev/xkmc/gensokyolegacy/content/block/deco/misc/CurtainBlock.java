package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 门帘。无碰撞（可穿过），但保留薄碰撞轮廓供鼠标交互；
 * 玩家面向南/北时门帘呈东西走向，面向东/西时呈南北走向。
 */
public class CurtainBlock extends Block {

	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

	private static final VoxelShape SHAPE_X = Block.box(0, 0, 7, 16, 16, 9);
	private static final VoxelShape SHAPE_Z = Block.box(7, 0, 0, 9, 16, 16);

	public CurtainBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// 面向南/北（Z 轴）→ 门帘东西走向（X 轴）；面向东/西 → 南北走向（Z 轴）
		Direction.Axis axis = context.getHorizontalDirection().getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
		return defaultBlockState().setValue(AXIS, axis);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
	}
}
