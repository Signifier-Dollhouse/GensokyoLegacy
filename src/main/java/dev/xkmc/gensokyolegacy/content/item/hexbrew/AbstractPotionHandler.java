package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

public interface AbstractPotionHandler extends HexBrewHandler {

	default void applyDrinkEffects(LivingEntity user, ItemStack stack) {
		PotionContents contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
		if (contents == null) return;
		for (MobEffectInstance e : contents.getAllEffects()) {
			user.addEffect(new MobEffectInstance(e));
		}
	}

	@Override
	default void onDrink(LivingEntity user, ItemStack stack, Level level) {
		if (level.isClientSide) return;
		applyDrinkEffects(user, stack);
	}

}
