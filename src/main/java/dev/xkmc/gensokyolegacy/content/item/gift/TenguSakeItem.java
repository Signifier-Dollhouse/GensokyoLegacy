package dev.xkmc.gensokyolegacy.content.item.gift;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

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

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext level, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, level, list, flag);
		list.add(GLLang.ItemCommon.USAGE_TENGU_SAKE.get().withStyle(ChatFormatting.GRAY));
	}

}
