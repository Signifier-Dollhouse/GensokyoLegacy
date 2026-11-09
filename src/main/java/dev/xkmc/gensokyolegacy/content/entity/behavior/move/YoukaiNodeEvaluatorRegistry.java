package dev.xkmc.gensokyolegacy.content.entity.behavior.move;

import dev.xkmc.gensokyolegacy.content.block.deco.door.SlidingDoorUtils;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YoukaiNodeEvaluatorRegistry {

	private static final Map<Block, YoukaiPathTypeHandler> BLOCK_HANDLERS = new LinkedHashMap<>();
	private static final Map<TagKey<Block>, YoukaiPathTypeHandler> TAG_HANDLERS = new LinkedHashMap<>();
	/**
	 * Consulted for every block, in order. For rules that are not a property of one block.
	 */
	private static final List<YoukaiPathTypeHandler> GLOBAL_HANDLERS = new ArrayList<>();

	public static void init() {
		register(BlockTags.WOODEN_DOORS, (ans, level, pos, state, mob) -> {
			if (!state.hasProperty(DoorBlock.OPEN)) return ans;
			return state.getValue(DoorBlock.OPEN) ? BlockPathTypes.DOOR_OPEN : BlockPathTypes.DOOR_WOOD_CLOSED;
		});
		register(BlockTags.TRAPDOORS, YoukaiNodeHelpers::trapdoor);
		register(GLTagGen.SLIDING_DOOR, (ans, level, pos, state, mob) -> {
			if (SlidingDoorUtils.isSeatedAndOpenable(level, pos))
				return BlockPathTypes.DOOR_WOOD_CLOSED;
			return BlockPathTypes.BLOCKED;
		});
		register(BlockTags.CLIMBABLE, (ans, level, pos, state, mob) -> {
			if (ans == BlockPathTypes.OPEN) return BlockPathTypes.WALKABLE;
			return ans;
		});
		registerGlobal(YoukaiNodeHelpers::partyFurniture);
	}

	public static void register(Block block, YoukaiPathTypeHandler handler) {
		BLOCK_HANDLERS.put(block, handler);
	}

	public static void register(TagKey<Block> tag, YoukaiPathTypeHandler handler) {
		TAG_HANDLERS.put(tag, handler);
	}

	/**
	 * Register a rule consulted for every block, after the block and tag rules.
	 */
	public static void registerGlobal(YoukaiPathTypeHandler handler) {
		GLOBAL_HANDLERS.add(handler);
	}

	static BlockPathTypes apply(BlockPathTypes ans, CollisionGetter level, BlockPos pos, @Nullable Mob mob) {
		BlockState state = level.getBlockState(pos);
		var handler = BLOCK_HANDLERS.get(state.getBlock());
		if (handler != null) ans = handler.getPathType(ans, level, pos, state, mob);
		for (var entry : TAG_HANDLERS.entrySet()) {
			if (state.is(entry.getKey())) ans = entry.getValue().getPathType(ans, level, pos, state, mob);
		}
		for (var global : GLOBAL_HANDLERS) {
			ans = global.getPathType(ans, level, pos, state, mob);
		}
		return ans;
	}

}
