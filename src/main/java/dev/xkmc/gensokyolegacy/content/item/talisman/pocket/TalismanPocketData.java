package dev.xkmc.gensokyolegacy.content.item.talisman.pocket;

import net.minecraft.world.item.Item;

public record TalismanPocketData(TalismanSlot[] slots) {

	public static final int MAX_SLOTS = 9;

	public TalismanPocketData {
		if (slots.length == 0) {
			slots = defaultSlots();
		} else if (slots.length != MAX_SLOTS) {
			var normalized = new TalismanSlot[MAX_SLOTS];
			for (int i = 0; i < MAX_SLOTS; i++) {
				normalized[i] = i < slots.length && slots[i] != null ? slots[i] : TalismanSlot.EMPTY;
			}
			slots = normalized;
		}
	}

	public static TalismanSlot[] defaultSlots() {
		var slots = new TalismanSlot[MAX_SLOTS];
		for (int i = 0; i < MAX_SLOTS; i++) {
			slots[i] = TalismanSlot.EMPTY;
		}
		return slots;
	}

	public TalismanSlot get(int index) {
		if (index < 0 || index >= MAX_SLOTS) return TalismanSlot.EMPTY;
		return slots[index];
	}

	public TalismanPocketData with(int index, TalismanSlot slot) {
		if (index < 0 || index >= MAX_SLOTS) return this;
		var copy = slots.clone();
		copy[index] = slot;
		return new TalismanPocketData(copy);
	}

	public int findSlot(Item paper) {
		for (int i = 0; i < MAX_SLOTS; i++) {
			var slot = slots[i];
			if (!slot.isEmpty() && paper.equals(slot.kindItem())) return i;
		}
		return -1;
	}

	public int findEmptySlot() {
		for (int i = 0; i < MAX_SLOTS; i++) {
			if (slots[i].isEmpty()) return i;
		}
		return -1;
	}
}