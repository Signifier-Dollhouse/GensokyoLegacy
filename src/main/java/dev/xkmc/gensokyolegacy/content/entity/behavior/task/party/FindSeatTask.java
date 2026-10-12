package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * Find a chair and sit in it.
 *
 * <p>
 * The bottom of the cascade and the default answer for a guest with nothing to do, which is
 * why it has no signature at all: by the time the gate reaches it, everything more specific
 * has been ruled out. It is the only task that runs while she is not seated.
 *
 * <p>
 * Failing is a process, not a verdict. The host is told on the first failure, she looks again
 * every {@code SEAT_RETRY}, and the third consecutive failure ends it — because the
 * party-wide feast gate waits for <i>every</i> guest to have a chair.
 *
 * @see doc/design/party/guest.md
 */
public class FindSeatTask extends PartyTask {

	@Override
	protected boolean stillMine(SmartYoukaiEntity entity, PartyContext ctx) {
		return !ctx.seated();
	}

	@Override
	protected void start(ServerLevel level, SmartYoukaiEntity entity, long gameTime) {
		// a fresh episode, so a guest taken off her chair starts counting again from zero
		var party = entity.getModule(PartyModule.class).orElseThrow();
		if (party.seat() == null) {
			party.beginSeatEpisode(gameTime, PartyTasks.SEAT_RETRY, PartyTasks.SEAT_FAILURES);
		}
	}

	@Override
	protected void step(ServerLevel level, SmartYoukaiEntity entity, PartyContext ctx) {
		var seat = ctx.requestSeat();
		if (seat == null) {
			// paced by its own clock, not by ticks: this task runs every tick she has no chair, and
			// three failures a tick would be three failures in no time at all
			if (ctx.gameTime() < ctx.party().seatCheckAt()) return;
			ctx.party().nextSeatCheck(ctx.gameTime(), PartyTasks.SEAT_RETRY);
			if (ctx.party().countSeatFailure()) ctx.reportNoSeat();
			if (ctx.party().outOfSeatAttempts(ctx.gameTime(), PartyTasks.SEAT_FAILURES)) ctx.expel();
			return;
		}
		ctx.walkTo(seat);
		// arriving at the tile is not yet sitting down; the reach is what decides she can
		if (seat.arrived(ctx) && seat.inReach(ctx)) {
			ctx.sit(seat);
			ctx.party().beginSeatEpisode(ctx.gameTime(), PartyTasks.SEAT_RETRY, PartyTasks.SEAT_FAILURES);
		}
	}
}