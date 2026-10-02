package dev.xkmc.gensokyolegacy.content.block.deco.seat;

import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public record WoodChairBlock() implements ShapePathFindBlockMethod {

	public static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 12, 15);

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

}
