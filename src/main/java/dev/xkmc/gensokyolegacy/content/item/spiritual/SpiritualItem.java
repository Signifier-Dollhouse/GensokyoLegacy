package dev.xkmc.gensokyolegacy.content.item.spiritual;

import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * An item whose durability bar displays its spiritual power pool.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public class SpiritualItem extends Item implements SpiritualCarrier {

	private final int maxPower;

	public SpiritualItem(Properties properties, int max) {
		super(properties);
		this.maxPower = max;
	}

	@Override
	public int maxPower() {
		return maxPower;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return SpiritualPower.barVisible(stack);
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return SpiritualPower.barWidth(stack);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return SpiritualPower.BAR_COLOR;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		SpiritualPower.appendHoverText(stack, list);
	}

}
