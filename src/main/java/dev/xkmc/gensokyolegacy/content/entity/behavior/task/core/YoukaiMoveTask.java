package dev.xkmc.gensokyolegacy.content.entity.behavior.task.core;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.IHomeHolder;
import dev.xkmc.gensokyolegacy.content.entity.behavior.move.CompoundPath;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YoukaiMoveTask<E extends YoukaiEntity> extends Behavior<E> {

	private static final double DRIFT_SQR = 9.0;
	private static final int DRIFT_REPATH_INTERVAL = 20;

	@Nullable
	protected CompoundPath path;
	@Nullable
	protected BlockPos lastTargetPos;
	protected float speedModifier;
	private int cooldown;
	private int leaveGroundTick;
	private long lastRepathTime;

	public YoukaiMoveTask() {
		super(Map.of(
				MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_PRESENT,
				GLBrains.MEM_PATH.get(), MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.PATH, MemoryStatus.REGISTERED,
				MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED
		), 150, 250);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
		if (entity.isSleeping()) return false;
		if (cooldown > entity.tickCount) return false;
		Brain<?> brain = entity.getBrain();
		WalkTarget walkTarget = BrainUtils.getMemory(brain, MemoryModuleType.WALK_TARGET);
		if (walkTarget != null &&
				!hasReachedTarget(entity, walkTarget) &&
				attemptNewPath(entity, walkTarget, false)
		) {
			this.lastTargetPos = walkTarget.getTarget().currentBlockPosition();
			return true;
		}
		BrainUtils.clearMemory(brain, MemoryModuleType.WALK_TARGET);
		BrainUtils.clearMemory(brain, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		return false;
	}

	@Override
	protected boolean canStillUse(ServerLevel level, E entity, long gameTime) {
		if (entity.isSleeping())
			return false;
		if (this.path == null || this.lastTargetPos == null)
			return false;
		if (entity.getNavigation().isDone())
			return false;
		WalkTarget walkTarget = BrainUtils.getMemory(entity, MemoryModuleType.WALK_TARGET);
		return walkTarget != null && !hasReachedTarget(entity, walkTarget);
	}

	@Override
	protected void start(ServerLevel level, E entity, long gameTime) {
		lastRepathTime = gameTime;
		BrainUtils.setMemory(entity, MemoryModuleType.PATH, path == null ? null : path.path());
		BrainUtils.setMemory(entity, GLBrains.MEM_PATH.get(), this.path);
		if (path == null) return;
		entity.navCtrl.moveTo(this.path, this.speedModifier);
	}

	@Override
	protected void tick(ServerLevel level, E entity, long gameTime) {
		CompoundPath path = entity.navCtrl.getPath();
		Brain<?> brain = entity.getBrain();
		if (this.path != path) {
			this.path = path;
			BrainUtils.setMemory(brain, MemoryModuleType.PATH, path == null ? null : path.path());
			BrainUtils.setMemory(brain, GLBrains.MEM_PATH.get(), path);
		}
		if (path != null && this.lastTargetPos != null) {
			WalkTarget target = BrainUtils.getMemory(brain, MemoryModuleType.WALK_TARGET);
			if (target == null) return;
			if (target.getTarget().currentBlockPosition().distSqr(this.lastTargetPos) > 4) {
				repath(level, entity, gameTime, target);
			} else if (gameTime - lastRepathTime > DRIFT_REPATH_INTERVAL &&
					isDriftedFromPath(entity, path.path())) {
				repath(level, entity, gameTime, target);
			}
		}
	}

	private void repath(ServerLevel level, E entity, long gameTime, WalkTarget target) {
		if (attemptNewPath(entity, target, hasReachedTarget(entity, target))) {
			this.lastTargetPos = target.getTarget().currentBlockPosition();
			Brain<?> brain = entity.getBrain();
			BrainUtils.setMemory(brain, MemoryModuleType.PATH, this.path == null ? null : this.path.path());
			BrainUtils.setMemory(brain, GLBrains.MEM_PATH.get(), this.path);
			if (this.path != null) entity.navCtrl.moveTo(this.path, this.speedModifier);
		}
		lastRepathTime = gameTime;
	}

	@Override
	protected void stop(ServerLevel level, E entity, long gameTime) {
		Brain<?> brain = entity.getBrain();
		var target = BrainUtils.getMemory(brain, MemoryModuleType.WALK_TARGET);
		if (!entity.getNavigation().isStuck() ||
				!BrainUtils.hasMemory(brain, MemoryModuleType.WALK_TARGET) ||
				target != null && hasReachedTarget(entity, target)
		) cooldown = 0;
		else cooldown = entity.tickCount + entity.getRandom().nextInt(40);
		entity.getNavigation().stop();
		BrainUtils.clearMemories(brain, MemoryModuleType.WALK_TARGET, MemoryModuleType.PATH, GLBrains.MEM_PATH.get());
		this.path = null;
	}

	protected boolean attemptNewPath(E entity, WalkTarget walkTarget, boolean reachedCurrentTarget) {
		Brain<?> brain = entity.getBrain();
		if (reachedCurrentTarget) {
			BrainUtils.clearMemory(brain, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
			return false;
		}
		BlockPos finalPos = walkTarget.getTarget().currentBlockPosition();
		Vec3 pos = Vec3.atBottomCenterOf(stageViaInterior(entity, finalPos));
		entity.getNavigation().moveTo(pos.x, pos.y, pos.z, 0, walkTarget.getSpeedModifier());
		this.path = entity.navCtrl.getPath();
		this.speedModifier = walkTarget.getSpeedModifier();
		if (this.path != null && this.path.path().canReach()) {
			BrainUtils.clearMemory(brain, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		} else {
			BrainUtils.setMemory(brain, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, entity.level().getGameTime());
		}
		if (this.path != null) return true;
		Vec3 nextPos = DefaultRandomPos.getPosTowards(entity, 10, 7, pos, Mth.HALF_PI);
		if (nextPos != null) {
			entity.getNavigation().moveTo(nextPos.x(), nextPos.y(), nextPos.z(), 0, 1);
			this.path = entity.navCtrl.getPath();
			return this.path != null;
		}
		return false;
	}

	protected boolean hasReachedTarget(E entity, WalkTarget target) {
		return target.getTarget().currentBlockPosition().distManhattan(entity.blockPosition()) <= target.getCloseEnoughDist();
	}

	/**
	 * When the entity and its final target sit in different interior rooms
	 * of the same home, head for the next node on the room route instead of
	 * the final target, so each vanilla search stays short and in-room.
	 * Chaining happens through behavior restarts: reaching a staging node
	 * ends navigation with the final WalkTarget still pending, and the next
	 * attempt plans the following leg from the new room. Returns the final
	 * target whenever staging does not apply.
	 */
	private static BlockPos stageViaInterior(YoukaiEntity entity, BlockPos finalPos) {
		if (!(entity instanceof SmartYoukaiEntity smart)) return finalPos;
		if (!(entity.level() instanceof ServerLevel level)) return finalPos;
		if (entity.navCtrl.isFlying()) return finalPos;
		var home = IHomeHolder.of(level, smart);
		if (home == null || !home.isValid()) return finalPos;
		var interior = home.getInterior();
		if (interior.isEmpty()) return finalPos;
		int from = interior.roomIndexOf(entity.blockPosition());
		int to = interior.roomIndexOf(finalPos);
		if (from < 0 || to < 0 || from == to) return finalPos;
		var route = interior.findRoute(from, to);
		if (route.isEmpty()) return finalPos;
		List<BlockPos> waypoints = new ArrayList<>();
		int side = from;
		for (var edge : route) {
			if (side == edge.roomA()) {
				waypoints.add(edge.posA());
				if (edge.isDual()) waypoints.add(edge.posB());
				side = edge.roomB();
			} else if (side == edge.roomB()) {
				if (edge.isDual()) waypoints.add(edge.posB());
				waypoints.add(edge.posA());
				side = edge.roomA();
			} else {
				return finalPos;
			}
		}
		if (side != to) return finalPos;
		BlockPos feet = entity.blockPosition();
		for (var waypoint : waypoints) {
			if (waypoint.distSqr(feet) > 4) return waypoint;
		}
		return finalPos;
	}

	/**
	 * Whether the entity drifted away from the remaining path polyline. Vanilla
	 * navigation just steers toward the next node when this happens, cutting
	 * straight through whatever is in between, so detect it and re-path instead.
	 */
	private static boolean isDriftedFromPath(YoukaiEntity entity, Path path) {
		if (path.getNodeCount() == 0 || path.isDone()) return false;
		Vec3 pos = entity.position();
		int start = Math.max(0, path.getNextNodeIndex() - 1);
		Vec3 prev = Vec3.atBottomCenterOf(path.getNodePos(start));
		if (start == path.getNodeCount() - 1)
			return prev.distanceToSqr(pos) > DRIFT_SQR;
		for (int i = start + 1; i < path.getNodeCount(); i++) {
			Vec3 cur = Vec3.atBottomCenterOf(path.getNodePos(i));
			if (distToSegmentSqr(pos, prev, cur) <= DRIFT_SQR)
				return false;
			prev = cur;
		}
		return true;
	}

	private static double distToSegmentSqr(Vec3 p, Vec3 a, Vec3 b) {
		double abx = b.x - a.x;
		double aby = b.y - a.y;
		double abz = b.z - a.z;
		double lenSqr = abx * abx + aby * aby + abz * abz;
		if (lenSqr < 1e-8) return p.distanceToSqr(a);
		double t = ((p.x - a.x) * abx + (p.y - a.y) * aby + (p.z - a.z) * abz) / lenSqr;
		t = Math.max(0, Math.min(1, t));
		double dx = p.x - (a.x + abx * t);
		double dy = p.y - (a.y + aby * t);
		double dz = p.z - (a.z + abz * t);
		return dx * dx + dy * dy + dz * dz;
	}

}