package dev.xkmc.gensokyolegacy.content.entity.behavior.move;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;

public class YoukaiNodeEvaluatorUtils {

	public static PathType getPathType(PathType ans, PathfindingContext context, int x, int y, int z) {
		return YoukaiNodeEvaluatorRegistry.apply(ans, context, new BlockPos(x, y, z));
	}

}
