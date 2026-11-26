package dev.xkmc.gensokyolegacy.content.entity.dolls.goals;

import dev.xkmc.gensokyolegacy.content.entity.dolls.StrayDollEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Keeps a group member close to its leader. A member whose leader is gone simply stops following
 * and falls back to wandering and hunting on its own.
 *
 * @see "doc/design/doll/stray.md"
 */
public class FollowStrayLeaderGoal extends Goal {

	private static final double STOP_DISTANCE_SQ = 9;
	private static final int REPATH_INTERVAL = 10;
	private static final double SPEED = 1;

	private final StrayDollEntity doll;
	@Nullable
	private StrayDollEntity leader;
	private int repath;

	public FollowStrayLeaderGoal(StrayDollEntity doll) {
		this.doll = doll;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		leader = doll.getLeader();
		return leader != null && doll.distanceToSqr(leader) > STOP_DISTANCE_SQ;
	}

	@Override
	public void start() {
		repath = 0;
	}

	@Override
	public boolean canContinueToUse() {
		return leader != null && leader.isAlive() && doll.distanceToSqr(leader) > STOP_DISTANCE_SQ;
	}

	@Override
	public void tick() {
		if (leader == null) return;
		if (--repath <= 0) {
			repath = adjustedTickDelay(REPATH_INTERVAL);
			doll.getNavigation().moveTo(leader, SPEED);
		}
	}

	@Override
	public void stop() {
		doll.getNavigation().stop();
		leader = null;
	}

}
