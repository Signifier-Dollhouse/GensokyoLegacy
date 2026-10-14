package dev.xkmc.gensokyolegacy.content.entity.behavior.task.core;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.IHomeHolder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiAnim;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Drop-in replacement for vanilla {@link net.minecraft.world.entity.ai.behavior.DoNothing},
 * which exposes no start hook (all methods are final). Behaves identically (idles for a random
 * duration), except that standing outside her house bound with nothing to do is the one moment she
 * is allowed to do something about how she is standing. At most once every
 * {@link #IDLE_TRICK_COOLDOWN} ticks, and then either: she lies down ({@link GLBrains#NAP}, for a
 * character whose {@link SmartYoukaiEntity#napChance} is above zero, at that chance), or a
 * per-character outdoor idle one-shot is broadcast at a {@link #FIDGET_CHANCE} roll (an
 * {@code 闲置动画} fidget for most of them, none for Morichika, Sakuya and Suika). The nap is offered
 * first, so a character that naps only fidgets when the nap roll loses - not a second chance at the
 * same thing - and the cooldown is shared either way.
 */
public class YoukaiDoNothing<E extends SmartYoukaiEntity> implements BehaviorControl<E> {

	private static final long IDLE_TRICK_COOLDOWN = 200;
	private static final float FIDGET_CHANCE = 0.5f;
	/**
	 * Five to twenty seconds, and the whole of a nap: {@link GLBrains#MEM_NAP} is set
	 * with this as its expiry, so the activity ends when it runs out.
	 */
	private static final int NAP_MIN_TICKS = 100;
	private static final int NAP_MAX_TICKS = 400;

	private final int minDuration;
	private final int maxDuration;
	private Behavior.Status status = Behavior.Status.STOPPED;
	private long endTimestamp;
	private long lastIdleTrick = -1000;

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
				gameTime - lastIdleTrick >= IDLE_TRICK_COOLDOWN) {
			lastIdleTrick = gameTime;
			if (!offerNap(level, entity) && level.getRandom().nextFloat() < FIDGET_CHANCE)
				YoukaiAnim.OUTDOOR_IDLE.play(entity);
		}
		return true;
	}

	/**
	 * Write the nap memory and let the brain move her on her own, rather than putting her straight
	 * down from here: going through the activity gives the nap a length and lets
	 * {@link YoukaiNapTask} decide when she gets up. Writing the memory is enough and takes effect
	 * next tick, which is also what makes this safe from inside the activity that ends it - the
	 * brain only reconsiders its activity at the top of the tick.
	 */
	private static <E extends SmartYoukaiEntity> boolean offerNap(ServerLevel level, E entity) {
		float chance = entity.napChance();
		if (chance <= 0 || level.getRandom().nextFloat() >= chance) return false;
		var napTicks = entity.getRandom().nextInt(NAP_MIN_TICKS, NAP_MAX_TICKS + 1);
		BrainUtils.setForgettableMemory(entity, GLBrains.MEM_NAP.get(), Unit.INSTANCE, napTicks);
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
