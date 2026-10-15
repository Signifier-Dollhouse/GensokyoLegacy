package dev.xkmc.gensokyolegacy.content.entity.behavior.task.core;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

/**
 * The whole of what {@link GLBrains#NAP} does: lie down and stay there until something
 * more urgent comes up.
 * <p>
 * This task is the pose; {@link GLBrains#MEM_NAP} is the clock. Nothing here times out,
 * because the length of a nap belongs to the memory that admitted her to it.
 * <p>
 * A conversation is the one thing that does not get her up: {@link #canStillUse} accepts
 * {@link GLBrains#TALK} as well as {@link GLBrains#NAP}, so a nap covers the whole of a
 * conversation rather than ending the moment somebody says something, and {@link #WAKE_UP_TICKS}
 * keeps the two from drifting apart at the end of it.
 * <p>
 * That check is load-bearing rather than decoration. {@code Brain} does not stop the
 * behaviors of an activity it leaves, so a preempted nap keeps being ticked and would hold the
 * pose straight through whatever interrupted it.
 *
 * @see GLBrains#NAP
 */
public class YoukaiNapTask<E extends SmartYoukaiEntity> extends Behavior<E> {

	/**
	 * How long she stays down after a conversation that outlived her nap, so she gets up
	 * on a clock rather than the instant the last line of dialogue is closed - standing up
	 * from three seconds of lying still reads as getting up, standing up from nothing reads
	 * as a snap. A fresh nap rolled while she is still lying here cancels the countdown
	 * instead, and she simply keeps napping.
	 */
	private static final int WAKE_UP_TICKS = 60;

	/**
	 * When a conversation started while she was down, and whether one has. {@link #tick} writes
	 * them and {@link #canStillUse} reads them, because {@code Behavior#tickOrStop} checks
	 * {@code canStillUse} <em>before</em> it ticks: a wake-up written in {@code tick} would arrive
	 * one tick too late, on the tick she is already being told to stand. The flag keeps the
	 * arithmetic honest - a "never talked" sentinel in {@link #lastTalkTime} would have to be a
	 * negative long, and subtracting one overflows to a negative difference that reads as "just
	 * stopped talking".
	 */
	private boolean talkedWhileDown;
	private long lastTalkTime;

	public YoukaiNapTask() {
		super(Map.of(GLBrains.MEM_NAP.get(), MemoryStatus.VALUE_PRESENT), 0, 0);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, E owner) {
		// A nap is horizontal. She is only ever admitted to one while standing on the
		// ground with navigation done, so the ways to be caught out are knockback and pickup.
		return owner.onGround() && !owner.isPassenger();
	}

	@Override
	protected void start(ServerLevel level, E entity, long gameTime) {
		entity.setFlag(YoukaiFlags.NAPPING, true);
		// She is lying down: drop whatever was walking her somewhere, or she gets up
		// from the nap still strolling. Clearing the memory is enough - YoukaiMoveTask stops.
		entity.getNavigation().stop();
		BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
		BrainUtils.clearMemory(entity, MemoryModuleType.LOOK_TARGET);
	}

	@Override
	protected boolean timedOut(long gameTime) {
		return false;
	}

	@Override
	protected boolean canStillUse(ServerLevel level, E entity, long gameTime) {
		if (!entity.onGround()) return false;
		var activity = entity.getActivity();
		if (activity == GLBrains.NAP.get() || activity == GLBrains.TALK.get()) return true;
		return this.talkedWhileDown && gameTime - this.lastTalkTime < WAKE_UP_TICKS;
	}

	@Override
	protected void tick(ServerLevel level, E entity, long gameTime) {
		if (entity.getActivity() != GLBrains.TALK.get()) return;
		this.talkedWhileDown = true;
		this.lastTalkTime = gameTime;
	}

	@Override
	protected void stop(ServerLevel level, E entity, long gameTime) {
		entity.setFlag(YoukaiFlags.NAPPING, false);
		this.talkedWhileDown = false;
	}

}