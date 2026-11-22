package dev.xkmc.gensokyolegacy.content.block.functional.treasure;

import dev.xkmc.gensokyolegacy.content.block.deco.misc.MagicTableBlock;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 篮身 x2..14、z3..13，到篮口为止；提手不算进碰撞箱。
 */
public class TreasureBasketShape implements ShapeBlockMethod {

	public static final VoxelShape[] SHAPES =
			MagicTableBlock.allFaces(new VoxelBuilder(2, 0, 3, 14, 6, 13));

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES[state.getValue(BlockTemplates.HORIZONTAL_FACING).get2DDataValue()];
	}

}
