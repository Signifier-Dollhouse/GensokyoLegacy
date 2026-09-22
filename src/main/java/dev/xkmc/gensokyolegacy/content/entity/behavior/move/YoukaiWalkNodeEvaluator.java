package dev.xkmc.gensokyolegacy.content.entity.behavior.move;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.Nullable;

public class YoukaiWalkNodeEvaluator extends WalkNodeEvaluator {

	@Override
	public PathType getPathType(PathfindingContext context, int x, int y, int z) {
		var ans = super.getPathType(context, x, y, z);
		return YoukaiNodeEvaluatorUtils.getPathType(ans, context, x, y, z);
	}

	@Override
	public int getNeighbors(Node[] outputArray, Node node) {
		int count = super.getNeighbors(outputArray, node);
		count = addLadderNeighbor(outputArray, count, node, 1);
		count = addLadderNeighbor(outputArray, count, node, -1);
		return count;
	}

	private int addLadderNeighbor(Node[] outputArray, int count, Node node, int dy) {
		if (count >= outputArray.length) return count;
		if (mob == null || currentContext == null) return count;
		BlockPos pos = new BlockPos(node.x, node.y, node.z);
		BlockPos target = pos.offset(0, dy, 0);
		if (target.getY() < mob.level().getMinBuildHeight()) return count;
		if (dy > 0) {
			// climbing up: entering the shaft from below, or continuing up inside it
			if (!isClimbable(pos) && !isClimbable(target)) return count;
		} else {
			// climbing down: only step down while standing on the ladder itself,
			// so plain drops still respect maxFallDistance
			if (!isClimbable(pos)) return count;
		}
		Node next = getLadderNode(target);
		if (isNeighborValid(next, node)) {
			outputArray[count++] = next;
		}
		return count;
	}

	@Nullable
	private Node getLadderNode(BlockPos pos) {
		PathType type = getCachedPathType(pos.getX(), pos.getY(), pos.getZ());
		float malus = mob.getPathfindingMalus(type);
		if (malus < 0) return null;
		Node node = getNode(pos.getX(), pos.getY(), pos.getZ());
		if (node.closed) return null;
		node.type = type;
		node.costMalus = Math.max(node.costMalus, malus);
		return node;
	}

	private boolean isClimbable(BlockPos pos) {
		return currentContext.level().getBlockState(pos).is(BlockTags.CLIMBABLE);
	}

}
