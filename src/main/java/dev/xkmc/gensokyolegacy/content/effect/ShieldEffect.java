package dev.xkmc.gensokyolegacy.content.effect;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2damagetracker.init.L2DamageTracker;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ShieldEffect extends EmptyEffect {

	public ShieldEffect() {
		super(MobEffectCategory.BENEFICIAL, 0xFFFFF7AE);
		addAttributeModifier(L2DamageTracker.ABSORB.holder(), GensokyoLegacy.loc("shield"),
				1, AttributeModifier.Operation.ADD_VALUE);
	}

}
