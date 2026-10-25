package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollRig;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Curios ghost: the slowest upkeep and a periodic cleanse of the owner's longest debuff.
 */
final class CuriosGhostHandler implements GhostVariantHandler {

	private static final int DECAY_INTERVAL = 40;
	private static final int SKILL_INTERVAL = 60;
	private static final int SKILL_COST = 50;

	@Override
	public Component getDisplayName() {
		return GLLang.Ghost.CURIOS.get();
	}

	@Override
	public void tick(GhostEntity ghost) {
		if (ghost.tickCount % DECAY_INTERVAL == 0) ghost.addPower(-1);
	}

	@Override
	public void aura(GhostEntity ghost, LivingEntity owner) {
		if (ghost.getPower() < SKILL_COST) return;
		if (ghost.tickCount % SKILL_INTERVAL != 0) return;
		if (cleanseLongest(ghost, owner)) ghost.addPower(-SKILL_COST);
	}

	/**
	 * One harmful effect per pulse, the one with the most time left, so a long debuff is worked
	 * down before a short one that would have expired on its own anyway.
	 */
	private static boolean cleanseLongest(GhostEntity ghost, LivingEntity owner) {
		MobEffectInstance target = null;
		for (MobEffectInstance effect : owner.getActiveEffects()) {
			if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) continue;
			if (target == null || remaining(effect) > remaining(target)) target = effect;
		}
		if (target == null) return false;
		owner.removeEffect(target.getEffect());
		ghost.level().broadcastEntityEvent(ghost, DollRig.EVENT_SKILL);
		return true;
	}

	private static int remaining(MobEffectInstance effect) {
		return effect.getDuration() < 0 ? Integer.MAX_VALUE : effect.getDuration();
	}

}
