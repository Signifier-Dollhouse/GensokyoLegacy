package dev.xkmc.gensokyolegacy.content.block.deco.donation;

import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

public class DonationBoxShape implements ShapePathFindBlockMethod {

	public static final VoxelShape SHAPE_NS = Shapes.or(
			Block.box(-4, 0, 0, 20, 14, 16),
			Block.box(-5, 14, -1, 21, 16, 17)
	);
	public static final VoxelShape SHAPE_WE = Shapes.or(
			Block.box(0, 0, -4, 16, 14, 20),
			Block.box(-1, 14, -5, 17, 16, 21)
	);

	private final VoxelShape northSouth;
	private final VoxelShape westEast;

	public DonationBoxShape() {
		this(SHAPE_NS, SHAPE_WE);
	}

	private DonationBoxShape(VoxelShape northSouth, VoxelShape westEast) {
		this.northSouth = northSouth;
		this.westEast = westEast;
	}

	public static DonationBoxShape myouren() {
		return new DonationBoxShape(
				Shapes.or(Block.box(-11, 0, -1, 27, 2, 17), Block.box(-8, 2, 0, 24, 14, 16),
						Block.box(-9, 14, -1, 25, 17, 17)),
				Shapes.or(Block.box(-1, 0, -11, 17, 2, 27), Block.box(0, 2, -8, 16, 14, 24),
						Block.box(-1, 14, -9, 17, 17, 25)));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? northSouth : westEast;
	}
}
