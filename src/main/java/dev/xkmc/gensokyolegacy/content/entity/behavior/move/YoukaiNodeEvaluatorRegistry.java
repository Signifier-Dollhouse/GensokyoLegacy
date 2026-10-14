package dev.xkmc.gensokyolegacy.content.entity.behavior.move;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyArea;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodValue;
import dev.xkmc.gensokyolegacy.content.block.deco.door.SlidingDoorUtils;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
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
		register(BlockTags.MOB_INTERACTABLE_DOORS, (ans, context, pos, state, mob) -> {
			if (!state.hasProperty(DoorBlock.OPEN)) return ans;
			return state.getValue(DoorBlock.OPEN) ? PathType.DOOR_OPEN : PathType.DOOR_WOOD_CLOSED;
		});
		register(GLTagGen.SLIDING_DOOR, (ans, context, pos, state, mob) -> {
			if (SlidingDoorUtils.isSeatedAndOpenable(context.level(), pos))
				return PathType.DOOR_WOOD_CLOSED;
			return PathType.BLOCKED;
		});
		register(BlockTags.CLIMBABLE, (ans, context, pos, state, mob) -> {
			if (ans == PathType.OPEN) return PathType.WALKABLE;
			return ans;
		});
		registerGlobal(YoukaiNodeEvaluatorRegistry::partyFurniture);
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

	/**
	 * A guest routes around the spread: the table, what stands on it, the food, and the chairs, so
	 * she does not path across the table to reach a chair on the far side. Party guests only - a rule
	 * about who is walking is not a rule about a block, so this is a global handler taking the mob.
	 * "Is this food" is {@link PartyFoodValue#isFood} and nothing else: {@code PartyFoodHandler.claims}
	 * says which handler was written for a block, leaving a dish with no bespoke one walkable.
	 *
	 * @see "doc/design/party/guest.md"
	 */
	public static PathType partyFurniture(
			PathType ans, PathfindingContext context, BlockPos pos,
			BlockState state, @Nullable Mob mob) {
		if (ans == PathType.BLOCKED) return ans;
		// the party flag lives on the entity rather than synced data, so anything else is
		// somebody else's problem
		if (!(mob instanceof YoukaiEntity youkai) || !youkai.getFlag(YoukaiFlags.PARTYING)) return ans;
		if (youkai.blockPosition().getY() == pos.getY()) return ans;
		// a chair is furniture to stand beside, never a floor to walk over
		if (state.is(GLTagGen.PARTY_SEAT) || state.is(GLTagGen.PARTY_TABLE)) return PathType.BLOCKED;
		if (context.level().getBlockState(pos.below()).is(GLTagGen.PARTY_TABLE))
			return PathType.DANGER_OTHER;
		// the block below is the one she would be standing on food with
		if (PartyFoodValue.isFood(context.level(), pos, state)) return PathType.BLOCKED;
		if (PartyFoodValue.isFood(context.level(), pos.below(), context.level().getBlockState(pos.below())))
			return PathType.DANGER_OTHER;
		return ans;
	}

	static PathType apply(PathType ans, PathfindingContext context, BlockPos pos, @Nullable Mob mob) {
		BlockState state = context.getBlockState(pos);
		var handler = BLOCK_HANDLERS.get(state.getBlock());
		if (handler != null) ans = handler.getPathType(ans, context, pos, state, mob);
		for (var entry : TAG_HANDLERS.entrySet()) {
			if (state.is(entry.getKey())) ans = entry.getValue().getPathType(ans, context, pos, state, mob);
		}
		for (var global : GLOBAL_HANDLERS) {
			ans = global.getPathType(ans, context, pos, state, mob);
		}
		return ans;
	}

}