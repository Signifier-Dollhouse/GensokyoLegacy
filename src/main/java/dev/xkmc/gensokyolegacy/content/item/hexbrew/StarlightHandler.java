package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import dev.xkmc.gensokyolegacy.content.effect.DanmakuRetaliateEffect;
import dev.xkmc.gensokyolegacy.content.effect.SparklingEffect;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class StarlightHandler implements HexBrewHandler {

	private static final int DURATION = 3600;

	@Override
	public boolean isThrowable() {
		return true;
	}

	@Override
	public void onHit(Level level, Vec3 pos, @Nullable Entity thrower, ItemStack stack) {
		if (level.isClientSide) return;
		level.levelEvent(2002, BlockPos.containing(pos), 0xFFFFF7AE);
		if (thrower == null) return;
		Entity credit = HexBrewHandler.damageOwner(thrower);
		AABB box = new AABB(pos, pos).inflate(4.0);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, en -> true)) {
			if (e == thrower || e == credit) continue;
			if (HexBrewHandler.isAllied(e, thrower) || HexBrewHandler.isAllied(e, credit)) continue;
			if (e.distanceToSqr(pos) > 16) continue;
			DanmakuRetaliateEffect.apply(GLEffects.SPARKLING, e, thrower.getUUID(), DURATION);
			if (credit != null) {
				e.hurt(level.damageSources().indirectMagic(thrower, credit), 4.0f);
			} else {
				e.hurt(level.damageSources().magic(), 4.0f);
			}
		}
		if (!(thrower instanceof LivingEntity livingOwner)) return;
		SparklingEffect.spawnStars(level, livingOwner, pos, 0.5f);
	}

}
