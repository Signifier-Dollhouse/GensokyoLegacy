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

	// ========== Talisman ==========
	public enum Talisman implements LangEntry {
		BLANK("Blank folded talisman"),
		FOLDED("Folded Paper Talisman: %s", 1),
		KIND_HEAL("Healing"),
		KIND_SPEED("Speed Boost"),
		KIND_HYDROPHOBIC("Hydrophobic"),
		KIND_LAVA("Lava Affinity"),
		KIND_SHELTER("Shelter"),
		DURABILITY("Uses left: %s / %s", 2),
		FOLD("Right-click to fold into a folded talisman", 0, ChatFormatting.GRAY),
		UNFOLD("At full durability, right-click to unfold into talisman paper", 0, ChatFormatting.GRAY),
		EQUIP("Equip in a charm curio slot to activate"),
		HEAL("Recovers %s%% of max health when health is low", 1),
		SPEED("Grants speed while sprinting"),
		HYDROPHOBIC("Restores air supply while drowning"),
		LAVA("Grants lava affinity while on fire or in lava: faster swim speed, clear lava vision, fire immunity"),
		SHELTER("Protects against heavy hits"),
		POCKET("Talisman Pocket"),
		POCKET_DESC("Right click the item to open its talisman magazine.", 0, ChatFormatting.GRAY);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		Talisman(String def) {
			this(def, 0);
		}

		Talisman(String def, int argn) {
			this(def, argn, null);
		}

		Talisman(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".talisman." + name().toLowerCase(Locale.ROOT);
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
		ENTITY_REPUTATION("Your reputation: %s / %s", 2),
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
		USAGE_MAGIC_BOOK("Can be used as furnace fuel.", 0, ChatFormatting.GRAY),
		OBTAIN_FAIRY_ICE("Crafted by Cirno.", 0, ChatFormatting.GRAY),
		USAGE_FAIRY_ICE("Throw to deal damage and freeze target.", 0, ChatFormatting.GRAY),
		OBTAIN_FROZEN_FROG("Dropped when Cirno freezes a frog.", 0, ChatFormatting.GRAY),
		USAGE_FROZEN_FROG("Throw toward target to summon a frog.", 0, ChatFormatting.GRAY),
		USAGE_STRAW_HAT("With %s, you can equip it on frogs to allow them to eat raiders", 1, ChatFormatting.GRAY),
		OBTAIN_SUWAKO_HAT("Drops when frog with hat eats %s different kinds of raiders", 1, ChatFormatting.GRAY),
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

	// ========== ItemUmbrella (umbrella.*) ==========
	public enum ItemUmbrella implements LangEntry {
		MODE("Mode: %s", 1),
		SLOT("Slot: %s", 1),
		SLOT_EMPTY_ITEM("Empty slot", 0, ChatFormatting.DARK_GRAY),
		LOCKED_TRAVEL("Travel mode locked: apply chorus fruit in anvil", 0, ChatFormatting.DARK_RED),
		LOCKED_CAPTURE("Capture mode locked: apply echo shard in anvil", 0, ChatFormatting.DARK_RED),
		RECORDED("Recorded position %s: %s", 2),
		WAYPOINT("Teleported to %s", 1),
		TRAVEL_START("Charging border travel...", 0),
		TRAVEL_DONE("Border travel complete", 0),
		CAPTURED("Teleported %s to %s", 2),
		CAPTURE_FAIL("Cannot capture this entity", 0, ChatFormatting.RED),
		TRAVEL_CANCELLED("Travel cancelled", 0, ChatFormatting.GRAY),
		DIM_MISSING("Dimension %s not found", 1),
		RENAME_TITLE("Rename Position", 0),
		WHEEL("Hold %s to open wheel", 1, ChatFormatting.GRAY),
		DISTANCE("Distance: %s blocks", 1, ChatFormatting.GRAY),
		UNLOCKED_TRAVEL("Travel unlocked", 0, ChatFormatting.GREEN),
		UNLOCKED_CAPTURE("Capture unlocked", 0, ChatFormatting.GREEN),
		DESC_RECORD("Right-click block to record position", 0, ChatFormatting.GRAY),
		DESC_WAYPOINT("Right-click to teleport to selected position", 0, ChatFormatting.GRAY),
		DESC_TRAVEL("Hold use to charge and travel forward", 0, ChatFormatting.GRAY),
		DESC_CAPTURE("Interact with entity to teleport it to selected position", 0, ChatFormatting.GRAY),
		MODE_RECORD("Record"),
		MODE_WAYPOINT("Waypoint"),
		MODE_TRAVEL("Travel"),
		MODE_CAPTURE("Capture"),
		SLOT_EMPTY("Empty"),
		WHEEL_TARGET("Target Position"),
		WHEEL_DISTANCE("Travel Distance"),
		WHEEL_EDIT("Edit Position"),
		MANAGE_TITLE("Manage Positions"),
		MANAGE_RENAME("Rename"),
		MANAGE_DELETE("Delete");

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		ItemUmbrella(String def) {
			this(def, 0);
		}

		ItemUmbrella(String def, int argn) {
			this(def, argn, null);
		}

		ItemUmbrella(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".umbrella." + name().toLowerCase(Locale.ROOT);
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

	// ========== ItemGlove (glove.*) ==========
	public enum ItemGlove implements LangEntry {
		MODE("Mode: %s", 1),
		WHEEL("Hold %s to open wheel", 1, ChatFormatting.GRAY),
		MODE_SUMMON("Summon / Recall"),
		MODE_HEAL_MARK("Heal Mark"),
		MODE_VOLLEY("Volley"),
		MODE_SUPER("Super Attack"),
		MODE_SUICIDE("Suicide"),
		MODE_STOP("Stop"),
		MODE_EDITOR("Edit Loadout"),
		DESC_SUMMON("Summon all parked dolls, or recall all summoned ones", 0, ChatFormatting.GRAY),
		DESC_HEAL_MARK("Mark the sighted target for healing", 0, ChatFormatting.GRAY),
		DESC_VOLLEY("Order every armed doll to fire once, in turn", 0, ChatFormatting.GRAY),
		DESC_SUPER("Order one random doll to fire its super attack", 0, ChatFormatting.GRAY),
		DESC_SUICIDE("Order one random doll to dive and detonate", 0, ChatFormatting.GRAY),
		DESC_STOP("Halt all summoned dolls", 0, ChatFormatting.GRAY),
		DESC_EDITOR("Open the sighted doll's loadout out of reach", 0, ChatFormatting.GRAY),
		SUMMONED("Summoned %s dolls", 1),
		RECALLED("Recalled %s dolls (%s parked to data)", 2),
		VOLLEY("Volley ordered", 0),
		SUPER("Super attack ordered: %s", 1),
		SUICIDE("Suicide dive ordered: %s", 1),
		STOPPED("Stopped %s dolls", 1),
		MARKED("Marked %s for healing", 1),
		UNMARKED("Unmarked %s", 1),
		NO_TARGET("No target in sight", 0, ChatFormatting.RED),
		NO_DOLL("No available doll", 0, ChatFormatting.RED),
		NOT_DOLL("Target is not your doll", 0, ChatFormatting.RED);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		ItemGlove(String def) {
			this(def, 0);
		}

		ItemGlove(String def, int argn) {
			this(def, argn, null);
		}

		ItemGlove(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".glove." + name().toLowerCase(Locale.ROOT);
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

	// ========== Jei ==========
	public enum Jei implements LangEntry {
		ALCHEMY("jei.gensokyolegacy.alchemy", "Alchemy Pot");

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		Jei(String key, String def) {
			this(key, def, 0);
		}

		Jei(String key, String def, int argn) {
			this(key, def, argn, null);
		}

		Jei(String key, String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = key;
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

	// ========== Alchemy Pot Overlay ==========
	public enum Alchemy implements LangEntry {
		ALLOW("Possible ingredients"),
		EXTRA("+%s more", 1);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		Alchemy(String def) {
			this(def, 0);
		}

		Alchemy(String def, int argn) {
			this(def, argn, null);
		}

		Alchemy(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".alchemy." + name().toLowerCase(Locale.ROOT);
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

	// ========== Doll ==========
	public enum Doll implements LangEntry {
		NO_SPACE("No inventory space"),
		RESYNC_MISSING("Doll entity missing — restoring"),
		RESYNC_ORPHAN("Removed a strayed doll"),
		RESYNC_TAMPERED("Removed a tampered doll"),
		BROKEN("This doll is broken — repair it in an anvil with wool", 0, ChatFormatting.RED),
		TOO_MANY("Too many summoned dolls (max %s)", 1, ChatFormatting.RED);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		Doll(String def) {
			this(def, 0, null);
		}

		Doll(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".doll." + name().toLowerCase(Locale.ROOT);
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
				ItemDebug.values(), ItemFurnace.values(), ItemCommon.values(), ItemUmbrella.values(), ItemGlove.values(), Alchemy.values(), Jei.values(),
				Talisman.values(), Doll.values()}) {
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
