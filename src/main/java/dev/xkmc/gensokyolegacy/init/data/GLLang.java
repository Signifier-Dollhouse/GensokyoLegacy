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
		YOUKAI_AVOID("%s might not want to see you for a while", 1),
		ENTITY_FEED("Feed cool down: %s", 1),
		ENTITY_GIFT("Gift cool down: %s", 1),
		ENTITY_PARTY_EATEN("At the party: eaten %s of %s hunger", 2),
		ENTITY_PARTY_APPEAL("At the party: appeal eaten %s", 1),
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

	// ========== Party ==========
	public enum Party implements LangEntry {
		NO_FOOD("There is no food laid out on the tables"),
		NO_SEATS("There is nowhere to sit - put some chairs around the table"),
		PARTY_EXISTS("There is already a party here"),
		NOBODY_CAME("Nobody came. Lay out more food, or get to know someone better"),
		NEAR_TOO_PLAIN("%s would come, but wants a grander spread than this", 1),
		NEAR_NO_SEAT("%s would come, but there is no seat free", 1),
		SENT("Invitations sent. %s guest(s) are on the way", 1),
		LOOKING_FOR_CHAIR("%s cannot find a chair yet - put another one out if you can", 1),
		NO_CHAIR("%s gave up - no chair was free for her. Put out another chair", 1),
		ENDED("The party is over"),
		NO_PARTY("There is no party here"),
		PREVIEW_TITLE("Spread appeal: %s, seats: %s", 2),
		PREVIEW_HEADER("%s would come:", 1),
		PREVIEW_GUEST("  %s  weight %s  rep %s  wants %s appeal", 4),
		PREVIEW_NONE("Nobody would come"),
		INFO_PHASE("Phase: %s", 1),
		INFO_ROSTER("Invited: %s", 1),
		INFO_SERVED("Served %s of %s hunger", 2),
		INFO_APPEAL("Appeal served: %s", 1),
		INFO_GUESTS("Guests present: %s", 1),
		LETTER_HINT("Right-click a table to send it out");

		private final String def;
		private final int argn;
		private final String key;

		Party(String def) {
			this(def, 0);
		}

		Party(String def, int argn) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".party." + name().toLowerCase(Locale.ROOT);
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
			return null;
		}
	}

	// ========== Trade ==========
	public enum Trade implements LangEntry {
		STOCK("Stock: %s/%s", 2),
		INGREDIENTS("Ingredients:", 0),
		BUY("Buy %s from %s", 2),
		SELL("Sell %s to %s", 2),
		CRAFT("Request %s to craft %s", 2),
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
		MSG_SHELF_RETURNED("msg.shelf_returned", "Your shop shelf was broken while you were away; its stock and earnings have been returned to you."),
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

	// ========== Biome ==========
	/**
	 * Display names for the Gensokyo biomes. The key is the vanilla {@code biome.<namespace>.<id>} key rather than the
	 * usual {@code gensokyolegacy.*} one, because that is what the F3 biome readout and the locator item read.
	 */
	public enum Biome implements LangEntry {
		MUENZUKA("muenzuka", "Muenzuka"),
		HUMAN_VILLAGE("human_village", "Human Village"),
		MYOUREN_TEMPLE_BARRENS("myouren_temple_barrens", "Myouren Temple Barrens"),
		MAGICAL_FOREST("magical_forest", "Forest of Magic"),
		BAMBOO_FOREST_OF_THE_LOST("bamboo_forest_of_the_lost", "Bamboo Forest of the Lost"),
		SAKURA_FOREST("sakura_forest", "Sakura Forest"),
		HILL_OF_THE_NAMELESS("hill_of_the_nameless", "Hill of the Nameless"),
		UNTRODDEN_VALLEY("untrodden_valley", "Untrodden Valley"),
		GENBU_RAVINE("genbu_ravine", "Genbu Ravine"),
		YOUKAI_MOUNTAIN_FOOTHILLS("youkai_mountain_foothills", "Youkai Mountain Foothills"),
		SANCTUARY("sanctuary", "Sanctuary"),
		HELL_OF_BLOOD_POOLS("hell_of_blood_pools", "Hell of Blood Pools"),
		ANIMAL_REALM("animal_realm", "Animal Realm"),
		HELL_OF_BLAZING_FIRES("hell_of_blazing_fires", "Hell of Blazing Fires"),
		FORMER_HELL("former_hell", "Former Hell"),
		MISTY_LAKE("misty_lake", "Misty Lake"),
		MISTY_LAKE_SHORE("misty_lake_shore", "Misty Lake Shore"),
		TENGU_SETTLEMENT("tengu_settlement", "Tengu Settlement"),
		SECRET_HEAVEN_CLIFF("secret_heaven_cliff", "Secret Heaven Cliff"),
		MOUNTAIN_INTERIOR("mountain_interior", "Mountain Interior"),
		YOUKAI_MOUNTAIN_SUMMIT("youkai_mountain_summit", "Youkai Mountain Summit"),
		SUNFLOWER_PLAINS("sunflower_plains", "Sunflower Plains");

		private final String def;
		private final String key;

		Biome(String id, String def) {
			this.def = def;
			this.key = "biome." + GensokyoLegacy.MODID + "." + id;
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
			return 0;
		}

		@Override
		public @Nullable ChatFormatting format() {
			return null;
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
		DOOR_DEBUG_MOVING("Youkai moving to (%s, %s, %s)", 3),
		PARTY_WAND_HOVER("Look at a food block to see what it is worth"),
		PARTY_WAND_EAT("Click a food block to take one serving"),
		PARTY_WAND_SIMULATE("Click a party table to simulate a party"),
		PARTY_WAND_INVALID("That is tagged food but nothing to eat"),
		PARTY_WAND_FOOD("%s", 1),
		PARTY_WAND_SERVING("serving size: %s", 1),
		PARTY_WAND_SERVES("serves: %s", 1),
		PARTY_WAND_FOOD_VALUES("hunger: %s   saturation: %s", 2),
		PARTY_WAND_TOTAL_HUNGER("hunger total: %s", 1),
		PARTY_WAND_ATTRACTION("attraction: %s", 1),
		PARTY_WAND_PARTY("Party simulated at (%s, %s, %s)", 3),
		PARTY_WAND_COUNTS("tables: %s   seats: %s   food: %s", 3),
		PARTY_WAND_TOTALS("hunger: %s   appeal: %s", 2),
		PARTY_WAND_NOBODY("Nobody would come"),
		PARTY_WAND_GUEST("  %s", 1),
		PARTY_WAND_STALE("(taken away from that table - may be out of date)");

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
		FURNACE_1_USE("Right click the item in inventory to switch modes.", 0, ChatFormatting.GRAY),
		FURNACE_1_OFF("Mode: OFF", 0, ChatFormatting.GRAY),
		FURNACE_1_DESC("Mode: %s", 1, ChatFormatting.GRAY),
		FURNACE_2_USE("Right click the item in inventory to open its menu. Click the hakkero in the center to switch modes.", 0, ChatFormatting.GRAY),
		FURNACE_2_FUEL("Feed it fuel to reach ten times furnace speed.", 0, ChatFormatting.GRAY),
FURNACE_2_BOOST("Held in main hand while riding a broom, right click to burn fuel and speed up.", 0, ChatFormatting.GRAY);

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

	// ========== ItemLores (flavour lines shown under an item's name) ==========
	public enum ItemLores implements LangEntry {
		CENTIPICKAXE_LORE("A pickaxe with a taste for blood, and a habit of hanging on to it.", 0, ChatFormatting.GRAY),
		DOWSER_LORE("Nazrin's dowser. Borrowed, and never returned.", 0, ChatFormatting.GRAY),
		MERMAID_PEARL_LORE("Still wet, and still listening to something a long way down.", 0, ChatFormatting.GRAY),
		CAT_BELL_LORE("Small, and heavier than any bell has a right to be.", 0, ChatFormatting.GRAY),
		BROOM_LORE("It flies itself, given there is somebody left to fly.", 0, ChatFormatting.GRAY),
		DOLL_LANCE_LORE("A polearm built to a doll's reach rather than a hand's.", 0, ChatFormatting.GRAY),
		MINI_SHIELD_LORE("Too small for a hand, but a doll can still get behind it.", 0, ChatFormatting.GRAY),
		STAR_WAND_LORE("Throws the star, keeps the wand.", 0, ChatFormatting.GRAY),
		IRON_DAGGER_LORE("Thrown, not dropped — it knows the way back.", 0, ChatFormatting.GRAY),
		STRANGE_GLASSES_LORE("Someone else's idea of what counts as real.", 0, ChatFormatting.GRAY),
		FURNACE_1_LORE("A portable magical furnace that emits heat. Can slowly smelt adjacent items when placed in inventory.", 0, ChatFormatting.GRAY),
		FURNACE_2_LORE("A portable magical furnace with the leaks sealed. Smelts what you put in it, and rather too fast at that.", 0, ChatFormatting.GRAY),
		STRAW_HAT_LORE("Woven by someone with four legs, for a frog with a very large appetite.", 0, ChatFormatting.GRAY),
		SUWAKO_HAT_LORE("A river god's hat, traded for a frog's very specific appetite.", 0, ChatFormatting.GRAY),
		KOISHI_HAT_LORE("Taken off a third party, and it never stopped sulking about it.", 0, ChatFormatting.GRAY),
		ICE_WINGS_LORE("Six crystals, and it is suddenly very hard to count past nine.", 0, ChatFormatting.GRAY),
		FAIRY_WINGS_LORE("A fairy's wing, priced at half of whatever the wearer has.", 0, ChatFormatting.GRAY),
		FANCY_FAN_LORE("Pretty enough to give away, and it blows when you wave it.", 0, ChatFormatting.GRAY);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		ItemLores(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".lore." + name().toLowerCase(Locale.ROOT);
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
		UNKNOWN("???", 0, ChatFormatting.GRAY),
		GIFT_FAVOR("Favor: %s", 1),
		GIFT_TYPE("Type: %s", 1),
		BLOW("Right-click to blow a gust", 0, ChatFormatting.AQUA),
		INFINITE("Never runs out", 0, ChatFormatting.AQUA);

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
		RECORD_OVERWRITE("Slot %s already holds %s — confirming will overwrite it", 2, ChatFormatting.RED),
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
		MANAGE_DELETE("Delete"),
		MANAGE_SLOT("Slot %s", 1),
		MANAGE_NO_HELD("No umbrella held", 0, ChatFormatting.RED);

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
		MODE_SUMMON("Rally"),
		MODE_VOLLEY("Attack"),
		MODE_SUPER("Skill"),
		MODE_SUICIDE("Suicide"),
		MODE_EDITOR("Edit Loadout"),
		DESC_SUMMON("Summon all parked dolls, or recall all summoned ones", 0, ChatFormatting.GRAY),
		DESC_VOLLEY("Order every armed doll to fire once, in turn", 0, ChatFormatting.GRAY),
		DESC_SUPER("Order one random doll to fire its super attack", 0, ChatFormatting.GRAY),
		DESC_SUICIDE("Order one random doll to dive and detonate", 0, ChatFormatting.GRAY),
		DESC_EDITOR("Open the sighted doll's loadout out of reach", 0, ChatFormatting.GRAY),
		SUMMONED("Summoned %s dolls", 1),
		RECALLED("Recalled %s dolls (%s parked to data)", 2),
		VOLLEY("Volley ordered", 0),
		SUPER("Super attack ordered: %s", 1),
		SUICIDE("Suicide dive ordered: %s", 1),
		NO_TARGET("No target in sight", 0, ChatFormatting.RED),
		NO_DOLL("No available doll", 0, ChatFormatting.RED),
		NO_WEAPON("No doll holds a valid weapon", 0, ChatFormatting.RED),
		TOO_FAR("Target is too far to melee", 0, ChatFormatting.RED),
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

	// ========== ItemDaggerGlove (dagger_glove.*) ==========
	public enum ItemDaggerGlove implements LangEntry {
		MODE("Mode: %s", 1),
		MODE_SINGLE("Single"),
		MODE_FAN("Fan"),
		MODE_HOMING("Homing"),
		DESC_SINGLE("Throw one dagger", 0, ChatFormatting.GRAY),
		DESC_FAN("Throw 7 daggers in a 30-degree fan", 0, ChatFormatting.GRAY),
		DESC_HOMING("Throw 10 daggers that turn onto the target you are looking at", 0, ChatFormatting.GRAY),
		RUNE("Rune: +%s ticks cooldown", 1, ChatFormatting.LIGHT_PURPLE),
		NO_DAGGER("No daggers", 0, ChatFormatting.RED),
		NO_TARGET("No target in sight", 0, ChatFormatting.RED);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		ItemDaggerGlove(String def) {
			this(def, 0);
		}

		ItemDaggerGlove(String def, int argn) {
			this(def, argn, null);
		}

		ItemDaggerGlove(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".dagger_glove." + name().toLowerCase(Locale.ROOT);
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
		ALCHEMY("jei.gensokyolegacy.alchemy", "Alchemy Pot"),
		TRADE("jei.gensokyolegacy.trade", "Trade"),
		QUEST("jei.gensokyolegacy.quest", "Quest Rewards"),
		GIFT("jei.gensokyolegacy.gift", "Party Gifts"),
		INTERACTION("jei.gensokyolegacy.interaction", "Entity Interaction");

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

	// ========== Jei extra labels ==========
	public enum JeiExtra implements LangEntry {
		MAX_STOCK("Max stock: %s", 1),
		MORE("+%s more", 1),
		COUNT_RANGE("%s-%s", 2),
		COUNT_UNKNOWN("?", 0),
		REWARD_EXP("%s experience", 1),
		REWARD_REPUTATION("%s reputation", 1),
		REWARD_LOOT("Loot table: %s", 1),
		GIFT_BAND("%s-%s", 2),
		GIFT_BAND_TOP("%s+", 1),
		GIFT_CHANCE("Chance: %s%%", 1),
		GIFT_NOTE("One gift each; the appeal she ate picks how generous she is", 0),
		MORE_RUNGS("+%s more rungs", 1),
		KIND_FEED("Feeding", 0),
		FEED_WORTH("+%s reputation", 1),
		FEED_NOTE("Right-click her with it; the thank-you only lands once she no longer counts you an enemy, and feeding on its own caps at %s reputation", 1);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		JeiExtra(String def, int argn) {
			this(def, argn, null);
		}

		JeiExtra(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".jei." + name().toLowerCase(Locale.ROOT);
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
		EXTRA("+%s more", 1),
		PROGRESS("Progress: %s%%", 1);

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
	/**
	 * The Sekibanki head's item tooltip. Its own group rather than {@link ItemGlove} or a doll one,
	 * because the four lines are this gadget's alone: what possession is, is not a doll fact.
	 */
	public enum ItemSekibankiHead implements LangEntry {
		SUMMON("Right-click to call the head out", 0, ChatFormatting.GRAY),
		COLLECT("Right-click the head with an empty hand to collect it", 0, ChatFormatting.GRAY),
		POSSESS("Shift and right-click the head to see through it", 0, ChatFormatting.GRAY),
		EXIT("Left-click to stop looking through it", 0, ChatFormatting.GRAY);

		private final String def;
		private final int argn;
		private final String key;
		private final @Nullable ChatFormatting format;

		ItemSekibankiHead(String def, int argn, @Nullable ChatFormatting format) {
			this.def = def;
			this.argn = argn;
			this.key = GensokyoLegacy.MODID + ".sekibanki_head." + name().toLowerCase(Locale.ROOT);
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
				Quest.values(), Info.values(), Trade.values(), Misc.values(), Biome.values(),
				ItemDebug.values(), ItemFurnace.values(), ItemLores.values(), ItemCommon.values(), ItemUmbrella.values(), ItemGlove.values(), ItemDaggerGlove.values(), Alchemy.values(), Jei.values(), JeiExtra.values(),
				Talisman.values(), Doll.values(), Party.values(), ItemSekibankiHead.values()}) {
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
