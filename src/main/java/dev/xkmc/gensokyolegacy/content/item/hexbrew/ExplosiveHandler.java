package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import dev.xkmc.l2library.content.explosion.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ExplosiveHandler implements HexBrewHandler {

	@Override
	public boolean isThrowable() {
		return true;
	}

	@Override
	public void onHit(Level level, Vec3 pos, @Nullable Entity thrower, ItemStack stack) {
		if (level.isClientSide) return;
		BaseExplosionContext base = new BaseExplosionContext(level, pos.x, pos.y, pos.z, 4.0f);
		VanillaExplosionContext vanilla = new VanillaExplosionContext(
				thrower,
				thrower instanceof LivingEntity le ? level.damageSources().explosion(null, le) : null,
				null, false, Explosion.BlockInteraction.KEEP);
		ModExplosionContext mod = entity -> {
			if (entity == thrower) return false;
			if (thrower != null && entity.isAlliedTo(thrower)) return false;
			return entity instanceof LivingEntity;
		};
		ExplosionHandler.explode(new BaseExplosion(base, vanilla, mod, ParticleExplosionContext.of(4.0f)));
	}
}
