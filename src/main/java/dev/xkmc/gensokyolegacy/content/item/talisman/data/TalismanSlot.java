package dev.xkmc.gensokyolegacy.content.item.talisman.data;

import dev.xkmc.gensokyolegacy.content.item.talisman.GLTalismans;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record TalismanSlot(ItemStack foldedStack, ItemStack paperStack) {

	public static final TalismanSlot EMPTY = new TalismanSlot(ItemStack.EMPTY, ItemStack.EMPTY);

	public TalismanSlot {
		if (foldedStack == null) foldedStack = ItemStack.EMPTY;
		if (paperStack == null) paperStack = ItemStack.EMPTY;
	}

	public boolean isEmpty() {
		return foldedStack.isEmpty() && paperStack.isEmpty();
	}

	public Item kindItem() {
		if (!paperStack.isEmpty()) return paperStack.getItem();
		var holder = GLTalismans.DC_TALISMAN_PAPER.get(foldedStack);
		return holder == null ? null : holder.value();
	}

	public TalismanSlot withFolded(ItemStack folded) {
		return new TalismanSlot(folded, paperStack);
	}

	public TalismanSlot withPaper(ItemStack paper) {
		return new TalismanSlot(foldedStack, paper);
	}

	public TalismanSlot clear() {
		return EMPTY;
	}
}