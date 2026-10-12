package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;


/**
 * The party is over: stand up, then go.
 *
 * <p>
 * The first line of the cascade and the only task whose signature is a single memory, so
 * anything that can end a visit is the same thing here: arm the departure and let
 * the gate find it — whether that was the spread running out, a roll on a satisfied sit, or
 * the party closing under her.
 *
 * <p>
 * Unseating and walking are separate so a table does not empty inside one tick, which reads
 * as a cutscene; the wait before it is drawn per guest rather than counted down party-wide
 * for the same reason.
 *
 * <p>
 * Everything after the walk is the module's business rather than this one's: where a
 * summoned clone goes, and what happens to a character who came as herself.
 */
public class LeaveTask extends PartyTask {


	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return ctx.leaving();
	}

	@Override
	protected boolean blocksWhileLeaving() {
		// this is the task the exit routes through, so it must start while she is leaving
		return false;
	}

	@Override
	protected boolean canRunWithoutParty() {
		// the party may reset while she is still walking out; she still leaves
		return true;
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		// the memory counts down but never goes away: see PartyContext#countLeave
		if (ctx.leaveWait() > 0) {
			ctx.countLeave();
			return;
		}
		if (ctx.seated()) ctx.dropSeat();
		// the plate leaves a dish that is already gone, so it still counts even though the
		// party is ending before the chew does
		ctx.finishServe();
		BlockPos dest = ctx.destination();
		if (dest == null) {
			// nowhere to go, or a visit whose window has closed: she leaves from here
			ctx.finishLeaving();
			return;
		}
		// looser than a dish or a chair: she is walking away, not picking anything up, and
		// the last two steps of a departure are not worth the leave timeout after them
		var target = new PartyTarget(dest, dest, PartyTasks.DEPART_REACH, PartyTasks.DEPART_REACH);
		if (target.arrived(ctx)) {
			ctx.finishLeaving();
			return;
		}
		// yields as soon as the order is out; the move task walks and this finds her arrived
		if (ctx.canWalk()) ctx.walkTo(target);
	}
}