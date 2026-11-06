package dev.xkmc.gensokyolegacy.content.effect;

import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SparklingEffect extends DanmakuRetaliateEffect {

	private static final int COOLDOWN = 10;

	public SparklingEffect() {
		super(MobEffectCategory.HARMFUL, 0xFFFFF7AE, "gensokyolegacy:starlight", COOLDOWN);
	}

	@Override
	protected void spawnDanmaku(ServerLevel level, LivingEntity owner, LivingEntity victim) {
		spawnStars(level, owner, victim.position().add(0, victim.getBbHeight() * 0.5, 0), victim.getBbWidth());
	}

	public static void spawnStars(Level level, LivingEntity owner, Vec3 center, float dist) {
		double offset = owner.getRandom().nextDouble();
		for (int i = 0; i < 16; i++) {
			double angle = (i + offset) * Math.PI * 2 / 16;
			Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle)).normalize().scale(1.0);
			Vec3 spawn = center.add(dir.scale(dist));
			int life = 15 + level.getRandom().nextInt(11);
			ItemBulletEntity bullet = new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), spawn.x, spawn.y, spawn.z, level);
			bullet.setOwner(owner);
			bullet.setItem(GLItems.STAR.asStack());
			bullet.setup(4.0f, life, false, false, dir);
			level.addFreshEntity(bullet);
		}
	}

}
