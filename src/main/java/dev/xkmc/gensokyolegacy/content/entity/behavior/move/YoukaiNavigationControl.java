package dev.xkmc.gensokyolegacy.content.entity.behavior.move;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.Nullable;

public class YoukaiNavigationControl {

	private final YoukaiEntity self;
	private final CombatFlyingControl combat;
	private final NavigationDebugger debugger;
	private final ClimbMoveControl walkCtrl;
	private final FlyControl flyCtrl;
	private final Ground walkNav;
	private final Flying flyNav;

	private boolean isFlying = false;
	private int leaveGroundTick = 0;

	public YoukaiNavigationControl(YoukaiEntity self) {
		this.self = self;
		this.combat = new CombatFlyingControl(self);
		this.debugger = new NavigationDebugger(self);
		this.walkCtrl = new ClimbMoveControl(self);
		this.walkNav = new Ground(self, self.level());
		this.flyCtrl = new FlyControl(self, 10, false);
		this.flyNav = new Flying(self, self.level());
		self.setControl(walkCtrl, walkNav);
		markHuman();
	}

	public final void setFlying() {
		// Flight entry wakes: within one brain tick the new activity's behaviors
		// start before the old activity's stop, so a fight order issued while
		// asleep must end sleep first instead of flying in sleeping pose.
		if (self.isSleeping()) {
			self.stopSleeping();
		}
		flyNav.tempFly = false;
		self.setNoGravity(true);
		self.setFlag(YoukaiFlags.FLYING, true);
		if (isFlying) return;
		self.getNavigation().stop();
		self.setControl(flyCtrl, flyNav);
		isFlying = true;
	}

	public final void setWalking() {
		flyNav.tempFly = false;
		self.setNoGravity(false);
		self.setFlag(YoukaiFlags.FLYING, false);
		if (!isFlying) return;
		self.getNavigation().stop();
		self.setYya(0);
		self.setXxa(0);
		self.setZza(0);
		self.setControl(walkCtrl, walkNav);
		isFlying = false;
		leaveGroundTick = 0;
	}

	public final boolean isFlying() {
		return isFlying;
	}

	public void stopMoving() {
		walkCtrl.stop();
		flyCtrl.stop();
	}

	public void tickMove() {
		if (!isFlying) {
			if (self.onGround()) leaveGroundTick = 0;
			else leaveGroundTick++;
		}
		if (!self.onGround() && self.getDeltaMovement().y < 0.0D) {
			tickFalling();
		}
		LivingEntity target = self.getTarget();
		if (target != null && self.canAttack(target) && isFlying) {
			combat.tickCombatFlying(target);
		}
		if (target == null && !self.getNavigation().isDone()) {
			debugger.debugPath();
		}
	}

	private void tickFalling() {
		if (isFlying && flyNav.tempFly) return;
		if (!isFlying && !walkNav.isDone() && !walkNav.isStuck()) return;
		double fall = self.isAggressive() ? 0.6 : 0.8;
		self.setDeltaMovement(self.getDeltaMovement().multiply(1.0D, fall, 1.0D));
	}

	public void markHuman() {
		walkNav.setCanPassDoors(true);
		walkNav.setCanOpenDoors(true);
		walkNav.setCanFloat(true);

		flyNav.setCanFloat(true);
	}

	public boolean moveTo(CompoundPath path, float speedModifier) {
		if (path.flying()) {
			if (!self.mayFly()) return false;
			if (!isFlying()) {
				setFlying();
				flyNav.tempFly = true;
			}
			flyNav.moveTo(path.path(), speedModifier);
			return true;
		} else {
			if (!isFlying()) {
				walkNav.moveTo(path.path(), speedModifier);
				return true;
			} else {
				return false;
			}
		}
	}

	@Nullable
	public CompoundPath getPath() {
		var path = self.getNavigation().getPath();
		if (path == null) return null;
		return new CompoundPath(isFlying(), path);
	}

	public class Ground extends GroundPathNavigation {

		public Ground(Mob mob, Level level) {
			super(mob, level);
		}

		@Override
		public boolean moveTo(double x, double y, double z, int accuracy, double speed) {
			boolean ans = super.moveTo(x, y, z, accuracy, speed);
			if (path != null && path.canReach()) {
				if (ans) return true;
				if (!(isDone() || isStuck())) return false;
				if (!self.onGround() && leaveGroundTick < 20) return false;
			}
			if (!self.mayFly()) return false;
			setFlying();
			flyNav.tempFly = true;
			return flyNav.moveTo(x, y, z, accuracy, speed);
		}

