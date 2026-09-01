package dev.xkmc.gensokyolegacy.content.block.functional.barriers;

import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

public class SealingPotShape implements ShapeBlockMethod {

	public static final VoxelShape SHAPE_NS = Shapes.or(
			Block.box(4, 0, 4, 12, 1, 12),
			Block.box(2, 1, 2, 14, 11, 14),
			Block.box(4, 11, 4, 12, 16, 12),
			Block.box(-1, 4, 7, 2, 8, 9),
			Block.box(14, 4, 7, 17, 8, 9)
	);
	public static final VoxelShape SHAPE_WE = Shapes.or(
			Block.box(4, 0, 4, 12, 1, 12),
			Block.box(2, 1, 2, 14, 11, 14),
			Block.box(4, 11, 4, 12, 16, 12),
			Block.box(7, 4, -1, 9, 8, 2),
			Block.box(7, 4, 14, 9, 8, 17)
	);

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(FACING) == Direction.NORTH || state.getValue(FACING) == Direction.SOUTH ? SHAPE_NS : SHAPE_WE;
	}
}
