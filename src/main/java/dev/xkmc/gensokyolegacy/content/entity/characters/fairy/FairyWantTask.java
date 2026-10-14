package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;

/**
 * Every so often a fairy lets slip what she is hoping for: a bubble up beside her head
 * with one item in it, up for a few seconds and then gone. Nobody has to be looking, and
 * nothing is owed - it is her thinking out loud, and the point of it is that the
 * player learns what to hand her without being told.
 * <p>
 * What she wants and where it is put are not decided here.
 * {@link FairyEntity#broadcastWant} rolls both out of {@link FairyEntity#wantItems()}
 * and sends them as one entity event, so the event that starts the bubble carries both
 * halves of it. This class is only the clock: when it is time, ask her for one.
 * <p>
 * A {@link BehaviorControl} with its own timer rather than an entry in the random
 * pool. The pool is one slot shared with the strolls, the do-nothing and the
 * stay-put tasks, and a want added to it would come up whenever that slot happened to
 * land on it - three in a row, then none for a minute. Ten to twenty seconds is a
 * number this feature actually has, so the interval lives here and the task is
 * registered as an always-behavior: it runs for as long as she is on one of the idle
 * activities, counting.
 * <p>
 * Being an always-behavior is also what keeps the bubble out of everything else. It
 * is only registered on {@code IDLE}, {@code PLAY} and {@code AT_HOME} (see
 * {@link FairyEntity#constructTaskBoard}), and an activity that is not running runs
 * none of its behaviors - so a fight, a conversation or a dish does not interrupt a
 * want, the countdown simply pauses across it and picks up where it was.
 */
public class FairyWantTask implements BehaviorControl<FairyEntity> {

	/**
	 * Ten to twenty seconds between two wants, both ends inclusive.
	 */
	private static final int MIN_DELAY = 200;
	private static final int MAX_DELAY = 400;

	private Behavior.Status status = Behavior.Status.STOPPED;

	/**
	 * Ticks left until the next want.
	 * <p>
	 * Kept across the behavior stopping and starting again, so a fairy interrupted by a
	 * fight does not come back and start her ten seconds over. Rolled on the first tick
	 * rather than at construction because the roll wants a random source to come off an
	 * entity, and the behavior is built once per brain while the entity is only handed
	 * to it later.
	 */
	private int delay;

	@Override
	public Behavior.Status getStatus() {
		return status;
	}

	@Override
	public boolean tryStart(ServerLevel level, FairyEntity entity, long gameTime) {
		status = Behavior.Status.RUNNING;
		if (delay <= 0) delay = roll(entity);
		return true;
	}

	@Override
	public void tickOrStop(ServerLevel level, FairyEntity entity, long gameTime) {
		if (--delay > 0) return;
		delay = roll(entity);
		entity.broadcastWant(entity.getRandom());
	}

	@Override
	public void doStop(ServerLevel level, FairyEntity entity, long gameTime) {
		status = Behavior.Status.STOPPED;
	}

	@Override
	public String debugString() {
		return getClass().getSimpleName();
	}

	private static int roll(FairyEntity entity) {
		return entity.getRandom().nextInt(MIN_DELAY, MAX_DELAY + 1);
	}

}
