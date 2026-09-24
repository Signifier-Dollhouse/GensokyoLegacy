package dev.xkmc.gensokyolegacy.content.effect;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.l2damagetracker.init.L2DamageTracker;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class MiasmaEffect extends EmptyEffect {

	public MiasmaEffect() {
		super(MobEffectCategory.HARMFUL, 0xFF7A4BA1);
		addAttributeModifier(Attributes.ARMOR, GensokyoLegacy.loc("miasma_armor"),
				-0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addAttributeModifier(L2DamageTracker.REDUCTION.holder(), GensokyoLegacy.loc("miasma_damage"),
				0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addAttributeModifier(L2DamageTracker.REGEN, GensokyoLegacy.loc("miasma_regen"),
				-0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	public static void onHurt(LivingEntity target) {
		var ins = target.getEffect(GLEffects.MIASMA);
		if (ins == null) return;
		if (target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || target.hasEffect(MobEffects.POISON) || target.hasEffect(MobEffects.WEAKNESS))
			return;
		int amp = ins.getAmplifier();
		double rate = target.getRandom().nextDouble();
		if (rate < 0.4f) {
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, amp));
		} else if (rate < 0.8f) {
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, amp));
		} else if (amp > 0) {
			target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
		}
	}

}
