package dev.xkmc.gensokyolegacy.content.entity.behavior.task.core;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.IHomeHolder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Drop-in replacement for vanilla {@link net.minecraft.world.entity.ai.behavior.DoNothing},
 * which exposes no start hook (all methods are final). Behaves identically
 * (idles for a random duration), except that when the current activity is
 * outdoor ({@link Activity#IDLE} or {@link Activity#PLAY}) and the entity
 * stands outside its house bound, a per-character outdoor idle one-shot is
 * broadcast at 10% chance and at most once per 10 seconds ({@code 伸懒腰}
 * for Marisa, {@code 眺望} for Reimu, none for Morichika).
 */
public class YoukaiDoNothing<E extends SmartYoukaiEntity> implements BehaviorControl<E> {

	private static final long OUTDOOR_IDLE_COOLDOWN = 200;

	private final int minDuration;
	private final int maxDuration;
	private Behavior.Status status = Behavior.Status.STOPPED;
	private long endTimestamp;
	private long lastOutdoorIdle = -1000;

	public YoukaiDoNothing(int minDuration, int maxDuration) {
		this.minDuration = minDuration;
		this.maxDuration = maxDuration;
	}

	@Override
	public Behavior.Status getStatus() {
		return status;
	}

	@Override
	public boolean tryStart(ServerLevel level, E entity, long gameTime) {
		status = Behavior.Status.RUNNING;
		int i = minDuration + level.getRandom().nextInt(maxDuration + 1 - minDuration);
		endTimestamp = gameTime + i;
		var activity = entity.getActivity();
		if ((activity == Activity.IDLE || activity == Activity.PLAY) &&
				isOutsideHouse(level, entity) && entity.getNavigation().isDone() &&
				gameTime - lastOutdoorIdle >= OUTDOOR_IDLE_COOLDOWN &&
				level.getRandom().nextFloat() < 0.5f) {
			lastOutdoorIdle = gameTime;
			YoukaiAnim.OUTDOOR_IDLE.play(entity);
		}
		return true;
	}

	private static <T extends SmartYoukaiEntity> boolean isOutsideHouse(ServerLevel level, T entity) {
		var home = IHomeHolder.of(level, entity);
		if (home == null) return true;
		return !home.isInRoom(entity.blockPosition());
	}

	@Override
	public void tickOrStop(ServerLevel level, E entity, long gameTime) {
		if (gameTime > endTimestamp) {
			doStop(level, entity, gameTime);
		}
	}

	@Override
	public void doStop(ServerLevel level, E entity, long gameTime) {
		status = Behavior.Status.STOPPED;
	}

	@Override
	public String debugString() {
		return getClass().getSimpleName();
	}

}
