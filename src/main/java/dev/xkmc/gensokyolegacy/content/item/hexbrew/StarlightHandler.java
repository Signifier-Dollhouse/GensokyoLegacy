package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

		}
	}

	public static String getOwnerKey() {
		return OWNER_KEY;
	}
}
