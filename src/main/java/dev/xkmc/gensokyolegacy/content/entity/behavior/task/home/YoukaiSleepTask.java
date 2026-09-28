package dev.xkmc.gensokyolegacy.content.entity.behavior.task.home;

import dev.xkmc.gensokyolegacy.content.attachment.index.BedRefData;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class YoukaiSleepTask extends Behavior<YoukaiEntity> {

	@Nullable
	private GlobalPos pos = null;
	private long desperateSleepyTime = 0;
	private long nextOkStartTime = 0;

	public YoukaiSleepTask() {
		super(Map.of(
				MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT,
				MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT
		));
	}

	@Override
	protected boolean canStillUse(ServerLevel level, YoukaiEntity entity, long gameTime) {
		return entity.getBrain().isActive(Activity.REST);
	}

	@Override
	protected void start(ServerLevel level, YoukaiEntity entity, long gameTime) {
		if (gameTime < nextOkStartTime) return;
		pos = BrainUtils.getMemory(entity, MemoryModuleType.HOME);
		if (pos == null || !level.dimension().equals(pos.dimension())) return;
		desperateSleepyTime = gameTime + 1200;
		BlockPos[] ends = bedEnds(level);
		if (ends == null) return;
		if (bedDistSqr(entity, ends) > 4) {
			BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET,
					new WalkTarget(scanApproach(level, entity, ends), 1, 2));
		}
	}

	@Override
	protected void stop(ServerLevel level, YoukaiEntity entity, long gameTime) {
		// Sleep exit always releases the bed approach: when REST ends mid-approach
		// the entity is awake but still holds this task's WALK_TARGET, which would
		// keep MoveTask walking on a stale order and block tasks that require
		// WALK_TARGET absent (e.g. YoukaiGoHomeTask) from starting.
		entity.getNavigation().stop();
		BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
		if (!entity.isSleeping()) return;
		entity.stopSleeping();
		this.nextOkStartTime = gameTime + 40L;
		this.desperateSleepyTime = 0;
	}

	@Override
	protected void tick(ServerLevel level, YoukaiEntity entity, long gameTime) {
		if (pos == null) return;
		if (entity.isSleeping()) return;
		if (desperateSleepyTime > 0 && gameTime > desperateSleepyTime) {
			if (level.isLoaded(pos.pos())) {
				entity.moveTo(pos.pos().getCenter());
			} else {
				BedRefData.of(level, entity).ifPresent(bed -> bed.removeEntity(level, entity));
			}
			desperateSleepyTime = 0;
		}
		BlockPos[] ends = bedEnds(level);
		if (ends == null) {
			pos = null;
			return;
		}
		if (bedDistSqr(entity, ends) < 4) {
			BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
			entity.getNavigation().stop();
			// Sleep with the head on the head block, mirroring vanilla
			// BedBlock.useWithoutItem: the head sits one step along FACING from
			// the foot, so ends[0] is always the head (see bedEnds).
			entity.startSleeping(ends[0]);
		} else if (!BrainUtils.hasMemory(entity, MemoryModuleType.WALK_TARGET)) {
			BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET,
					new WalkTarget(scanApproach(level, entity, ends), 1, 0));
		}
	}

	/**
	 * Head and foot of the home bed ({@code ends[0]} is always the head).
	 * Returns null when the home bed block is gone.
	 */
	@Nullable
	private BlockPos[] bedEnds(ServerLevel level) {
		if (pos == null) return null;
		var state = level.getBlockState(pos.pos());
		if (!state.hasProperty(BedBlock.PART)) return null;
		BlockPos home = pos.pos();
		Direction facing = state.getValue(BedBlock.FACING);
		boolean isHead = state.getValue(BedBlock.PART) == BedPart.HEAD;
		return new BlockPos[]{
				isHead ? home : home.relative(facing),
				isHead ? home.relative(facing.getOpposite()) : home};
	}

	private static double bedDistSqr(YoukaiEntity entity, BlockPos[] ends) {
		return Math.min(entity.distanceToSqr(ends[0].getCenter()), entity.distanceToSqr(ends[1].getCenter()));
	}

	/**
	 * Walk target for the bed approach: the nearest standable cell in the ring
	 * around the bed (4 horizontal neighbors of each bed end). No {@code above()}:
	 * standing on top of the mattress is never required since sleep entry
	 * teleports onto the head block. Falls back to the nearer bed end itself when
	 * every neighbor is blocked, so the fly fallback and desperate teleport still
	 * have a closest-reachable endpoint to work with.
	 */
	private static BlockPos scanApproach(ServerLevel level, YoukaiEntity entity, BlockPos[] ends) {
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (BlockPos end : ends) {
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				BlockPos cell = end.relative(dir);
				if (!isStandable(level, cell)) continue;
				double dist = entity.distanceToSqr(cell.getCenter());
				if (dist < bestDist) {
					bestDist = dist;
					best = cell;
				}
			}
		}
		if (best != null) return best;
		return entity.distanceToSqr(ends[0].getCenter()) <= entity.distanceToSqr(ends[1].getCenter()) ?
				ends[0] : ends[1];
	}

	private static boolean isStandable(ServerLevel level, BlockPos cell) {
		if (!level.isLoaded(cell)) return false;
		BlockPos floor = cell.below();
		if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return false;
		if (!level.getBlockState(cell).getCollisionShape(level, cell).isEmpty()) return false;
		return level.getBlockState(cell.above()).getCollisionShape(level, cell.above()).isEmpty();
	}

	@Override
	protected boolean timedOut(long gameTime) {
		return false;
	}

}
