package dev.xkmc.gensokyolegacy.content.entity.dolls.action;

import dev.xkmc.danmakuapi.api.DanmakuBullet;
import dev.xkmc.danmakuapi.api.DanmakuLaser;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.entity.ItemLaserEntity;
import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * {@link CardHolder} for a doll action: mirrors {@code YoukaiCardHolder} but aims at
 * the commanded target position captured by the goal. Shot damage is the bullet's
 * base damage — dolls carry no attack-damage attribute.
 */
public record DollCardHolder(DollEntity doll, Vec3 targetPos) implements CardHolder {

	private Level level() {
		return doll.level();
	}

	@Override
	public Vec3 center() {
		return doll.position().add(0, doll.getBbHeight() / 2, 0);
	}

	@Override
	public Vec3 forward() {
		Vec3 dir = targetPos.subtract(center());
		return dir.lengthSqr() < 1e-6 ? doll.getForward() : dir.normalize();
	}

	@Override
	public Vec3 target() {
		return targetPos;
	}

	@Override
	public @Nullable Vec3 targetVelocity() {
		return null;
	}

	@Override
	public RandomSource random() {
		return doll.getRandom();
	}

	@Override
	public ItemBulletEntity prepareDanmaku(int life, Vec3 vec, DanmakuBullet type, DyeColor color) {
		ItemBulletEntity danmaku = new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), doll, level());
		danmaku.setPos(center());
		danmaku.setItem(type.get(color).asStack());
		danmaku.setup(type.damage(), life, true, true, vec);
		return danmaku;
	}

	@Override
	public ItemLaserEntity prepareLaser(int life, Vec3 pos, Vec3 vec, float len, DanmakuLaser type, DyeColor color) {
		ItemLaserEntity danmaku = new ItemLaserEntity(DanmakuEntities.ITEM_LASER.get(), doll, level());
		danmaku.setItem(type.get(color).asStack());
		danmaku.setup(type.damage(), life, len, true, vec);
		danmaku.setPos(pos);
		return danmaku;
	}

	@Override
	public void shoot(SimplifiedProjectile danmaku) {
		level().addFreshEntity(danmaku);
	}

	@Override
	public LivingEntity self() {
		return doll;
	}

}
