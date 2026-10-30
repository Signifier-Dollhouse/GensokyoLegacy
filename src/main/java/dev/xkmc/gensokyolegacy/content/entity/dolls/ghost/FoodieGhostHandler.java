package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Foodie ghost: keeps the owner fed and eating fast while its power lasts.
 */
final class FoodieGhostHandler implements GhostVariantHandler {

	private static final int AURA_INTERVAL = 20;
	private static final int AURA_DURATION = 60;

	@Override
	public Component getDisplayName() {
		return GLLang.Ghost.FOODIE.get();
	}

	@Override
	public void tick(GhostEntity ghost) {
		if (ghost.tickCount % GLModConfig.SERVER.foodieDecayInterval.get() == 0) ghost.addPower(-1);
	}

	@Override
	public void aura(GhostEntity ghost, LivingEntity owner) {
		if (ghost.getPower() <= 0) return;
		if (ghost.tickCount % AURA_INTERVAL != 0) return;
		owner.addEffect(new MobEffectInstance(GLEffects.FOODIE, AURA_DURATION, 0, true, true, true));
	}

}
