package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WaterloggedBushBlock extends SimpleBushBlock implements SimpleWaterloggedBlock {

	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private static final MapCodec<WaterloggedBushBlock> CODEC = simpleCodec(WaterloggedBushBlock::new);

	public WaterloggedBushBlock(Properties properties, VoxelShape shape) {
		super(properties, shape);
		this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
	}

	public WaterloggedBushBlock(Properties properties) {
		this(properties, FULL);
	}

	@Override
	public MapCodec<? extends BushBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(WATERLOGGED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
		return this.defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos pos, BlockPos facingPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, facing, facingState, level, pos, facingPos);
	}

}