package dev.xkmc.gensokyolegacy.content.block.misc;

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

public class DonationBox2Shape implements ShapeBlockMethod {

	public static final VoxelShape SHAPE_NS = Shapes.or(
            Block.box(-4, 0, 0, 20, 14, 16),
            Block.box(-5,14,-1,21,16,17)
    );
    public static final VoxelShape SHAPE_WE = Shapes.or(
            Block.box(0, 0, -4, 16, 14, 20),
            Block.box(-1,14,-5,17,16,21)
    );

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(FACING) == Direction.NORTH || state.getValue(FACING) == Direction.SOUTH ? SHAPE_NS : SHAPE_WE;
    }
}
