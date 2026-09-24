package dev.xkmc.gensokyolegacy.content.entity.behavior.task.marisa;

import dev.xkmc.gensokyolegacy.content.block.nature.SideBushBlock;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.home.AbstractHomeHolderTask;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.UseMainhandAnim;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Map;

/**
 * Marisa's outdoor chore: break wild foliage within 12 blocks of the house.
 * Crops, saplings and berry bushes are never touched. Mushrooms (and other
 * mushroom-like growths) are only broken when another one of the same type
 * stands within a 5x3x5 area, so patches are never wiped out.
 */
public class MarisaForageTask<E extends SmartYoukaiEntity> extends AbstractHomeHolderTask<E> {

	private static final int TRIALS = 12;

	private BlockPos target;
	private long walkEnd;

	public MarisaForageTask() {
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
		if (!level.isLoaded(target) || !isBreakableFoliage(level, target)) return false;
		if (entity.distanceToSqr(target.getCenter()) < 9) {
			entity.swing(InteractionHand.MAIN_HAND);
			if (entity instanceof UseMainhandAnim anim) anim.broadcastUseMainhandAnim();
			level.destroyBlock(target, true, entity);
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
			if (!level.canSeeSky(pos)) continue;
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof DoublePlantBlock &&
					state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
				pos = pos.below();
				if (!area.isInside(pos) || house.isInside(pos)) continue;
			}
			if (isBreakableFoliage(level, pos)) return pos;
		}
		return null;
	}

	static boolean isBreakableFoliage(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if (block instanceof SaplingBlock) return false;
		if (block instanceof CropBlock) return false;
		if (block instanceof SweetBerryBushBlock) return false;
		if (!(block instanceof BushBlock) && !(block instanceof SideBushBlock)) return false;
		if (block instanceof MushroomBlock || block instanceof SideBushBlock) {
			return hasSameTypeNearby(level, pos, block);
		}
		return true;
	}

	private static boolean hasSameTypeNearby(ServerLevel level, BlockPos pos, Block block) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) continue;
					BlockPos p = pos.offset(dx, dy, dz);
					if (!level.isLoaded(p)) continue;
					if (level.getBlockState(p).is(block)) return true;
				}
			}
		}
		return false;
	}

}
