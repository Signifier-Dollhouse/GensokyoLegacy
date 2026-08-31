package dev.xkmc.gensokyolegacy.content.block.misc;

import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

public class CartonShape implements ShapeBlockMethod {

    public static final VoxelShape SHAPE_NS = Block.box(3, 0, 2, 13, 8, 14);
    public static final VoxelShape SHAPE_WE = Block.box(2, 0, 3, 14, 8, 13);

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(FACING) == Direction.NORTH || state.getValue(FACING) == Direction.SOUTH ? SHAPE_NS : SHAPE_WE;
    }

}
