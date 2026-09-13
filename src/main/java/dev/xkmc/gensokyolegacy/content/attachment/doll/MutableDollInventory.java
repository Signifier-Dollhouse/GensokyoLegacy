package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.l2serial.serialization.marker.OnInject;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Mutable variant of {@link DollInventory}: the live loadout held by {@code DollData}.
 * Unlike the item component, {@link #get(DollSlot)} returns the stored stack directly
 * so callers can modify it in place (shrink ammo, damage talismans, swap hands).
 * Backed by a plain array indexed by {@link DollSlot} ordinal (not an enum map).
 */
@SerialClass
public class MutableDollInventory {

	private static ItemStack[] defaultStacks() {
		ItemStack[] ans = new ItemStack[DollSlot.values().length];
		Arrays.fill(ans, ItemStack.EMPTY);
		return ans;
	}

	@SerialField
	private ItemStack[] items = defaultStacks();

	public MutableDollInventory() {
	}

	public static MutableDollInventory fromInventory(DollInventory data) {
		MutableDollInventory ans = new MutableDollInventory();
		for (DollSlot slot : DollSlot.values())
			ans.set(slot, data.get(slot));
		return ans;
	}

	public DollInventory toInventory() {
		DollInventory ans = new DollInventory();
		for (DollSlot slot : DollSlot.values())
			ans = ans.with(slot, get(slot));
		return ans;
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

	/**
	 * Returns the live stored stack — callers may modify it directly.
	 */
	public ItemStack get(DollSlot slot) {
		ItemStack stack = items[slot.ordinal()];
		return stack == null ? ItemStack.EMPTY : stack;
	}

	/**
	 * Stores the stack directly, taking ownership. Mutates in place.
	 */
	public void set(DollSlot slot, ItemStack stack) {
		items[slot.ordinal()] = stack.isEmpty() ? ItemStack.EMPTY : stack;
	}

	/**
	 * Hand switch, in place: swaps main/off contents. Used when a doll is commanded
	 * to act with its off-hand item — the swap sticks until a further command swaps back.
	 */
	public void swapHands() {
		int a = DollSlot.MAIN_HAND.ordinal(), b = DollSlot.OFF_HAND.ordinal();
		ItemStack tmp = items[a];
		items[a] = items[b];
		items[b] = tmp;
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

	public void clear() {
		Arrays.fill(items, ItemStack.EMPTY);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(items);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof MutableDollInventory other && Arrays.equals(items, other.items);
	}

}
