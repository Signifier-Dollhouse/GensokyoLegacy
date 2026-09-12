package dev.xkmc.gensokyolegacy.content.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class HyphaeInfectionEffect extends EmptyEffect {

	public HyphaeInfectionEffect() {
		super(MobEffectCategory.HARMFUL, 0xFF47C0FC);
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		entity.hurt(entity.damageSources().magic(), amplifier + 1);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % 20 == 0;
	}

}