package dev.xkmc.gensokyolegacy.content.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class HyphaeInfectionEffect extends EmptyEffect {

	public HyphaeInfectionEffect() {
		super(MobEffectCategory.HARMFUL, 0xFF47C0FC);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		entity.hurt(entity.damageSources().magic(), amplifier + 1);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return duration % 20 == 0;
	}

}