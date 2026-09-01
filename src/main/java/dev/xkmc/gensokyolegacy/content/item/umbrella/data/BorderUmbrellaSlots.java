package dev.xkmc.gensokyolegacy.content.item.umbrella.data;

public record BorderUmbrellaSlots(BorderSlot[] slots) {

	public static final int MAX_SLOTS = 8;

	public BorderUmbrellaSlots() {
		this(initSlots());
	}

	private static BorderSlot[] initSlots() {
		var arr = new BorderSlot[MAX_SLOTS];
		for (int i = 0; i < MAX_SLOTS; i++) arr[i] = BorderSlot.empty();
		return arr;
	}

	public BorderUmbrellaSlots(BorderSlot[] slots) {
		if (slots.length != MAX_SLOTS) {
			var arr = new BorderSlot[MAX_SLOTS];
			for (int i = 0; i < MAX_SLOTS; i++)
				arr[i] = i < slots.length && slots[i] != null ? slots[i] : BorderSlot.empty();
			this.slots = arr;
		} else {
			this.slots = slots;
		}
	}

	public BorderSlot get(int idx) {
		int i = Math.floorMod(idx, MAX_SLOTS);
		var s = slots[i];
		return s == null ? BorderSlot.empty() : s;
	}

	public BorderUmbrellaSlots with(int idx, BorderSlot slot) {
		var ns = slots.clone();
		ns[Math.floorMod(idx, MAX_SLOTS)] = slot;
		return new BorderUmbrellaSlots(ns);
	}

	public static BorderUmbrellaSlots defaultSlots() {
		return new BorderUmbrellaSlots();
	}
}
