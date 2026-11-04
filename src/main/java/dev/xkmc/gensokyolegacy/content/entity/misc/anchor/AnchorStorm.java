package dev.xkmc.gensokyolegacy.content.entity.misc.anchor;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.spell.mover.RectMover;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.fastprojectileapi.collision.EntityStorageHelper;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Trail, burst and area damage shared by the unbound storm anchors.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public final class AnchorStorm {

	private static final int MAX_TRAIL = 160;
	private static final double BURST_SPEED = 0.7;
	private static final int TRAIL_LIFE = 40;
	private static final double AREA_RADIUS = 6;
	private static final float AREA_DAMAGE = 12;
	private static final Vec3 GRAVITY = new Vec3(0, -0.05, 0);

	private AnchorStorm() {
	}

	/**
	 * Sheds one falling danmaku with no initial speed per tick; returns the new trail budget.
	 */
	public static int shedTrail(Entity self, Entity owner, int spent) {
		if (spent >= MAX_TRAIL || !stormDanmaku()) return spent;
		spawn(self, owner, Vec3.ZERO, TRAIL_LIFE + self.getRandom().nextInt(11));
		return spent + 1;
	}

	/**
	 * Area damage on impact, plus the gravitational burst.
	 */
	public static void impact(Entity self, Entity owner) {
		areaDamage(self, owner);
		if (stormDanmaku()) burst(self, owner);
	}

	private static void areaDamage(Entity self, Entity owner) {
		areaDamage(self, owner, self.position(), AREA_RADIUS, AREA_DAMAGE);
	}

	/**
	 * Hurts every living entity within {@code radius} of {@code at}, gated like every danmaku hit.
	 */
	public static void areaDamage(Entity self, Entity owner, Vec3 at, double radius, float damage) {
		if (owner == null) return;
		hurtArea(self.level(), owner, self.damageSources().thrown(self, owner), at, radius, damage);
	}

	/**
	 * The anchor's cancelled melee: the swing hits everything around the clicked target for the
	 * wielder's attack damage, and spends the attack cooldown as a vanilla hit would.
	 */
	public static void melee(Player player, Vec3 at, double radius) {
		float scale = player.getAttackStrengthScale(0.5F);
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
				* (0.2F + scale * scale * 0.8F);
		player.resetAttackStrengthTicker();
		hurtArea(player.level(), player, player.damageSources().playerAttack(player), at, radius, damage);
	}

	private static void hurtArea(Level level, Entity owner, DamageSource source, Vec3 at,
			double radius, float damage) {
		AABB box = new AABB(at.subtract(radius, radius, radius), at.add(radius, radius, radius));
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
			if (target == owner) continue;
			if (!IDanmakuEntity.canHurt(owner, target)) continue;
			if (target.position().distanceToSqr(at) > radius * radius) continue;
			target.hurt(source, damage);
		}
	}

	private static void burst(Entity self, Entity owner) {
		Vec3 forward = self.getDeltaMovement().normalize();
		if (forward.lengthSqr() < 1e-6) forward = new Vec3(0, 1, 0);
		for (int i = 0; i < 300; i++) {
			Vec3 dir = new Vec3(self.getRandom().nextGaussian(), self.getRandom().nextGaussian(),
					self.getRandom().nextGaussian()).add(forward.scale(1.2));
			if (dir.lengthSqr() < 1e-6) dir = forward;
			spawn(self, owner, dir.normalize().scale(BURST_SPEED), TRAIL_LIFE + self.getRandom().nextInt(11));
		}
	}

	private static void spawn(Entity self, Entity owner, Vec3 vec, int life) {
		if (!(self.level() instanceof ServerLevel sl)) return;
		ItemStack stack = new ItemStack(DanmakuItems.Bullet.CIRCLE.get(DyeColor.BLUE).get());
		ItemBulletEntity danmaku = new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), 0, 0, 0, sl);
		if (owner != null) danmaku.setOwner(owner);
		danmaku.setItem(stack);
		danmaku.setup(1, life, false, false, vec);
		danmaku.setPos(self.position());
		danmaku.mover = new RectMover(self.position(), vec, GRAVITY);
		EntityStorageHelper.fastAdd(sl, danmaku);
	}

	private static boolean stormDanmaku() {
		return GLModConfig.SERVER.anchorStormDanmaku.get();
	}

}
