package dev.xkmc.gensokyolegacy.content.item.gift;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tengu sake — a strong drink that can be given to characters (GiftType.DRINK)
 * or drunk by the player for a temporary boost.
 */
public class TenguSakeItem extends DrinkGiftItem {

	public TenguSakeItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		if (!level.isClientSide) {
			user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
			user.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
		}
		return super.finishUsingItem(stack, level, user);
	}

}
