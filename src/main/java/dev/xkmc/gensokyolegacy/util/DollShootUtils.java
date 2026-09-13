package dev.xkmc.gensokyolegacy.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Trimmed copy of MobWeaponAPI's {@code ShootUtils} aim helpers: target-velocity
 * lead plus optional gravity-arc compensation. The arrow/infinity machinery over
 * there is dropped — dolls never use arrows. Callers pass gravity explicitly;
 * hexbrew throws use 0 (straight aim with lead, same drop as before) because the
 * bottle entity declares no gravity override to compensate against.
 */
public final class DollShootUtils {

	private DollShootUtils() {
	}

	/** Aim at a moving target and fire in one step. */
	public static void shootAimHelper(LivingEntity target, Projectile projectile, float velocity, float gravity) {
		getShootVector(target, projectile.position(), velocity, gravity, 0).launch(projectile);
	}

	/**
	 * Laser-style firing solution for projectile shots: estimates flight ticks
	 * from distance ÷ shot speed, predicts the target center with
	 * {@link #predictCenter} (the same helper the laser aims with, including
	 * its airborne-vertical and slime handling), then refines once against the
	 * predicted distance so approaching/fleeing targets are actually led.
	 * Fire at {@code speed} so the estimate and the shot agree.
	 */
	public static Vec3 predictShotDir(LivingEntity target, Vec3 from, double speed) {
		if (speed <= 0) speed = 1;
		Vec3 center = new Vec3(target.getX(), target.getY(0.5), target.getZ());
		Vec3 aim = predictCenter(target, flightTicks(center.distanceTo(from), speed));
		aim = predictCenter(target, flightTicks(aim.distanceTo(from), speed));
		Vec3 dir = aim.subtract(from);
		return dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
	}

	private static int flightTicks(double dist, double speed) {
		return Math.max(1, (int) Math.ceil(dist / speed));
	}

	/**
	 * Predicts a target's body center after the given seconds, extrapolating its
	 * current velocity (vertical motion only counts while airborne, mirroring the
	 * aim helper). Used for instant attacks with a fixed delay, like lasers.
	 */
	public static Vec3 predictCenter(LivingEntity target, int tick) {
		var motion = target.getDeltaMovement();
		double x = target.getX() + motion.x * tick;
		double z = target.getZ() + motion.z * tick;
		double y = target.position().y + target.getBbHeight() / 2 +
				(target.onGround() ? 0 : motion.y * tick);

		if (target instanceof Slime) {
			var clip = target.level().clip(new ClipContext(target.position(), target.position().add(0, -3, 0),
					ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, target));
			if (clip.getType() == HitResult.Type.BLOCK) {
				y += clip.getLocation().y() - target.getY();
			}
		}

		return new Vec3(x, y, z);
	}

	public static AimResult getShootVector(LivingEntity target, Vec3 from, float velocity, float gravity,
										   float inaccuracy) {
		double dx = target.getX() - from.x();
		double dy = target.getY(0.5) - from.y();
		double dz = target.getZ() - from.z();

		double c = dx * dx + dz * dz + dy * dy;
		boolean completed = false;
		if (target instanceof Slime) {
			var clip = target.level().clip(new ClipContext(target.position(), target.position().add(0, -3, 0),
					ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, target));
			if (clip.getType() == HitResult.Type.BLOCK) {
				dy += clip.getLocation().y() - target.getY();
				completed = true;
			}
		}
		if (!completed) {
			double flightTime = Math.sqrt(c) / velocity;
			var motion = target.getDeltaMovement();
			dx += motion.x * flightTime;
			dz += motion.z * flightTime;
			if (!target.onGround())
				dy += motion.y * flightTime;
		}

		c = dx * dx + dz * dz + dy * dy;

		if (gravity > 0 && c > velocity * velocity * 4) {
			double a = gravity * gravity / 4;
			double b = dy * gravity - velocity * velocity;

			double delta = b * b - 4 * a * c;
			if (delta > 0) {
				double t21 = (-b + Math.sqrt(delta)) / (2 * a);
				double t22 = (-b - Math.sqrt(delta)) / (2 * a);
				if (t21 > 0 || t22 > 0) {
					double t2 = t21 > 0 ? t22 > 0 ? Math.min(t21, t22) : t21 : t22;
					return new AimResult(new Vec3(dx, dy + gravity * t2 / 2, dz), velocity, inaccuracy);

				}
			}
		}
		return new AimResult(new Vec3(dx, dy, dz), velocity, inaccuracy);
	}

	public record AimResult(Vec3 direction, float velocity, float inaccuracy) {

		public void launch(Projectile projectile) {
			projectile.shoot(direction.x, direction.y, direction.z, velocity, inaccuracy);
		}

	}

}
