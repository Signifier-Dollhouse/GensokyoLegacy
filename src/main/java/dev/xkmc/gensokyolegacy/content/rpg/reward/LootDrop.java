package dev.xkmc.gensokyolegacy.content.rpg.reward;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One item a loot table can drop, and how many of it.
 * <p>
 * A count that the table states outright goes into the stack, and is drawn like any other stack
 * count. A count it only bounds cannot go into a stack at all — every single number would
 * misdescribe some roll — so that one is a bare stack plus {@link #countText()}. A count the
 * table does not state at all, because it turns on the luck or the enchantment of whoever rolls
 * it, is neither bound nor stated, and says so.
 *
 * @param stack the item, always a single one; the count is carried separately
 * @param min   the fewest of this item one roll can give, or null if the table does not say
 * @param max   the most, or null if the table does not say
 */
public record LootDrop(ItemStack stack, @Nullable Integer min, @Nullable Integer max) {

	/** whether one roll gives exactly this many, every time */
	public boolean certain() {
		return min != null && min.equals(max);
	}

	/** the stack for a slot: the count baked in when it is certain, a bare one otherwise */
	public ItemStack slotStack() {
		return certain() ? stack.copyWithCount(min) : stack.copy();
	}

	/** how many of this item one roll gives, as far as the table says */
	public Component countText() {
		if (certain()) return Component.literal("" + min);
		if (min == null) return GLLang.JeiExtra.COUNT_UNKNOWN.get();
		return GLLang.JeiExtra.COUNT_RANGE.get(min, max);
	}

}
