package dev.xkmc.gensokyolegacy.content.entity.behavior.task.core;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

/**
 * The whole of what {@link GLBrains#NAP} does: lie down and stay there until something
 * else comes up.
 * <p>
 * This task is the pose; {@link GLBrains#MEM_NAP} is the clock. Nothing here times out,
 * because the length of a nap belongs to the memory that admitted her to it. A task with
 * a duration of its own would end the pose while the activity went on, leaving her in NAP,
 * standing up, doing nothing, for the rest of the memory's life.
 * <p>
 * {@link #canStillUse} is load-bearing rather than decoration. {@code Brain} does not
 * stop the behaviors of an activity it leaves - {@code getRunningBehaviors} walks every
 * activity's set, so a preempted nap keeps being ticked and would hold the pose straight
 * through whatever interrupted it. Checking the activity here is what ends a nap the
 * moment something above it on the priority scale takes over: a fight, a conversation,
 * or a party.
 *
 * @see GLBrains#NAP
 */
public class YoukaiNapTask<E extends SmartYoukaiEntity> extends Behavior<E> {

	public YoukaiNapTask() {
		super(Map.of(GLBrains.MEM_NAP.get(), MemoryStatus.VALUE_PRESENT), 0, 0);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, E owner) {
		// A nap is horizontal. She is only ever admitted to one while standing on the
		// ground with navigation done, so the ways to be caught out are knockback and
		// being picked up, both of which land her somewhere a nap would look wrong.
		return owner.onGround() && !owner.isPassenger();
	}

	@Override
	protected void start(ServerLevel level, E entity, long gameTime) {
		entity.setFlag(YoukaiFlags.NAPPING, true);
		// She is lying down: drop whatever was walking her somewhere, or she gets up
		// from the nap still strolling. Clearing the memory is enough - YoukaiMoveTask
		// notices it is gone in the same tick and stops the navigation on its way out.
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
		return entity.getActivity() == GLBrains.NAP.get() && entity.onGround();
	}

	@Override
	protected void stop(ServerLevel level, E entity, long gameTime) {
		entity.setFlag(YoukaiFlags.NAPPING, false);
	}

}