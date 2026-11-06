package dev.xkmc.gensokyolegacy.content.entity.dolls;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared movement for the wild strays: a flying pathfinder that holds one block of clearance
 * over the floor, like the plain fairy.
 *
 * @see "doc/design/doll/stray.md"
 */
public abstract class StrayFlyingMob extends PathfinderMob {

	private static final double HOVER_CLEARANCE = 1.0;
	private static final double HOVER_SCAN = 4.0;
	private static final double HOVER_STIFFNESS = 0.2;
	private static final double HOVER_MAX_RISE = 0.25;
	/**
	 * Airborne drift. Vanilla's 0.02 is a bat's glide and would make the wander crawl.
	 */
	private static final float HOVER_DRIFT = 0.06f;

	protected StrayFlyingMob(EntityType<? extends StrayFlyingMob> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		this.setNoGravity(true);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new FlyingPathNavigation(this, level);
	}

	/**
	 * Hovering is a movement problem, not a goal one: this runs after the move so the vertical
	 * velocity can be replaced before the next tick's travel.
	 */
	@Override
	public void travel(Vec3 input) {
		super.travel(input);
		hoverStep();
	}

	@Override
	protected float getFlyingSpeed() {
		return HOVER_DRIFT;
	}

	private void hoverStep() {
		var hit = level().clip(new ClipContext(position(), position().add(0, -HOVER_SCAN, 0),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		double floor = hit.getType() != HitResult.Type.BLOCK ? getY() - HOVER_SCAN : hit.getLocation().y;
		Vec3 v = getDeltaMovement();
		double diff = floor + HOVER_CLEARANCE - getY();
		if (!getNavigation().isDone() && diff < 0) return;
		if (Math.abs(diff) < 0.02) {
			setDeltaMovement(v.x, 0, v.z);
			return;
		}
		setDeltaMovement(v.x, Mth.clamp(diff * HOVER_STIFFNESS, -HOVER_MAX_RISE, HOVER_MAX_RISE), v.z);
	}

}
