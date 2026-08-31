package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class StarlightHandler implements HexBrewHandler {

	private static final String OWNER_KEY = "gensokyolegacy:starlight_owner";

	@Override
	public boolean isThrowable() {
		return true;
	}

	@Override
	public void onHit(Level level, Vec3 pos, @Nullable Entity thrower) {
		if (level.isClientSide) return;
		level.levelEvent(2002, BlockPos.containing(pos), 0xFFFFF7AE);
		if (thrower == null) return;
		AABB box = new AABB(pos, pos).inflate(4.0);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, en -> true)) {
			if (e == thrower) continue;
			if (e.isAlliedTo(thrower)) continue;
			if (e.distanceToSqr(pos) > 16) continue;
			e.addEffect(new MobEffectInstance(GLEffects.SPARKLING.holder(), 1200, 0));
			e.getPersistentData().putUUID(OWNER_KEY, thrower.getUUID());
			if (thrower instanceof LivingEntity le) {
				e.hurt(level.damageSources().indirectMagic(thrower, le), 4.0f);
			} else {
				e.hurt(level.damageSources().magic(), 4.0f);
			}
		}
		if (!(thrower instanceof LivingEntity livingOwner)) return;
		for (int i = 0; i < 16; i++) {
			double angle = i * Math.PI * 2 / 16;
			Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle)).normalize().scale(1.0);
			Vec3 spawn = pos.add(dir.scale(0.5)).add(0, 0.5, 0);
			int life = 15 + level.getRandom().nextInt(11);
			ItemBulletEntity bullet = new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), spawn.x, spawn.y, spawn.z, level);
			bullet.setOwner(livingOwner);
			bullet.setItem(DanmakuItems.Bullet.STAR.get(DyeColor.YELLOW).asStack());
			bullet.setup(4.0f, life, false, false, dir);
			level.addFreshEntity(bullet);
		}
	}

	public static String getOwnerKey() {
		return OWNER_KEY;
	}
}
