package dev.xkmc.gensokyolegacy.content.block.deco;

import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import dev.xkmc.l2modularblock.mult.PlacementBlockMethod;
import dev.xkmc.l2modularblock.mult.SetPlacedByBlockMethod;
import dev.xkmc.l2modularblock.mult.ShapeUpdateBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nullable;

public class TeaTableBlock implements CreateBlockStateBlockMethod, DefaultStateBlockMethod,
		PlacementBlockMethod, SetPlacedByBlockMethod, ShapeUpdateBlockMethod {

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
		Direction facing = def.getValue(BlockTemplates.HORIZONTAL_FACING).getOpposite();
		Direction left = facing.getCounterClockWise();
		BlockPos leftPos = pos.relative(left);
		BlockPos backPos = pos.relative(facing);
		BlockPos leftBackPos = pos.relative(left).relative(facing);

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

		Direction facing = state.getValue(BlockTemplates.HORIZONTAL_FACING).getOpposite();
		Direction left = facing.getCounterClockWise();

		BlockState sub = state.setValue(ORIGIN, false);
		level.setBlockAndUpdate(pos.relative(left), sub);
		level.setBlockAndUpdate(pos.relative(facing), sub);
		level.setBlockAndUpdate(pos.relative(left).relative(facing), sub);
	}

	@Override
	public BlockState updateShape(Block self, BlockState current, BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
		if (level.isClientSide()) return current;

		if (facingState.is(self)) return current;

		Direction blockFacing = current.getValue(BlockTemplates.HORIZONTAL_FACING);
		if (!isStructureValid(level, currentPos, self, blockFacing)) {
            level.destroyBlock(currentPos, false);
			return Blocks.AIR.defaultBlockState();
		}

		return current;
	}

	private boolean isStructureValid(LevelAccessor level, BlockPos pos, Block self, Direction facing) {
		Direction left = facing.getClockWise();

		return validateOrigin(level, pos, self, facing) ||
			   validateOrigin(level, pos.relative(left), self, facing) ||
			   validateOrigin(level, pos.relative(facing.getOpposite()), self, facing) ||
			   validateOrigin(level, pos.relative(left).relative(facing.getOpposite()), self, facing);
	}

	private boolean validateOrigin(LevelAccessor level, BlockPos origin, Block self, Direction facing) {
		Direction left = facing.getClockWise();

		return level.getBlockState(origin).is(self) &&
			   level.getBlockState(origin.relative(left)).is(self) &&
			   level.getBlockState(origin.relative(facing)).is(self) &&
			   level.getBlockState(origin.relative(left).relative(facing)).is(self);
	}
}