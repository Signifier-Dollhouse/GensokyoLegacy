package dev.xkmc.gensokyolegacy.content.entity.behavior.move;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

public class YoukaiNodeEvaluatorUtils {

	public static BlockPathTypes getPathType(BlockPathTypes ans, CollisionGetter level, int x, int y, int z,
											 Mob mob) {
		return YoukaiNodeEvaluatorRegistry.apply(ans, level, new BlockPos(x, y, z), mob);
	}

}
