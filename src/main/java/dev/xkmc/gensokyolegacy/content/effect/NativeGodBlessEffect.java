package dev.xkmc.gensokyolegacy.content.effect;

import dev.xkmc.l2library.util.math.MathHelper;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

public class NativeGodBlessEffect extends EmptyEffect {

	public NativeGodBlessEffect(MobEffectCategory category, int color) {
		super(category, color);
		var uuid = MathHelper.getUUIDFromString("gensokyolegacy:native_god_bless").toString();
		addAttributeModifier(ForgeMod.ENTITY_REACH.get(), uuid, 1f,
				AttributeModifier.Operation.ADDITION);
		addAttributeModifier(ForgeMod.BLOCK_REACH.get(), uuid, 1f,
				AttributeModifier.Operation.ADDITION);
		addAttributeModifier(Attributes.MOVEMENT_SPEED, uuid, 0.2f,
				AttributeModifier.Operation.MULTIPLY_BASE);
	}

}
