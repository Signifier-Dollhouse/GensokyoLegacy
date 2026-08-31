package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MiasmaHandler implements HexBrewHandler {

	@Override
	public boolean isThrowable() {
		return true;
	}

	@Override
	public void onHit(Level level, Vec3 pos, Entity thrower) {
		if (level.isClientSide) return;
		AABB box = new AABB(pos, pos).inflate(4.0);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, en -> true)) {
			if (e == thrower) continue;
			if (thrower != null && e.isAlliedTo(thrower)) continue;
			if (e.distanceToSqr(pos) > 16) continue;
			e.addEffect(new MobEffectInstance(GLEffects.MIASMA.holder(), 200, 0));
		}
		level.levelEvent(2002, BlockPos.containing(pos), 0xFF7A4BA1);
	}
}
