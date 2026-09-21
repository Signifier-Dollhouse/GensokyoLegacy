package dev.xkmc.gensokyolegacy.content.block.base;

import dev.xkmc.l2modularblock.mult.PathFindBlockMethod;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Delegate blocks whose collision shape is tall enough to block walking
 * report as non-pathfindable, judged by shape height: shapes below 3px
 * (cushion, tatami, single book layer) stay walkable for pathfinding.
 */
public interface ShapePathFindBlockMethod extends ShapeBlockMethod, PathFindBlockMethod {

	@Nullable
	@Override
	default Boolean isPathfindable(BlockState state, PathComputationType type) {
		if (type != PathComputationType.LAND) return null;
		VoxelShape shape = getShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
		if (shape == null || shape.isEmpty()) return null;
		return shape.max(Direction.Axis.Y) < 3 / 16.0 ? null : false;
	}

}
