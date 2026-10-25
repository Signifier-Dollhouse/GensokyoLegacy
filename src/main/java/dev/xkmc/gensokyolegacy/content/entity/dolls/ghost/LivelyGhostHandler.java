package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Lively ghost: haste, speed and the cooldown-speed effect while its power lasts.
 */
final class LivelyGhostHandler implements GhostVariantHandler {

	private static final int DECAY_INTERVAL = 20;
	private static final int AURA_INTERVAL = 20;
	private static final int AURA_DURATION = 60;

	@Override
	public Component getDisplayName() {
		return GLLang.Ghost.LIVELY.get();
	}

	@Override
	public void tick(GhostEntity ghost) {
		if (ghost.tickCount % DECAY_INTERVAL == 0) ghost.addPower(-1);
	}

	@Override
	public void aura(GhostEntity ghost, LivingEntity owner) {
		if (ghost.getPower() <= 0) return;
		if (ghost.tickCount % AURA_INTERVAL != 0) return;
		owner.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, AURA_DURATION, 1, true, true, true));
		owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, AURA_DURATION, 1, true, true, true));
		owner.addEffect(new MobEffectInstance(GLEffects.LIVELY, AURA_DURATION, 0, true, true, true));
	}

}
