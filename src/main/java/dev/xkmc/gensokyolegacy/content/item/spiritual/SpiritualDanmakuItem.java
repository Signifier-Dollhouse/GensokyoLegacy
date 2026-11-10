package dev.xkmc.gensokyolegacy.content.item.spiritual;

import net.minecraft.world.level.Level;
import dev.xkmc.danmakuapi.api.DanmakuBullet;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A danmaku item that carries a spiritual power pool: the throw comes from {@link DanmakuItem}, the
 * bar and the pool line from {@link SpiritualPower}.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public class SpiritualDanmakuItem extends DanmakuItem implements SpiritualCarrier {

	private final int maxPower;

	public SpiritualDanmakuItem(Properties properties, DanmakuBullet type, DyeColor color, float size, int max) {
		super(properties, type, color, size);
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

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged;
	}

}
