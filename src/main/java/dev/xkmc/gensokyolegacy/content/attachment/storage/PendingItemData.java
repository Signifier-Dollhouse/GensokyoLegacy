package dev.xkmc.gensokyolegacy.content.attachment.storage;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.item.ItemStack;

/**
 * One pending item payout: a display item plus remaining count,
 * and uncollected emerald earnings. Used by {@link PendingItemStorage}
 * to restore lost contents (e.g. a broken shop shelf) to their owner.
 */
@SerialClass
public class PendingItemData {

	@SerialField
	public ItemStack stack = ItemStack.EMPTY;
	@SerialField
	public int stock, earning;

	public PendingItemData() {
	}

	public PendingItemData(ItemStack stack, int stock, int earning) {
		this.stack = stack;
		this.stock = stock;
		this.earning = earning;
	}

	public boolean isEmpty() {
		return (stack.isEmpty() || stock <= 0) && earning <= 0;
	}

}
