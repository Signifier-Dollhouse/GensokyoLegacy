package dev.xkmc.gensokyolegacy.content.effect;

import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.spell.mover.PolarMover;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.fastprojectileapi.collision.EntityStorageHelper;
import dev.xkmc.gensokyolegacy.content.entity.misc.ButterflyDanmakuBulletEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Answers a hit with the haunting ghost's butterfly flower: one circle per tilted plane, three
 * butterflies per circle, orbiting the victim in one round trip.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public class HauntingEffect extends DanmakuRetaliateEffect {

	private static final int COOLDOWN = 25;
	private static final int DURATION = 3600;
	private static final int ROUNDTRIP = 20;
	private static final int PER_CIRCLE = 3;
	private static final double RADIUS = 1;
	private static final double TILT = 70;

	public HauntingEffect() {
		super(MobEffectCategory.HARMFUL, 0xFFAA55FF, "gensokyolegacy:haunting", COOLDOWN);
	}

	public static void inflict(LivingEntity target, LivingEntity owner) {
		apply(GLEffects.HAUNTING, target, owner.getUUID(), DURATION);
	}

	@Override
	protected void spawnDanmaku(ServerLevel level, LivingEntity owner, LivingEntity victim) {
		Vec3 origin = victim.position().add(0, victim.getBbHeight() / 2, 0);
		Vec3 aim = origin.subtract(owner.position().add(0, owner.getBbHeight() / 2, 0));
		Vec3 dir = aim.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : aim.normalize();
		var ori = DanmakuHelper.getOrientation(dir);
		var pl0 = DanmakuHelper.getOrientation(dir, ori.rotateDegrees(90, TILT));
		var pl1 = DanmakuHelper.getOrientation(dir, ori.rotateDegrees(-90, TILT));
		spawnCircle(level, owner, origin, pl0, 1);
		spawnCircle(level, owner, origin, pl1, -1);
	}

	/**
	 * One circle of butterflies orbiting the point they spawn at. Every butterfly starts and ends
	 * at that point because its orbit radius matches its ring radius, so the circle is a round trip.
	 */
	private static void spawnCircle(ServerLevel level, LivingEntity owner, Vec3 origin,
									DanmakuHelper.Orientation plane, int direction) {
		double w = Math.PI * 2 / ROUNDTRIP;
		Vec3 normal = plane.normal();
		ItemStack item = (direction > 0 ? GLItems.PINK_BUTTERFLY : GLItems.BLUE_BUTTERFLY).asStack();
		for (int i = 0; i < PER_CIRCLE; i++) {
			Vec3 out = plane.rotateDegrees(i * 360.0 / PER_CIRCLE);
			Vec3 center = origin.add(out.scale(RADIUS));
			Vec3 back = out.scale(-1);
			Vec3 tangent = back.cross(normal).normalize().scale(w * RADIUS * direction);
			ItemBulletEntity danmaku = new ButterflyDanmakuBulletEntity(GLEntities.BUTTERFLY_DANMAKU.get(), owner, level);
			danmaku.setItem(item);
			danmaku.setup(DanmakuItems.Bullet.BUTTERFLY.damage(), ROUNDTRIP, true, true, tangent);
			danmaku.setPos(origin);
			danmaku.mover = PolarMover.ofPlane(center, back, normal)
					.radial(RADIUS, 0, 0)
					.angular(0, w * direction, 0);
			EntityStorageHelper.fastAdd(level, danmaku);
		}
	}

}
