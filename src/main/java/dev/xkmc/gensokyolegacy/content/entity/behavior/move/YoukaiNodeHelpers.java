package dev.xkmc.gensokyolegacy.content.entity.behavior.move;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodValue;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import org.jetbrains.annotations.Nullable;

public class YoukaiNodeHelpers {
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

	public static PathType trapdoor(PathType ans, PathfindingContext context, BlockPos pos, BlockState state, Mob mob) {
		if (!state.hasProperty(TrapDoorBlock.OPEN) ||
				!state.hasProperty(TrapDoorBlock.FACING) ||
				!state.hasProperty(TrapDoorBlock.HALF))
			return ans;
		var open = state.getValue(TrapDoorBlock.OPEN);
		if (open) {
			var facing = state.getValue(TrapDoorBlock.FACING);
			return context.getBlockState(pos.relative(facing)).isSolid() ? PathType.WALKABLE : PathType.BLOCKED;
		} else {
			var hinge = state.getValue(TrapDoorBlock.HALF);
			if (hinge == Half.BOTTOM)
				return context.getBlockState(pos.below()).isSolid() ? PathType.WALKABLE : PathType.BLOCKED;
			else
				return context.getBlockState(pos.above()).isSolid() ? PathType.OPEN : PathType.BLOCKED;
		}
	}
}
