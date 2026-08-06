package dev.xkmc.gensokyolegacy.content.entity.behavior.move;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;

public interface YoukaiPathTypeHandler {

	PathType getPathType(PathType ans, PathfindingContext context, BlockPos pos, BlockState state);

}
