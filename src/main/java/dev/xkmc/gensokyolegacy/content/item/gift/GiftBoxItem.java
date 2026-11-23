package dev.xkmc.gensokyolegacy.content.item.gift;

import dev.xkmc.gensokyolegacy.content.item.common.HoldToOpenItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The sealed box Morichika sells: a half-second hold opens it and rolls one gift from his shelf stock.
 */
public class GiftBoxItem extends HoldToOpenItem {

	private static final int HOLD_TICKS = 10;

	public GiftBoxItem(Properties properties) {
		super(properties, HOLD_TICKS);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.GIFT_BOX_LORE.get());
		list.add(GLLang.ItemGiftBox.OPEN.get());
	}

}
