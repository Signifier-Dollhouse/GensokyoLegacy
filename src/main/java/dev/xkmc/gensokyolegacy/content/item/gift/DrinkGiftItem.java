package dev.xkmc.gensokyolegacy.content.item.gift;

import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;

import java.util.List;

/**
 * A drink that can be given to characters, currently the two sakes ({@code GiftType.ALCOHOL}). Its
 * effects live in {@link FoodProperties}, so drinking it consumes the stack and gives back what they
 * say (a bottle).
 */
public class DrinkGiftItem extends Item {

	public DrinkGiftItem(Properties properties) {
		super(properties);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, level, list, flag);
		FoodProperties props = stack.getFoodProperties(null);
		if (props == null || props.getEffects().isEmpty()) return;
		// listed like the witch's hex brew: effect name with its potency and remaining
		// duration, followed by the attribute lines a potion of the same effect would show
		PotionUtils.addPotionTooltip(props.getEffects().stream().map(pair -> pair.getFirst()).toList(),
				list, 1.0F);
	}

}
