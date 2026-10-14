package dev.xkmc.gensokyolegacy.content.entity.behavior.move;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;

/**
 * One rule about what a block means to path over. The mob is here because a rule may legitimately
 * be about <i>who is walking</i> rather than about the block: a party guest must route around the
 * spread, while a character crossing an ordinary room has no reason to treat a laid table as a wall.
 */
public interface YoukaiPathTypeHandler {

	/**
	 * @param ans    what the vanilla rule already decided for this block
	 * @param mob    the entity being routed, or null when the evaluator has none
	 */
	PathType getPathType(PathType ans, PathfindingContext context, BlockPos pos,
			BlockState state, Mob mob);

}