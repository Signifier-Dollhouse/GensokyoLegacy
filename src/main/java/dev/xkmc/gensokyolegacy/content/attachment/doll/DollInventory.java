package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.l2serial.serialization.marker.OnInject;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Doll loadout storage as a mutable-free {@code @SerialClass}: one array slot per
 * {@link DollSlot} ordinal. Backed by a plain array (not an enum map) so l2serial
 * handles it as a native array codec, same trick as {@code TalismanPocketData}.
 * <p>
 * This is the item-side variant: reads return copies and writes produce new instances,
 * so stored {@link ItemStack}s are never aliased or mutated in place — the data
 * component rule. Backs the doll item's {@code DOLL_LOADOUT} component, hence value
 * {@code equals}/{@code hashCode}. The live ledger loadout is
 * {@link MutableDollInventory}, converting to/from this class at the item boundary.
 */
@SerialClass
public class DollInventory {

	public static DollInventory empty() {
		return new DollInventory();
	}

	private static ItemStack[] defaultStacks() {
		ItemStack[] ans = new ItemStack[DollSlot.values().length];
		Arrays.fill(ans, ItemStack.EMPTY);
		return ans;
	}

	@SerialField
	private ItemStack[] items = defaultStacks();

	private int hashCode;

	public DollInventory() {
	}

	private DollInventory(ItemStack[] items) {
		this.items = items;
	}

	@OnInject
	public void onInject() {
		items = normalize(items);
	}

	private static ItemStack[] normalize(@Nullable ItemStack[] data) {
		int n = DollSlot.values().length;
		ItemStack[] ans = data != null && data.length == n ? data : new ItemStack[n];
		if (data != null && data.length != n) {
			Arrays.fill(ans, ItemStack.EMPTY);
			System.arraycopy(data, 0, ans, 0, Math.min(data.length, n));
		}
		for (int i = 0; i < n; i++) {
			if (ans[i] == null) ans[i] = ItemStack.EMPTY;
		}
		return ans;
	}

	public ItemStack get(DollSlot slot) {
		ItemStack stack = items[slot.ordinal()];
		return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
	}

	public DollInventory with(DollSlot slot, ItemStack stack) {
		ItemStack[] next = items.clone();
		next[slot.ordinal()] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		return new DollInventory(next);
	}

	/**
	 * Hand switch: swaps main/off contents. Used when a doll is commanded to act with
	 * its off-hand item — the swap sticks until a further command swaps back.
	 */
	public DollInventory swappedHands() {
		ItemStack[] next = items.clone();
		int a = DollSlot.MAIN_HAND.ordinal(), b = DollSlot.OFF_HAND.ordinal();
		ItemStack tmp = next[a];
		next[a] = next[b];
		next[b] = tmp;
		return new DollInventory(next);
	}

	public boolean isEmpty(DollSlot slot) {
		ItemStack stack = items[slot.ordinal()];
		return stack == null || stack.isEmpty();
	}

	public boolean isEmpty() {
		for (ItemStack stack : items) {
			if (stack != null && !stack.isEmpty()) return false;
		}
		return true;
	}

	public DollInventory copy() {
		ItemStack[] next = new ItemStack[items.length];
		for (int i = 0; i < items.length; i++) {
			ItemStack stack = items[i];
			next[i] = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		}
		return new DollInventory(next);
	}

	@Override
	public int hashCode() {
		int h = hashCode;
		if (h == 0) {
			h = Arrays.hashCode(items);
			if (h == 0) h = 1;
			hashCode = h;
		}
		return h;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof DollInventory other && hashCode() == other.hashCode()) {
			return Arrays.equals(items, other.items);
		}
		return false;
	}

}
