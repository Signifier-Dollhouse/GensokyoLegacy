package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.UUID;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class SparklingEventHandler {

	private static final String OWNER_KEY = StarlightHandler.getOwnerKey();
	private static final String COOLDOWN_KEY = "gensokyolegacy:starlight_cooldown";

	@SubscribeEvent
	public static void onLivingHurt(LivingIncomingDamageEvent event) {
		LivingEntity victim = event.getEntity();
		if (!victim.hasEffect(GLEffects.SPARKLING.holder())) return;
		if (!(victim.level() instanceof ServerLevel level)) return;
		if (!victim.getPersistentData().contains(COOLDOWN_KEY)) {
			victim.getPersistentData().putLong(COOLDOWN_KEY, 0);
		}
		long last = victim.getPersistentData().getLong(COOLDOWN_KEY);
		long now = level.getGameTime();
		if (now - last < 10) return;
		if (!victim.getPersistentData().hasUUID(OWNER_KEY)) return;
		UUID ownerId = victim.getPersistentData().getUUID(OWNER_KEY);
		Entity owner = level.getEntity(ownerId);
		if (!(owner instanceof LivingEntity livingOwner)) return;
		victim.getPersistentData().putLong(COOLDOWN_KEY, now);
		Vec3 center = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
		spawnStars(level, livingOwner, center, victim.getBbWidth());

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
			bullet.setItem(DanmakuItems.Bullet.STAR.get(DyeColor.YELLOW).asStack());
			bullet.setup(4.0f, life, false, false, dir);
			level.addFreshEntity(bullet);
		}
	}

}
