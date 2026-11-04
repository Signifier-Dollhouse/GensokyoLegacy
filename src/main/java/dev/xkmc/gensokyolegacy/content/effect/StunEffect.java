package dev.xkmc.gensokyolegacy.content.effect;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Mirrors Cataclysm's stun: slower movement here, with attacking, jumping and item use blocked
 * by {@code StunEventHandlers}.
 */
public class StunEffect extends MobEffect {

	public StunEffect() {
		super(MobEffectCategory.HARMFUL, 0xFF8C00);
		addAttributeModifier(Attributes.MOVEMENT_SPEED, GensokyoLegacy.loc("stun_speed"),
				-0.5, AttributeModifier.Operation.ADD_VALUE);
	}

}
