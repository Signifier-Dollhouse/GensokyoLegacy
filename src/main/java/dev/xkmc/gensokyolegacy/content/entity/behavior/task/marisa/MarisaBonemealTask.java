package dev.xkmc.gensokyolegacy.content.entity.behavior.task.marisa;

import dev.xkmc.gensokyolegacy.content.entity.behavior.task.home.AbstractHomeHolderTask;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Map;

/**
 * Marisa's outdoor gardening: apply bonemeal to a grass block within 12 blocks
 * of the house, but outside the house itself. At most once per minute.
 */
public class MarisaBonemealTask<E extends SmartYoukaiEntity> extends AbstractHomeHolderTask<E> {

	private static final int TRIALS = 24;
	private static final int USE_COOLDOWN = 1200;

	private BlockPos target;
	private long walkEnd;
	private long nextUse;

	public MarisaBonemealTask() {
		super(Map.of(
				MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT,
				MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
		));
		cooldownFor(e -> e.getRandom().nextInt(200, 400));
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
		if (!super.checkExtraStartConditions(level, entity)) return false;
		if (level.getGameTime() < nextUse) return false;
		BoundingBox house = home.getHouseBound();
		if (house == null) return false;
		target = findTarget(level, entity, house, MarisaTaskUtil.outdoorBox(house));
		return target != null;
	}

	@Override
	protected void start(ServerLevel level, E entity, long gameTime) {
		BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET, new WalkTarget(target, 1, 2));
		BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target));
		walkEnd = gameTime + 200;
	}

	@Override
	protected boolean canStillUse(ServerLevel level, E entity, long gameTime) {
		if (!home.isValid() || target == null) return false;
		if (!level.isLoaded(target) || !isValidGrass(level, target)) return false;
		if (entity.distanceToSqr(target.getCenter()) < 9) {
			entity.swing(InteractionHand.MAIN_HAND);
			if (BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), level, target, null)) {
				level.levelEvent(1505, target, 15);
				nextUse = gameTime + USE_COOLDOWN;
			}
			BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
			BrainUtils.clearMemory(entity, MemoryModuleType.LOOK_TARGET);
			return false;
		}
		return gameTime < walkEnd;
	}

	@Override
	protected void stop(ServerLevel level, E entity, long gameTime) {
		target = null;
		walkEnd = 0;
		super.stop(level, entity, gameTime);
	}

	private static BlockPos findTarget(ServerLevel level, SmartYoukaiEntity entity, BoundingBox house, BoundingBox area) {
		for (int i = 0; i < TRIALS; i++) {
			BlockPos pos = MarisaTaskUtil.randomSurfacePos(level, area, entity.getRandom());
			if (pos == null || !area.isInside(pos)) continue;
			if (house.isInside(pos)) continue;
			if (isValidGrass(level, pos)) return pos;
		}
		return null;
	}

	private static boolean isValidGrass(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).is(Blocks.GRASS_BLOCK) &&
				level.isEmptyBlock(pos.above());
	}

}
