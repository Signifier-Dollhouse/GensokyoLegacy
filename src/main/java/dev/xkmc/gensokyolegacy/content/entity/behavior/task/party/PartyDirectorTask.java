package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartySend;
import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;

/**
 * The part of the machine that is not a state.
 *
 * <p>
 * Registered as an always-task under {@code PARTY}, so it runs alongside whichever of the
 * eleven exclusive tasks is going, and handles the three things that cut across all of
 * them: the party ending under her, the party closing while she is mid-course, and her own
 * chair being taken away by the layout rescan.
 */
public class PartyDirectorTask extends PartyTask {

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		var module = ctx.party();
		var party = ctx.attachment();

		// the party ended or its chunk unloaded: nothing here can be finished
		if (party == null) {
			ctx.dismiss();
			return;
		}
		// closing is a request, not a discard, and she may be halfway across the room.
		// The wait is drawn here so a table does not empty inside one tick, and the gift
		// rides along: this is the door the host calling it off sends her out by, and it
		// used to be the one departure that left nothing on the table.
		if ((module.leaving() || party.phase() == PartyAttachment.Phase.CLOSING)
				&& !BrainUtils.hasMemory(entity, GLBrains.MEM_PARTY_LEAVE.get())) {
			ctx.armLeave(entity.getRandom().nextInt(PartyTasks.UNSEAT_DELAY + 1));
		}
		// her chair was taken away under her. Dropping the claim is enough: a guest carrying
		// a serving goes to GoToSeat, finds no seat, and banks it standing.
		if (!module.leaving() && module.layoutChanged(party)) module.dropSeat(level);

		// a walk that has stopped closing on its target, which the pathfinder has already
		// failed. A guest crossing a long room is not stuck and must not be teleported.
		module.noteWalk(ctx.gameTime());
		if (module.walkStalled(ctx.gameTime(), PartyTasks.WALK_STALL)) teleportBeside(ctx, module);
	}

	/** A guest stuck in a doorway for the rest of a party is worse than one that blinks. */
	private void teleportBeside(PartyContext ctx, PartyModule module) {
		var target = module.walkTarget();
		if (target == null) return;
		var spot = PartySend.freeSpaceNear(ctx.level(), target, 2);
		if (spot == null) return;
		var entity = ctx.entity();
		if (entity.isPassenger()) entity.stopRiding();
		entity.teleportTo(spot.x, spot.y, spot.z);
		entity.getNavigation().stop();
		entity.navCtrl.stopMoving();
		ctx.clearWalk();
	}

	@Override
	protected boolean blocksWhileLeaving() {
		// the director is what arms the departure, so it must still run once she is leaving
		return false;
	}

	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		// an always-task: it is not part of the cascade and never yields
		return true;
	}
}