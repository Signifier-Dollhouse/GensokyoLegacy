package dev.xkmc.gensokyolegacy.content.item.currency;

import net.minecraft.world.level.Level;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A stack of coins, the trade currency; plain otherwise.
 */
public class CoinStackItem extends Item {

	public CoinStackItem(Properties p) {
		super(p);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemCommon.CURRENCY.get());
	}

}
