package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MyourenFlagBlock implements ShapePathFindBlockMethod {

	private static final VoxelShape POST = Block.box(6.5, 0, 6.5, 9.5, 16, 9.5);
	private static final VoxelShape[] TOP = MagicTableBlock.allFaces(new VoxelBuilder(4, 13, 7, 25, 15, 9));

	static {
		for (int i = 0; i < TOP.length; i++) {
			TOP[i] = Shapes.or(POST, TOP[i]);
		}
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(BlockStateProperties.HALF) == Half.TOP
				? TOP[state.getValue(BlockTemplates.HORIZONTAL_FACING).get2DDataValue()] : POST;
	}
}
