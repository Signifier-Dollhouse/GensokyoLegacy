package dev.xkmc.gensokyolegacy.content.entity.behavior.move;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;

public class YoukaiFlyNodeEvaluator extends FlyNodeEvaluator {

	@Override
	public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
		var ans = super.getBlockPathType(level, x, y, z);
		return YoukaiNodeEvaluatorUtils.getPathType(ans, (CollisionGetter) level, x, y, z, mob);
	}

}
