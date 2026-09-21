package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.mult.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class TeaTableBlock implements CreateBlockStateBlockMethod, DefaultStateBlockMethod,
		PlacementBlockMethod, SetPlacedByBlockMethod, OnReplacedBlockMethod, ShapePathFindBlockMethod {

	public static final BooleanProperty ORIGIN = BooleanProperty.create("origin");

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ORIGIN);
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(ORIGIN, true);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(@Nullable BlockState def, BlockPlaceContext ctx) {
		if (def == null) return null;
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		Direction direction = def.getValue(BlockTemplates.HORIZONTAL_FACING);
		Direction left = direction.getClockWise();
		Direction back = direction.getOpposite();
		BlockPos leftPos = pos.relative(left);
		BlockPos backPos = pos.relative(back);
		BlockPos leftBackPos = pos.relative(left).relative(back);

		if (!level.getBlockState(leftPos).canBeReplaced() ||
				!level.getBlockState(backPos).canBeReplaced() ||
				!level.getBlockState(leftBackPos).canBeReplaced()) {
			return null;
		}

		return def;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity le, ItemStack stack) {
		if (level.isClientSide()) return;

        Direction direction = state.getValue(BlockTemplates.HORIZONTAL_FACING);
		Direction left = direction.getClockWise();
		Direction back = direction.getOpposite();

		BlockState sub = state.setValue(ORIGIN, false);
		level.setBlockAndUpdate(pos.relative(left), sub.setValue(BlockTemplates.HORIZONTAL_FACING, direction.getClockWise()));
		level.setBlockAndUpdate(pos.relative(back), sub.setValue(BlockTemplates.HORIZONTAL_FACING, direction.getCounterClockWise()));
		level.setBlockAndUpdate(pos.relative(left).relative(back), sub.setValue(BlockTemplates.HORIZONTAL_FACING, direction.getOpposite()));
	}

	@Override
	public void onReplaced(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (level.isClientSide()) return;
		if (state.is(newState.getBlock())) return;

		Direction clockwise = state.getValue(BlockTemplates.HORIZONTAL_FACING).getClockWise();
        BlockPos relativePos = pos.relative(clockwise);
        if(level.getBlockState(relativePos).is(state.getBlock())){
            level.destroyBlock(relativePos, false);
        }
	}

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Block.box(0,0,0,16,12,16);
    }
}