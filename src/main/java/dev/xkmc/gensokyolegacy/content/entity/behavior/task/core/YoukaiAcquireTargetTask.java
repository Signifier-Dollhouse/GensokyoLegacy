package dev.xkmc.gensokyolegacy.content.entity.behavior.task.core;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Hands her a target to fight. She already has a queue of them -
 * {@link dev.xkmc.gensokyolegacy.content.entity.behavior.combat.YoukaiTargetContainer}
 * remembers the last few - so this is only about when that queue is skipped in favour of
 * looking around. Whether she searches at all is asked of the entity
 * ({@link SmartYoukaiEntity#shouldSearchTarget}) rather than fixed here, because it is a
 * property of the activity, not of the task: idle and play are her own time, while a
 * conversation or a task at home is not.
 */
public class YoukaiAcquireTargetTask<E extends SmartYoukaiEntity> extends Behavior<E> {

	@Nullable
	protected LivingEntity toTarget = null;

	public YoukaiAcquireTargetTask() {
		super(Map.of(
				MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT
		));
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, E owner) {
		this.toTarget = owner.targets.getPrimaryTarget();
		if (this.toTarget == null && owner.shouldSearchTarget(owner.getActivity())) {
			Brain<?> brain = owner.getBrain();
			NearestVisibleLivingEntities list = BrainUtils.getMemory(brain, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
			if (list == null) return false;
			this.toTarget = list.findClosest(owner::wouldInitiateAttack).orElse(null);
		}
		return this.toTarget != null;
	}

	@Override
	protected void start(ServerLevel level, E entity, long gameTime) {
		BrainUtils.setTargetOfEntity(entity, this.toTarget);
		BrainUtils.clearMemory(entity, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		this.toTarget = null;
	}

}
