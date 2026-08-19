package dev.xkmc.gensokyolegacy.content.item.gift;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Tengu sake — a strong drink that can be given to characters (GiftType.DRINK)
 * or drunk by the player for a temporary boost.
 */
public class TenguSakeItem extends AbstractGift {

	public TenguSakeItem(Properties properties) {
		super(properties.stacksTo(1), GiftType.DRINK, 5);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		if (!level.isClientSide) {
			user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
			user.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
		}
		return ItemStack.EMPTY;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return 32;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext level, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, level, list, flag);
		list.add(GLLang.ITEM$USAGE_TENGU_SAKE.get().withStyle(ChatFormatting.GRAY));
	}

}
