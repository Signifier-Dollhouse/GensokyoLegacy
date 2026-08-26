package dev.xkmc.gensokyolegacy.content.item.umbrella.data;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum BorderUmbrellaMode {
	RECORD,
	WAYPOINT,
	TRAVEL,
	CAPTURE;

	public ItemStack icon() {
		return switch (this) {
			case RECORD -> new ItemStack(Items.LODESTONE);
			case WAYPOINT -> new ItemStack(Items.RESPAWN_ANCHOR);
			case TRAVEL -> new ItemStack(Items.ENDER_PEARL);
			case CAPTURE -> new ItemStack(Items.IRON_BARS);
		};
	}

	public Component displayName() {
		return switch (this) {
			case RECORD -> GLLang.UMBRELLA$MODE_RECORD.get();
			case WAYPOINT -> GLLang.UMBRELLA$MODE_WAYPOINT.get();
			case TRAVEL -> GLLang.UMBRELLA$MODE_TRAVEL.get();
			case CAPTURE -> GLLang.UMBRELLA$MODE_CAPTURE.get();
		};
	}

	public boolean isAvailable(ItemStack stack) {
		var unlock = GLItems.UMBRELLA_UNLOCK.get(stack);
		if (unlock == null) {
			// default: travel and capture locked
			return this != TRAVEL && this != CAPTURE;
		}
		return switch (this) {
			case TRAVEL -> unlock.travelUnlocked();
			case CAPTURE -> unlock.captureUnlocked();
			default -> true;
		};
	}

	/**
	 * For future extensibility: allow adding custom handling per mode.
	 * Each mode can have its own logic; adding a new enum constant only requires
	 * adding a case in BorderUmbrellaItem switch statements.
	 */
	public boolean isHiddenWhenLocked() {
		return this == TRAVEL || this == CAPTURE;
	}
}