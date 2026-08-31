package dev.xkmc.gensokyolegacy.content.effect;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2damagetracker.init.L2DamageTracker;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class MiasmaEffect extends EmptyEffect {

	public MiasmaEffect() {
		super(MobEffectCategory.HARMFUL, 0xFF7A4BA1);
		addAttributeModifier(Attributes.ARMOR, GensokyoLegacy.loc("miasma_armor"),
				-0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addAttributeModifier(L2DamageTracker.REDUCTION.holder(), GensokyoLegacy.loc("miasma_damage"),
				0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

}
