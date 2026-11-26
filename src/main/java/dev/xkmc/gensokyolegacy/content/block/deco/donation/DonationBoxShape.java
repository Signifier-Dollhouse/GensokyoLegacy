package dev.xkmc.gensokyolegacy.content.block.deco.donation;

import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

public class DonationBoxShape implements ShapePathFindBlockMethod {

	// index: negative side, middle, positive side along the width axis
	private final VoxelShape[] northSouth;
	private final VoxelShape[] westEast;

	public DonationBoxShape() {
		this(4, 0);
	}

	public static DonationBoxShape myouren() {
		return new DonationBoxShape(9, 1);
	}

	/**
	 * Each part is a single one-block box in whole pixels. {@code depth} is how far it reaches into a
	 * side cell along the model's long axis, and {@code expand} how far every part overflows the
	 * short axis on either side.
	 */
	private DonationBoxShape(int depth, int expand) {
		int lo = -expand;
		int hi = 16 + expand;
		this.northSouth = new VoxelShape[]{
				Block.box(16 - depth, 0, lo, 16, 15, hi),
				Block.box(0, 0, lo, 16, 15, hi),
				Block.box(0, 0, lo, depth, 15, hi)
		};
		this.westEast = new VoxelShape[]{
				Block.box(lo, 0, 16 - depth, hi, 15, 16),
				Block.box(lo, 0, 0, hi, 15, 16),
				Block.box(lo, 0, 0, hi, 15, depth)
		};
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		boolean z = state.getValue(FACING).getAxis() == Direction.Axis.Z;
		VoxelShape[] shapes = z ? northSouth : westEast;
		return shapes[index(state)];
	}

	private static int index(BlockState state) {
		var part = state.getValue(DonationBoxBlock.PART);
		if (part == DonationBoxBlock.Part.MIDDLE) return 1;
		Direction side = part == DonationBoxBlock.Part.LEFT ?
				state.getValue(FACING).getCounterClockWise() : state.getValue(FACING).getClockWise();
		return side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 2 : 0;
	}

}
