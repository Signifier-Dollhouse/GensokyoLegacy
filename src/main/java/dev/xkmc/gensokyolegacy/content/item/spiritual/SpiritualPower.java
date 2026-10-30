package dev.xkmc.gensokyolegacy.content.item.spiritual;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Universal spiritual power stored on an item stack. The cap is a component too, so every item
 * declares its own intrinsic capacity; Yuyuko's fan is the first carrier.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public final class SpiritualPower {

	private SpiritualPower() {
	}

	public static int get(ItemStack stack) {
		return GLItems.SPIRITUAL_POWER.getOrDefault(stack, 0);
	}

	public static int max(ItemStack stack) {
		return Math.max(0, GLItems.SPIRITUAL_POWER_MAX.getOrDefault(stack, 0));
	}

	public static void set(ItemStack stack, int power) {
		GLItems.SPIRITUAL_POWER.set(stack, Mth.clamp(power, 0, max(stack)));
	}

	/**
	 * Adds {@code delta} clamped to the pool and returns the amount actually accepted.
	 */
	public static int add(ItemStack stack, int delta) {
		int old = get(stack);
		int next = Mth.clamp(old + delta, 0, max(stack));
		set(stack, next);
		return next - old;
	}

	public static void appendHoverText(ItemStack stack, List<Component> list) {
		list.add(GLLang.Spiritual.BAR.get(get(stack), max(stack)));
	}

}
