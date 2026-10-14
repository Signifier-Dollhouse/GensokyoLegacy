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
 * A drink (GiftType.DRINK) that is never used up: the gourd is drunk straight from and put
 * back where it was, so unlike the sakes it neither shrinks the stack nor leaves a bottle
 * behind — there is nothing in it to empty. The food properties are otherwise the drink's
 * own, so what comes out of the last gulp is exactly what came out of the first.
 * <p>
 * Suika carries one, and it is the top rung of her gift ladder (see
 * {@code GiftLootGen.suika}), which is the whole reason a gift can be worth more than a
 * consumable: it is the one thing on that ladder that keeps giving after she has left.
 */
public class GourdItem extends DrinkGiftItem {

	public GourdItem(Properties properties) {
		super(properties);
	}

	/**
	 * Drink it, then put it back exactly as it was.
	 * <p>
	 * {@link Item#finishUsingItem} hands the stack to {@link LivingEntity#eat}, which
	 * consumes one of it, and {@link LivingEntity#completeUsingItem} only writes the hand
	 * back when the returned stack is not the very object it passed in. So restoring the
	 * count and handing that same object back is enough to leave the gourd where it was: no
	 * copy, no re-inventory, and the client predicts it exactly the way the server does it.
	 * <p>
	 * There is deliberately no creative-mode exemption here — an infinite drink is the item's
	 * whole character, and a gourd that empties itself in survival but not in creative would
	 * be the opposite of that.
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