		@Override
		public boolean moveTo(Entity entity, double speed) {
			boolean ans = super.moveTo(entity, speed);
			if (path != null && path.canReach()) {
				if (ans) return true;
				if (!(isDone() || isStuck())) return false;
				if (entity.onGround() && !self.onGround() && leaveGroundTick < 20) return false;
			}
			if (!self.mayFly()) return false;
			setFlying();
			flyNav.tempFly = true;
			return flyNav.moveTo(entity, speed);
		}

		@Override
		protected PathFinder createPathFinder(int maxVisitedNodes) {
			this.nodeEvaluator = new YoukaiWalkNodeEvaluator();
			this.nodeEvaluator.setCanPassDoors(true);
			return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
		}

		@Override
		public void tick() {
			super.tick();
		}

	}

	public class Flying extends FlyingPathNavigation {

		private boolean tempFly = false;

		public Flying(Mob mob, Level level) {
			super(mob, level);
		}

		@Override
		public void tick() {
			super.tick();
			if (tempFly) {
				if (isDone() || isStuck()) {
					setWalking();
				}
			}
		}

		@Override
		protected PathFinder createPathFinder(int maxVisitedNodes) {
			this.nodeEvaluator = new YoukaiFlyNodeEvaluator();
			this.nodeEvaluator.setCanPassDoors(false);
			return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
		}
	}

	public static class ClimbMoveControl extends MoveControl {

		public ClimbMoveControl(Mob mob) {
			super(mob);
		}

		public void stop() {
			operation = Operation.WAIT;
		}

		@Override
		public void tick() {
			pressIntoLadder();
			// Vanilla MoveControl parks in JUMPING state until the mob lands, so a mob
			// climbing straight up a ladder shaft would stall after the first jump:
			// it never touches ground to re-trigger. Refresh the jump while the mob
			// is on a ladder and the target is above to sustain the climb.
			// (Climbing down needs no assist: being on a ladder clamps sink speed.)
			if ((operation == Operation.MOVE_TO || operation == Operation.JUMPING) &&
					mob.onClimbable() && wantedY > mob.getY() + 0.1) {
				mob.getJumpControl().jump();
			}
			super.tick();
		}

		/**
		 * While ascending a ladder shaft, steer into the ladder face instead of the
		 * bare node center. Otherwise the mob either jumps in place at the shaft base
		 * (centered under the ladder but never touching it) or drifts off the thin
		 * ladder shape mid-climb and falls. Only applies when the target is above;
		 * side/top exits keep normal steering so the mob can step off.
		 */
		private void pressIntoLadder() {
			if (operation != Operation.MOVE_TO && operation != Operation.JUMPING) return;
			if (wantedY < mob.getY() + 0.5) return;
			BlockPos ladderPos = findLadderForClimb();
			if (ladderPos == null) return;
			BlockState ladder = mob.level().getBlockState(ladderPos);
			double tx = ladderPos.getX() + 0.5;
			double tz = ladderPos.getZ() + 0.5;
			if (ladder.getBlock() instanceof LadderBlock && ladder.hasProperty(LadderBlock.FACING)) {
				Direction wall = ladder.getValue(LadderBlock.FACING).getOpposite();
				tx += wall.getStepX() * 0.3;
				tz += wall.getStepZ() * 0.3;
			}
			setWantedPosition(tx, wantedY, tz, speedModifier);
		}

		@Nullable
		private BlockPos findLadderForClimb() {
			if (mob.onClimbable()) {
				var last = mob.getLastClimbablePos();
				if (last.isPresent() && last.get().distManhattan(mob.blockPosition()) <= 2)
					return last.get();
			}
			// shaft entry from below: standing under the ladder, not yet touching it
			BlockPos feet = mob.blockPosition();
			for (int i = 0; i <= 2; i++) {
				BlockPos pos = feet.above(i);
				if (mob.level().getBlockState(pos).is(BlockTags.CLIMBABLE))
					return pos;
			}
			return null;
		}
	}

	public static class FlyControl extends FlyingMoveControl {

		public FlyControl(Mob mob, int maxTurn, boolean hovers) {
			super(mob, maxTurn, hovers);
		}

		public void stop() {
			operation = Operation.WAIT;
		}
	}

}
