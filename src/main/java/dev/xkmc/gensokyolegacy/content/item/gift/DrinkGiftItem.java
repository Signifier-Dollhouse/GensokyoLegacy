package dev.xkmc.gensokyolegacy.content.item.gift;

import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.List;

/**
 * A drink that can be given to characters (GiftType.DRINK). It carries its drinking
 * effects as {@link FoodProperties}, so drinking it consumes the stack and gives back
 * whatever the properties say (a glass bottle, for the sakes).
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
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, context, list, flag);
		FoodProperties props = stack.getFoodProperties(null);
		if (props == null || props.effects().isEmpty()) return;
		// listed like the witch's hex brew: effect name with its potency and remaining
		// duration, followed by the attribute lines a potion of the same effect would show
		PotionContents.addPotionTooltip(props.effects().stream().map(FoodProperties.PossibleEffect::effect).toList(),
				list::add, 1.0F, context.tickRate());
	}

}
