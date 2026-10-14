package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One thing a rung will hand over, and how often she picks it over the others standing in it.
 *
 * @param stack  the item, always a single one: a gift is exactly one item
 * @param chance her share of the rung, 0 to 1, or null where the rung cannot say it
 */
public record GiftEntry(ItemStack stack, @Nullable Float chance) {

	/** how likely she is to leave this on the table, as a percentage */
	public Component chanceText() {
		if (chance == null) return GLLang.JeiExtra.GIFT_CHANCE.get(GLLang.JeiExtra.COUNT_UNKNOWN.get());
		// in tenths, because the weights are small and thirds are common; a whole one is not padded
		long tenths = Math.round(chance * 1000);
		return GLLang.JeiExtra.GIFT_CHANCE.get(tenths % 10 == 0 ? "" + tenths / 10 : tenths / 10 + "." + tenths % 10);
	}

}