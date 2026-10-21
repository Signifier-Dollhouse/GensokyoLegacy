package dev.xkmc.gensokyolegacy.content.item.gift;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Suika's drink that is never used up: drunk straight from and put back where it was, so unlike the
 * sakes it neither shrinks the stack nor leaves a bottle behind, and the last gulp gives what the first did.
 * <p>
 * Suika carries one, the top rung of her gift ladder ({@code GiftLootGen.suika}): it keeps giving after she has left.
 */
public class GourdItem extends DrinkGiftItem {

	public GourdItem(Properties properties) {
		super(properties);
	}

	/**
	 * Drink it, then put it back exactly as it was. {@link Item#finishUsingItem} hands the stack to
	 * {@link LivingEntity#eat}, which consumes one, and {@link LivingEntity#completeUsingItem} only
	 * writes the hand back when the returned stack is not the very object it passed in — so restoring
	 * the count and returning that object leaves the gourd where it was, with no copy.
	 */
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		int count = stack.getCount();
		super.finishUsingItem(stack, level, entity);
		stack.setCount(count);
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, context, list, flag);
		list.add(GLLang.ItemCommon.INFINITE.get());
	}

}
