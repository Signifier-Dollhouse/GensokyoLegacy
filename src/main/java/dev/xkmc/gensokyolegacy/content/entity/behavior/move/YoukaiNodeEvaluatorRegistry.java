package dev.xkmc.gensokyolegacy.content.entity.behavior.move;

import dev.xkmc.gensokyolegacy.content.block.deco.door.SlidingDoorUtils;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class YoukaiNodeEvaluatorRegistry {

	private static final Map<Block, YoukaiPathTypeHandler> BLOCK_HANDLERS = new LinkedHashMap<>();
	private static final Map<TagKey<Block>, YoukaiPathTypeHandler> TAG_HANDLERS = new LinkedHashMap<>();

	public static void init() {
		register(BlockTags.MOB_INTERACTABLE_DOORS, (ans, context, pos, state) -> {
			if (!state.hasProperty(DoorBlock.OPEN)) return ans;
			return state.getValue(DoorBlock.OPEN) ? PathType.DOOR_OPEN : PathType.DOOR_WOOD_CLOSED;
		});
		register(BlockTags.TRAPDOORS, (ans, context, pos, state) -> {
			if ((ans == PathType.TRAPDOOR || ans == PathType.DANGER_TRAPDOOR)
					&& state.hasProperty(TrapDoorBlock.OPEN) && state.getValue(TrapDoorBlock.OPEN))
				return PathType.BLOCKED;
			return ans;
		});
		register(GLTagGen.SLIDING_DOOR, (ans, context, pos, state) -> {
			if (SlidingDoorUtils.isSeatedAndOpenable(context.level(), pos))
				return PathType.DOOR_WOOD_CLOSED;
			return PathType.BLOCKED;
		});
	}

	public static void register(Block block, YoukaiPathTypeHandler handler) {
		BLOCK_HANDLERS.put(block, handler);
	}

	public static void register(TagKey<Block> tag, YoukaiPathTypeHandler handler) {
		TAG_HANDLERS.put(tag, handler);
	}

	static PathType apply(PathType ans, PathfindingContext context, BlockPos pos) {
		BlockState state = context.getBlockState(pos);
		var handler = BLOCK_HANDLERS.get(state.getBlock());
		if (handler != null) ans = handler.getPathType(ans, context, pos, state);
		for (var entry : TAG_HANDLERS.entrySet()) {
			if (state.is(entry.getKey())) ans = entry.getValue().getPathType(ans, context, pos, state);
		}
		return ans;
	}

}
