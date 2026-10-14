package dev.xkmc.gensokyolegacy.content.entity.characters.magician;

import dev.xkmc.gensokyolegacy.content.entity.behavior.task.combat.YoukaiAttackTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Alice fights at range, so this replaces the stock {@code AttackTask} + {@code StrafeTarget}
 * pair: both want to stand on the target, while her dolls do the shooting and she conducts.
 * <p>
 * The point is a <b>dead band</b> of {@link #MIN}–{@link #MAX} blocks around {@link #HOLD} — the
 * stock strafe circles inside its radius and charges outside it, so hovering at the threshold
 * flip-flops forever, while here each edge only changes which way she drifts.
 * <p>
 * The orbit is 3D, its horizontal radius solved from the remaining 3D budget, so a target on a
 * tower is answered by rising to it. Movement goes through {@code getNavigation()} because
 * {@code FlyingMoveControl} ignores the strafe op and drops {@code noGravity} off a point, and
 * {@code WALK_TARGET = ABSENT} keeps the always-on {@code YoukaiMoveTask} off the navigation.
 */
public class AliceRangeTask extends YoukaiAttackTask<AliceEntity> {

	/** Inside this she opens the range. */
	private static final double MIN = 16;

	/** Beyond this she closes. */
	private static final double MAX = 24;

	/** Where she settles, and what both corrections aim at. */
	private static final double HOLD = 20;

	/**
	 * Radians of orbit per tick, giving a ten-second lap at {@link #HOLD}; navigation clamps her
	 * to her own flight speed and she cuts the chord, which is how a circling youkai looks.
	 */
	private static final double ORBIT_STEP = 0.011;

	/** Ticks of continuous line of sight before she gives up and charges a lost target. */
	private static final int LOSE_SIGHT_GRACE = 20;

	public AliceRangeTask() {
		super(Map.of(
				MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT,
				MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT
		), 16);
	}

	private double orbit;
	private int blind;

	@Override
	public void tick(ServerLevel level, AliceEntity alice, long gameTime) {
		LivingEntity target = alice.getTarget();
		if (target == null) {
			alice.getNavigation().stop();
			blind = 0;
			return;
		}
		alice.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
		blind = target.hasLineOfSight(alice) ? 0 : blind + 1;
		if (blind >= LOSE_SIGHT_GRACE) {
			close(alice, target);
			return;
		}
		double distance = alice.distanceTo(target);
		if (distance > MAX) {
			close(alice, target);
		} else if (distance < MIN) {
			withdraw(alice, target);
		} else {
			circle(alice, target);
		}
	}

	/** Inside the band: orbit at exactly {@link #HOLD}, rising or falling to suit. */
	private void circle(AliceEntity alice, LivingEntity target) {
		orbit += ORBIT_STEP;
		Vec3 dest = orbitPoint(alice, target);
		alice.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0F);
	}

	/**
	 * A point on the sphere of radius {@link #HOLD} around the target, in the direction she is
	 * circling, with the vertical part of the budget already spent. Solving the horizontal radius
	 * out of what is left is what makes the orbit 3D: the closer she is to the target's height,
	 * the wider the circle, so the 3D distance stays put while the circle answers altitude.
	 */
	private Vec3 orbitPoint(AliceEntity alice, LivingEntity target) {
		double dy = target.getEyeY() - alice.getEyeY();
		// never spend the whole budget on height: leave a floor so the circle
		// cannot collapse onto a point and stop being a circle
		double budget = Math.max(MIN / 2, HOLD * HOLD - dy * dy);
		double radius = Math.sqrt(budget);
		return new Vec3(target.getX() + Math.cos(orbit) * radius,
				alice.getEyeY(),
				target.getZ() + Math.sin(orbit) * radius);
	}

	/** Too far: step to {@link #HOLD} blocks out along the full 3D vector. */
	private void close(AliceEntity alice, LivingEntity target) {
		stepTo(alice, target, -1);
	}

	/** Too close: step to {@link #HOLD} blocks out along the same 3D vector. */
	private void withdraw(AliceEntity alice, LivingEntity target) {
		stepTo(alice, target, 1);
	}

	/**
	 * Aims at {@link #HOLD} blocks from the target on the {@code side} side of her ({@code -1}
	 * closes, {@code +1} backs off) along the full 3D vector, so backing off something overhead
	 * gains altitude as well as ground.
	 */
	private void stepTo(AliceEntity alice, LivingEntity target, int side) {
		Vec3 away = alice.position().subtract(target.position());
		if (away.lengthSqr() < 1.0e-6) return;
		Vec3 dest = target.position().add(away.normalize().scale(HOLD * side));
		alice.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0F);
	}

	@Override
	public void start(ServerLevel level, AliceEntity youkai, long gameTime) {
		super.start(level, youkai, gameTime);
		blind = 0;
		// start the lap from wherever she already is, so the first circle does not
		// send her across the target to the far side
		LivingEntity target = youkai.getTarget();
		if (target != null) orbit = Math.atan2(youkai.getZ() - target.getZ(), youkai.getX() - target.getX());
	}

	@Override
	public void stop(ServerLevel level, AliceEntity youkai, long gameTime) {
		youkai.getNavigation().stop();
		super.stop(level, youkai, gameTime);
	}

}
