package dev.xkmc.gensokyolegacy.content.item.spiritual;

import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * An item whose durability bar displays its spiritual power pool.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public class SpiritualItem extends Item {

	public SpiritualItem(Properties properties, int max) {
		super(properties.component(GLItems.SPIRITUAL_POWER_MAX.get(), max));
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return SpiritualPower.max(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		int max = SpiritualPower.max(stack);
		if (max <= 0) return 0;
		return Mth.clamp(Math.round(13.0F * SpiritualPower.get(stack) / max), 0, 13);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x55FFFF;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		SpiritualPower.appendHoverText(stack, list);
	}

}
