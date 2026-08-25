package dev.xkmc.gensokyolegacy.content.item.gift;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Fairy cake — an edible gift (GiftType.FOOD). Can be given to characters
 * or eaten by the player to restore hunger.
 */
public class FairyCakeItem extends Item {

	public FairyCakeItem(Properties properties) {

		super(properties.stacksTo(1).food(new FoodProperties.Builder()
				.nutrition(4).saturationModifier(0.3f).build()));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext level, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, level, list, flag);
		list.add(GLLang.ITEM$USAGE_FAIRY_CAKE.get().withStyle(ChatFormatting.GRAY));
	}

}
