package dev.xkmc.gensokyolegacy.content.entity.dolls.goals;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A wander goal for a hovering mob. {@link RandomStrollGoal}'s default ground target is rejected
 * by {@code FlyingPathNavigation.isStableDestination}, which requires a standable block, so every
 * roll comes back null and the mob never moves.
 */
public class HoverStrollGoal extends RandomStrollGoal {

	public HoverStrollGoal(PathfinderMob mob, double speedModifier) {
		super(mob, speedModifier, DEFAULT_INTERVAL, false);
	}

	@Nullable
	@Override
	protected Vec3 getPosition() {
		Vec3 view = this.mob.getViewVector(0.0F);
		return AirAndWaterRandomPos.getPos(this.mob, 10, 4, 0, view.x, view.z, Math.PI / 2);
	}

}
