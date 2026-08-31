package dev.xkmc.gensokyolegacy.init.data;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftType;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class GLLang {

	private GLLang() {
	}

	public interface LangEntry {
		String key();

		String def();

		int argn();

		@Nullable ChatFormatting format();

		default MutableComponent get(Object... args) {
			if (args.length != argn())
				throw new IllegalArgumentException("for " + ((Enum<?>) this).name() + ": expect " + argn() + " parameters, got " + args.length);
			var ans = Component.translatable(key(), args);
			if (format() != null) ans.withStyle(format());
			return ans;
		}

		default MutableComponent time(long diff) {
			if (diff < 0) diff = 0;
			int sec = (int) ((diff / 20) % 60);
			int min = (int) ((diff / 1200) % 60);
			int hrs = (int) (diff / 72000);
			var str = hrs == 0 ? "%d:%02d".formatted(min, sec) : "%d:%02d:%02d".formatted(hrs, min, sec);
			return get(str);
		}
	}

	// ========== Quest ==========
	public enum Quest implements LangEntry {
		TAB("Active Quests"),
		CHARACTER("For %s", 1),
		ITEM_SUBMIT_PASS("All criteria are met", 0, ChatFormatting.GREEN),
		ITEM_SUBMIT_FAIL("Some criteria are not met or used overlapped items");

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		Quest(String def) {
			this(def, 0);
		}

		Quest(String def, int argn) {
			this(def, argn, null);
		}

		Quest(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".quest." + name().toLowerCase(Locale.ROOT);
			this.format = format;
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public String def() {
			return def;
		}

		@Override
		public int argn() {
			return argn;
		}

		@Override
		public @Nullable ChatFormatting format() {
			return format;
		}
	}

	// ========== Info ==========
	public enum Info implements LangEntry {
		LOADING("Loading..."),
		BED_UNBOUND("This block is not linked to a structure"),
		BED_PRESENT("Character is present at (%s, %s, %s)", 3),
		BED_MISSING("Character is missing for %s", 1),
		BED_RESPAWN("Character respawning. Remaining time: %s", 1),
		ENTITY_UNBOUND("This character is not linked to a bed"),
		ENTITY_BED("Character's bed is at (%s, %s, %s)", 3),
		ENTITY_REPUTATION("Your reputation: %s", 1),
		ENTITY_FEED("Feed cool down: %s", 1),
		ENTITY_GIFT("Gift cool down: %s", 1),
		STRUCTURE_SCANNING("Scanning Structure...", 0),
		STRUCTURE_ABNORMAL("Found %s invalid blocks", 1),
		DOORS_TO_CLOSE("Doors to close (%s):", 1);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		Info(String def) {
			this(def, 0);
		}

		Info(String def, int argn) {
			this(def, argn, null);
		}

		Info(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".info." + name().toLowerCase(Locale.ROOT);
			this.format = format;
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public String def() {
			return def;
		}

		@Override
		public int argn() {
			return argn;
		}

		@Override
		public @Nullable ChatFormatting format() {
			return format;
		}
	}

	// ========== Trade ==========
	public enum Trade implements LangEntry {
		STOCK("Stock: %s/%s", 2),
		INGREDIENTS("Ingredients:", 0),
		OPTION("Trade");

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		Trade(String def) {
			this(def, 0);
		}

		Trade(String def, int argn) {
			this(def, argn, null);
		}

		Trade(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".trade." + name().toLowerCase(Locale.ROOT);
			this.format = format;
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public String def() {
			return def;
		}

		@Override
		public int argn() {
			return argn;
		}

		@Override
		public @Nullable ChatFormatting format() {
			return format;
		}
	}

	// ========== Misc (MSG + COMMAND + TAB) ==========
	public enum Misc implements LangEntry {
		MSG_RESET("msg.reset", "Character reset"),
		COMMAND_SUCCESS("command.success", "Success"),
		COMMAND_INVALID_ROLE("command.invalid_role", "Error: invalid role id"),
		TAB_TITLE("tab.title", "Gensokyo Roles", 0),
		TAB_NO_ROLE("tab.no_role", "Regular Human (No Role)", 0),
		TAB_MAIN_ROLE("tab.main_role", "%s (%s)", 2),
		TAB_ROLE_PROGRESS("tab.role_progress", "%s - %s", 2);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		Misc(String suffix, String def) {
			this(suffix, def, 0);
		}

		Misc(String suffix, String def, int argn) {
			this(suffix, def, argn, null);
		}

		Misc(String suffix, String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + "." + suffix;
			this.format = format;
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public String def() {
			return def;
		}

		@Override
		public int argn() {
			return argn;
		}

		@Override
		public @Nullable ChatFormatting format() {
			return format;
		}
	}

	// ========== ItemDebug ==========
	public enum ItemDebug implements LangEntry {
		WAND_BED("Click bed to reset character"),
		WAND_BLOCK("Click block to show structure bounds"),
		WAND_STRUCTURE("Sneak-click block to show structure option screen"),
		WAND_CHARACTER("Click character to reset global character data for you"),
		GLASS_PATH("Display character path finding"),
		GLASS_CHARACTER("Display character info"),
		GLASS_BED("Display bed info"),
		DOOR_DEBUG_USE("Right-click: bind nearest youkai"),
		DOOR_DEBUG_CLICK("Right-click block: tell bound youkai to go there"),
		DOOR_DEBUG_OVERLAY("Shows DOORS_TO_CLOSE of the bound youkai while held"),
		DOOR_DEBUG_NO_YOUKAI("No youkai nearby"),
		DOOR_DEBUG_UNBOUND("Not bound to a youkai. Right-click to bind."),
		DOOR_DEBUG_BOUND("Bound to %s", 1),
		DOOR_DEBUG_MISSING("Bound youkai is not loaded"),
		DOOR_DEBUG_MOVING("Youkai moving to (%s, %s, %s)", 3);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		ItemDebug(String def) {
			this(def, 0);
		}

		ItemDebug(String def, int argn) {
			this(def, argn, null);
		}

		ItemDebug(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".item." + name().toLowerCase(Locale.ROOT);
			this.format = format;
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public String def() {
			return def;
		}

		@Override
		public int argn() {
			return argn;
		}

		@Override
		public @Nullable ChatFormatting format() {
			return format;
		}
	}

	// ========== ItemFurnace ==========
	public enum ItemFurnace implements LangEntry {
		FURNACE_1_LORE("A portable magical furnace that emits heat. Can slowly smelt adjacent items when placed in inventory.", 0, ChatFormatting.GRAY),
		FURNACE_1_USE("Right click the item in inventory to switch modes.", 0, ChatFormatting.GRAY),
		FURNACE_1_OFF("Mode: OFF", 0, ChatFormatting.GRAY),
		FURNACE_1_DESC("Mode: %s", 1, ChatFormatting.GRAY);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		ItemFurnace(String def) {
			this(def, 0);
		}

		ItemFurnace(String def, int argn) {
			this(def, argn, null);
		}

		ItemFurnace(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".item." + name().toLowerCase(Locale.ROOT);
			this.format = format;
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public String def() {
			return def;
		}

		@Override
		public int argn() {
			return argn;
		}

		@Override
		public @Nullable ChatFormatting format() {
			return format;
		}
	}

	// ========== ItemCommon (generic + gear) ==========
	public enum ItemCommon implements LangEntry {
		HAS_ABILITY("gensokyo roles"),
		OBTAIN("Source: ", 0, ChatFormatting.GRAY),
		UNKNOWN("???", 0, ChatFormatting.GRAY),
		USAGE("Usage: ", 0, ChatFormatting.GRAY),
		GIFT_FAVOR("Favor: %s", 1),
		GIFT_TYPE("Type: %s", 1),
		USAGE_TENGU_SAKE("Drink for a temporary boost.", 0, ChatFormatting.GRAY),
		USAGE_FAIRY_CAKE("A sweet cake. Eat to restore hunger.", 0, ChatFormatting.GRAY),
		USAGE_MAGIC_BOOK("Can be used as furnace fuel.", 0, ChatFormatting.GRAY),
		OBTAIN_FAIRY_ICE("Crafted by Cirno.", 0, ChatFormatting.GRAY),
		USAGE_FAIRY_ICE("Throw to deal damage and freeze target.", 0, ChatFormatting.GRAY),
		OBTAIN_FROZEN_FROG("Dropped when Cirno freezes a frog.", 0, ChatFormatting.GRAY),
		USAGE_FROZEN_FROG("Throw toward target to summon a frog.", 0, ChatFormatting.GRAY),
		USAGE_STRAW_HAT("With %s, you can equip it on frogs to allow them to eat raiders", 1, ChatFormatting.GRAY),
		OBTAIN_SUWAKO_HAT("Drops when frog with hat eats %s different kinds of raiders in front of villagers", 1, ChatFormatting.GRAY),
		USAGE_SUWAKO_HAT("Grants constant %s. Allows using Cyan and Lime danmaku without consumption.", 1, ChatFormatting.GRAY),
		OBTAIN_KOISHI_HAT("Drops when blocking Koishi attacks %s times in a row", 1, ChatFormatting.GRAY),
		USAGE_KOISHI_HAT("Grants constant %s. Allows using Blue and Red danmaku without consumption.", 1, ChatFormatting.GRAY),
		OBTAIN_RUMIA_HAIRBAND("Drops when player defeat Ex. Rumia with Danmaku", 0, ChatFormatting.GRAY),
		USAGE_RUMIA_HAIRBAND("Shift player towards %s. Drops heads when killing mobs. Flesh and blood drops no longer require knife (bonus when still using knife).", 1, ChatFormatting.GRAY),
		OBTAIN_REIMU_HAIRBAND("Feed Reimu a variety of food", 0, ChatFormatting.GRAY),
		USAGE_REIMU_HAIRBAND("Enables creative flight. Your danmaku damage bypasses magical protection.", 0, ChatFormatting.GRAY),
		USAGE_CIRNO_HAIRBAND("Shift player towards %s. Your magic damage freezes target (and frogs). Allows using Light Blue danmaku without consumption.", 1, ChatFormatting.GRAY),
		USAGE_FAIRY_WINGS("When you are %s, enables creative flight.", 1, ChatFormatting.GRAY);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		ItemCommon(String def) {
			this(def, 0);
		}

		ItemCommon(String def, int argn) {
			this(def, argn, null);
		}

		ItemCommon(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".item." + name().toLowerCase(Locale.ROOT);
			this.format = format;
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public String def() {
			return def;
		}

		@Override
		public int argn() {
			return argn;
		}

		@Override
		public @Nullable ChatFormatting format() {
			return format;
		}
	}

	// ========== ItemUmbrella (item.umbrella.* + umbrella.*) ==========
	public enum ItemUmbrella implements LangEntry {
		// item.umbrella.*
		MODE("item.umbrella_mode", "Mode: %s", 1),
		SLOT("item.umbrella_slot", "Slot: %s", 1),
		SLOT_EMPTY_ITEM("item.umbrella_slot_empty", "Empty slot", 0, ChatFormatting.DARK_GRAY),
		LOCKED_TRAVEL("item.umbrella_locked_travel", "Travel mode locked: apply chorus fruit in anvil", 0, ChatFormatting.DARK_RED),
		LOCKED_CAPTURE("item.umbrella_locked_capture", "Capture mode locked: apply echo shard in anvil", 0, ChatFormatting.DARK_RED),
		RECORDED("item.umbrella_recorded", "Recorded position %s: %s", 2),
		WAYPOINT("item.umbrella_waypoint", "Teleported to %s", 1),
		TRAVEL_START("item.umbrella_travel_start", "Charging border travel...", 0),
		TRAVEL_DONE("item.umbrella_travel_done", "Border travel complete", 0),
		CAPTURED("item.umbrella_captured", "Teleported %s to %s", 2),
		CAPTURE_FAIL("item.umbrella_capture_fail", "Cannot capture this entity", 0, ChatFormatting.RED),
		TRAVEL_CANCELLED("item.umbrella_travel_cancelled", "Travel cancelled", 0, ChatFormatting.GRAY),
		DIM_MISSING("item.umbrella_dim_missing", "Dimension %s not found", 1),
		RENAME_TITLE("item.umbrella_rename_title", "Rename Position", 0),
		WHEEL("item.umbrella_wheel", "Hold %s to open wheel", 1, ChatFormatting.GRAY),
		DISTANCE("item.umbrella_distance", "Distance: %s blocks", 1, ChatFormatting.GRAY),
		UNLOCKED_TRAVEL("item.umbrella_unlocked_travel", "Travel unlocked", 0, ChatFormatting.GREEN),
		UNLOCKED_CAPTURE("item.umbrella_unlocked_capture", "Capture unlocked", 0, ChatFormatting.GREEN),
		DESC_RECORD("item.umbrella_desc_record", "Right-click block to record position", 0, ChatFormatting.GRAY),
		DESC_WAYPOINT("item.umbrella_desc_waypoint", "Right-click to teleport to selected position", 0, ChatFormatting.GRAY),
		DESC_TRAVEL("item.umbrella_desc_travel", "Hold use to charge and travel forward", 0, ChatFormatting.GRAY),
		DESC_CAPTURE("item.umbrella_desc_capture", "Interact with entity to teleport it to selected position", 0, ChatFormatting.GRAY),
		// umbrella.*
		MODE_RECORD("umbrella.mode_record", "Record"),
		MODE_WAYPOINT("umbrella.mode_waypoint", "Waypoint"),
		MODE_TRAVEL("umbrella.mode_travel", "Travel"),
		MODE_CAPTURE("umbrella.mode_capture", "Capture"),
		SLOT_EMPTY("umbrella.slot_empty", "Empty"),
		WHEEL_TARGET("umbrella.wheel_target", "Target Position"),
		WHEEL_DISTANCE("umbrella.wheel_distance", "Travel Distance"),
		WHEEL_EDIT("umbrella.wheel_edit", "Edit Position"),
		MANAGE_TITLE("umbrella.manage_title", "Manage Positions"),
		MANAGE_RENAME("umbrella.manage_rename", "Rename"),
		MANAGE_DELETE("umbrella.manage_delete", "Delete");

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		ItemUmbrella(String suffix, String def) {
			this(suffix, def, 0);
		}

		ItemUmbrella(String suffix, String def, int argn) {
			this(suffix, def, argn, null);
		}

		ItemUmbrella(String suffix, String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + "." + suffix;
			this.format = format;
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public String def() {
			return def;
		}

		@Override
		public int argn() {
			return argn;
		}

		@Override
		public @Nullable ChatFormatting format() {
			return format;
		}
	}

	public static void genLang(RegistrateLangProvider pvd) {
		for (var group : new LangEntry[][]{
				Quest.values(), Info.values(), Trade.values(), Misc.values(),
				ItemDebug.values(), ItemFurnace.values(), ItemCommon.values(), ItemUmbrella.values()}) {
			for (var e : group) {
				pvd.add(e.key(), e.def());
			}
		}

		for (var type : GiftType.values()) {
			pvd.add(GensokyoLegacy.MODID + ".gift.type." + type.name().toLowerCase(Locale.ROOT), type.getLangName());
		}

		pvd.add(GensokyoLegacy.MODID + ".subtitle.koishi_ring", "Koishi Phone Call");
	}
}
