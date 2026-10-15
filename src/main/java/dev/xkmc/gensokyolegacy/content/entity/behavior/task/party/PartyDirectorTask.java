package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartySend;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The part of the machine that is not a state. An always-task under {@code PARTY}, running
 * alongside whichever exclusive task is going <i>and alongside the walk itself</i>, handling the
 * things that cut across all of them: the party ending under her, the party closing mid-course,
 * her chair being taken away by the layout rescan, and a walk the pathfinder has already failed.
 *
 * @see "doc/design/party/guest.md"
 */
public class PartyDirectorTask extends PartyTask {

	/**
	 * Drops {@code WALK_TARGET: VALUE_ABSENT} from the entry conditions, because an outstanding
	 * walk order is the state this task exists to watch — with the condition kept, {@code Brain}
	 * cannot start it while she is walking and {@link #canStillUse} stops it the moment she is,
	 * so the stall clock would only ever be read on the tick after the walk gave up. The duration
	 * is a minute: {@link Behavior} stops on it and {@code Brain} starts it again, which costs
	 * nothing here because {@link #start} has nothing to do.
	 */
	public PartyDirectorTask() {
		super(Map.of(
				MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), 1200);
	}

	@Override
	protected boolean canStillUse(ServerLevel level, SmartYoukaiEntity entity, long gameTime) {
		// an always-task: it is not part of the cascade and never yields, not even to the walk
		return true;
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		var module = ctx.party();
		var party = ctx.attachment();

		// the party ended or its chunk unloaded: nothing here can be finished
		if (party == null) {
			ctx.dismiss();
			return;
		}
		// closing is a request, not a discard, and she may be halfway across the room. The wait
		// is drawn here so a table does not empty inside one tick, and the gift rides along.
		if ((module.leaving() || party.phase() == PartyAttachment.Phase.CLOSING)
				&& !ctx.leaving()) {
			ctx.armLeave();
		}
		// her chair was taken away under her. Dropping the claim is enough: a guest carrying
		// a serving goes to GoToSeat, finds no seat, and banks it standing.
		if (!module.leaving() && module.layoutChanged(party)) module.dropSeat(level);

		// a walk that has stopped closing on its target, which the pathfinder has already
		// failed. A guest crossing a long room is not stuck and must not be teleported.
		module.noteWalk(ctx.gameTime());
		if (module.walkStalled(ctx.gameTime(), PartyTasks.WALK_STALL)) teleportTo(ctx, module);
	}

	/** A guest stuck in a doorway for the rest of a party is worse than one that blinks. */
	private void teleportTo(PartyContext ctx, PartyModule module) {
		var target = module.walkTarget();
		if (target == null) return;
		var spot = landing(ctx.level(), target);
		// nothing to put her on: she keeps standing where she is and is asked again next timeout
		if (spot == null) return;
		var entity = ctx.entity();
		if (entity.isPassenger()) entity.stopRiding();
		entity.teleportTo(spot.x, spot.y, spot.z);
		entity.getNavigation().stop();
		entity.navCtrl.stopMoving();
		ctx.clearWalk();
	}

	/**
	 * Where to put her: on the block she was told to walk to, which the order already found free,
	 * and one ring out of it if something has been built there since. It has to land inside the
	 * walk reach, because a teleport she does not count as an arrival is one she is given again
	 * every timeout for as long as the party lasts.
	 */
	@Nullable
	private static Vec3 landing(ServerLevel level, BlockPos target) {
		if (PartySend.isStandable(level, target)) return target.getCenter();
		var spot = PartySend.freeSpaceNear(level, target, 1);
		if (spot == null) return null;
		var within = PartyTarget.WALK_REACH * PartyTarget.WALK_REACH;
		return new AABB(target).distanceToSqr(spot) <= within ? spot : null;
	}

	@Override
	protected boolean blocksWhileLeaving() {
		// the director is what arms the departure, so it must still run once she is leaving
		return false;
	}

	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		// an always-task: it is not part of the cascade, so no signature ever hands over from it
		return true;
	}
}
