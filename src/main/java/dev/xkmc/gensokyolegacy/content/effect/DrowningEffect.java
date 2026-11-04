package dev.xkmc.gensokyolegacy.content.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Extra drowning damage is applied by {@code EffectCombatHandlers}; this effect only sinks its
 * holder while it is inside any fluid.
 */
public class DrowningEffect extends MobEffect {

	public DrowningEffect() {
		super(MobEffectCategory.HARMFUL, 0xFF1E90FF);
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity.level().isClientSide) return true;
		if (entity.isInFluidType()) {
			Vec3 vec = entity.getDeltaMovement();
			entity.setDeltaMovement(vec.x, vec.y - 1, vec.z);
			entity.hurtMarked = true;
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

}
