package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.rpg.action.CompleteQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.StartQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.condition.HasQuestCompletedCondition;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.GroupDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.SimpleDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestRecurrence;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.QuestRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.SubmitItemRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ExpReward;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ReputationReward;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Alice Margatroid, the doll-maker living deep in the Magical Forest.
 *
 * <p>Quest chain: 1.1 ({@code first_doll}) → 2.1 ({@code seven_colors}) →
 * {@code daily_doll}. 2.1 is also one of the two ways into Marisa's talisman
 * errand (see {@code MarisaQDGen#talismanQuests}), which is why the quest ids
 * are public.
 */
@SuppressWarnings("SameParameterValue")
public class AliceQDGen extends QuestDialogData {

	public static final ResourceLocation QUEST_FIRST_DOLL = GensokyoLegacy.loc("alice/first_doll");
	public static final ResourceLocation QUEST_SEVEN_COLORS = GensokyoLegacy.loc("alice/seven_colors");

	/**
	 * The seven colors of the Doll Glove, in spectral order. Shared by the
	 * 2.1 request, the daily roll and the glove trade so the three can never
	 * drift apart.
	 */
	private static final List<Item> SPECTRUM_DYES = List.of(
			Items.RED_DYE, Items.ORANGE_DYE, Items.YELLOW_DYE, Items.LIME_DYE,
			Items.CYAN_DYE, Items.LIGHT_BLUE_DYE, Items.PURPLE_DYE);

	private final String byeKey;
	private final String dailyGroupKey;
	private final String dailyStartKey;
	private final String dailyAcceptKey;
	private final String dailyRejectKey;
	private final String dailyFollowKey;
	private final String dailyFollowEndKey;
	private final String dailyThanksKey;

	public AliceQDGen() {
		prefix("alice/shared");
		byeKey = text("option", "bye", "(Leave)");
		dailyGroupKey = text("option", "daily_group", "Daily Tasks");
		dailyStartKey = text("option", "daily_start", "[Daily] Do you need help?");
		dailyAcceptKey = text("option", "daily_accept", "I'll see to it.");
		dailyRejectKey = text("option", "daily_reject", "That might be difficult.");
		dailyFollowKey = text("option", "daily_follow", "[Daily] Sorry, I forgot what the list said.");
		dailyFollowEndKey = text("option", "daily_follow_end", "Understood.");
		dailyThanksKey = text("option", "daily_thanks", "The pleasure was mine.");

		chats();
		quests();
		trades();
	}

	private void chats() {
		prefix("alice/chat");
		defaultDialog(GLEntities.ALICE.get(),
				"Oh - a visitor. Do come in. Is there something you need?",
				"...What a coincidence. You are a guest as well.",
				"I would like to trade for something.");
		// home-bound: the reply below is about living out here, which is not what
		// a guest standing on someone else's doorstep should say
		starter("alice/chat", new DialogStarter(GLEntities.ALICE.get(), List.of(homeBound()),
				starterText("start", "Do you live out here all by yourself?"),
				dialog("hi", "All by myself? Not quite. I have the forest and my dolls for company, and my magic research besides - was there something you needed?",
						option("hi/end", "Not right now."))
		));

		visitChats();

		// Unlocked by 2.1, which is also the quest that hands out the first doll
		// lance - this chat says what a doll fights with and where the wands come from.
		// Deliberately not home-bound: it points at a house further along, which is
		// exactly what a guest should hear.
		prefix("alice/chat_star_wand");
		chat("alice/chat_star_wand", GLEntities.ALICE.get(),
				List.of(hasQuest(QUEST_SEVEN_COLORS)),
				starterText("start", "What are a doll's methods of attack?"),
				dialog("talk", "A doll is a delicate piece of magic equipment - it cannot do much on its own. The lance I gave you is the simplest thing I can put in a doll's hand: it charges in like a wasp, and it delivers. But if you would rather keep your distance, the doll has to have a wand in its hand.",
						option("where", "How does one get hold of that wand?",
								dialog("where_ans", "Another resident of the Magical Forest - the owner of the Kirisame Magic Shop makes the things. She does not advertise, but she always has stock.",
										option("where/end", "Then I'll go and see her.")))),
				CHAT_INFO);
	}

	/**
	 * Alice out visiting. Marisa's house is the only place she goes, so this is
	 * written for that house rather than left host-agnostic. {@code visitingAt}
	 * implies she is a guest at all, so none of it can surface at her own door.
	 */
	private void visitChats() {
		prefix("alice/visit_marisa_house");
		chat("alice/visit_marisa_house_call", GLEntities.ALICE.get(),
				List.of(visitingAt(GLStructureGen.MARISA_HOUSE)),
				starterText("call", "What a coincidence - you are here to see her as well?"),
				dialog("call", "Good day. I came for some magical materials - are you here for the same?",
						option("terms", "I heard she sells more than materials?",
								dialog("terms_ans", "Yes. She does not do everything by herself - she hands out commissions, brews potions, and now and then turns up with tools of unclear provenance. That last part is better left unasked about.",
										option("terms/end", "Thank you for the warning.")))),
				CHAT_MISC);

		chat("alice/visit_marisa_house_greet", GLEntities.ALICE.get(),
				List.of(visitingAt(GLStructureGen.MARISA_HOUSE)),
				starterText("greet", "This is all rather far from your mansion."),
				dialog("greet", "It is. I am not often away - but materials still have to be restocked, and getting out for a while is not unpleasant.",
						option("greet/end", "Do as you like. I have business of my own with her.")),
				CHAT_MISC);
	}

	private void quests() {
		prefix("alice/first_doll");
		quest("alice/first_doll", new Quest(GLEntities.ALICE.get(), List.of(),
				questTitle("Doll Materials"), questDesc("Bring Alice the materials every doll is made of."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-materials", new SubmitItemRequirement(List.of(
								item(Items.STRING, 8),
								itemTag(ItemTags.WOOL, 8),
								item(GLItems.MYSTICAL_STRAW.get(), 4)))
				)),
				List.of(new ExpReward(50), new ReputationReward(10, 300, 10, 300),
						loot("alice/first_doll", LootTable.lootTable()
								.withPool(lootItem(Items.EMERALD, 4)))),
				start("Are these dolls really handmade?",
						"Every one of them. The materials are simple, but the technique behind them is not. My stores are empty - bring me what I need and I will show you what a doll can do.",
						"I'll gather the materials.", "These are the materials I need. With these I can make a doll.", "I'll be right back.",
						"Do you not gather your own materials?", "If nobody helped me, I would go and get them myself.", "I have things to do."),
				follow("I would like to see the list again.",
						"Let me show you the list again. One of the items is fairly common under the canopy.",
						"I see.", "Take your time. None of those materials are hard to come by.", null),
				complete("I have everything you asked for.",
						"Very well. Let me look at the materials.",
						"Here you go.", "Thank you for your help. This is the payment we agreed on.", "Then I will take it.",
						"Um, some of it still seems to be missing.", "No rush. Gather it all together and I will start.", "Understood.")
		));

		prefix("alice/seven_colors");
		quest("alice/seven_colors", new Quest(GLEntities.ALICE.get(),
				List.of(new HasQuestCompletedCondition(QUEST_FIRST_DOLL)),
				questTitle("Seven Colors"), questDesc("Bring Alice four of each of the seven dyes the Doll Glove is woven from."),
				Optional.empty(),
				new TreeMap<>(dyeRequirements(4)),
				List.of(new ExpReward(200), new ReputationReward(20, 300, 10, 300),
						loot("alice/seven_colors", LootTable.lootTable()
								.withPool(lootItem(GLItems.DOLL_GLOVE.get(), 1))
								.withPool(lootItem(GLItems.DOLL.get(), 1))
								.withPool(lootItem(GLItems.DOLL_LANCE.get(), 1)))),
				start("How are dolls controlled?",
						"Controlling a doll is not easy to learn. But I have a project going now: getting people who know no magic to work a doll. It takes seven dyes, four of each - the colors tell the different commands apart. Will you help me?",
						"Consider it done.", "Then it is settled. Four of each - read the requirements over carefully before you go.", "I'll be back.",
						"I have more important things to do first.", "Very well. Do think it over.", "(Leave)"),
				follow("I seem to have forgotten which colors they were.",
						"Red, orange, yellow, light green, cyan, light blue, purple - four of each.",
						"Noted.", "The forest is never short of colors. What it lacks is persistence.", "I'll go on collecting."),
				complete("All seven colors, four of each.",
						"I have brought the dyes.",
						"These are the dyes you asked for.",
						"Here: the glove, a doll to match it, and a lance. Go and try it out - I will ask you now and then how it feels in use.",
						"Then I'll put them to good use.",
						"These aren't what you asked for.", "Then I will wait. The exact shades matter - nothing else quite substitutes for them.", "Very well.")
		));

		dailyQuest();
	}

	private void dailyQuest() {
		prefix("alice/daily_doll");
		// Three independent rolls out of the seven dyes: the same dye can come
		// up twice, which keeps the daily from being a fixed shopping list.
		var dyePool = LootPool.lootPool().setRolls(ConstantValue.exactly(3));
		for (var dye : SPECTRUM_DYES)
			dyePool.add(LootItem.lootTableItem(dye));
		var dyeTable = requestTable("daily_doll", LootTable.lootTable().withPool(dyePool));
		// The spear heads: rolled per run off the same table, so the number Alice asks
		// for is fixed once the quest starts and shown in the request like the dyes are.
		var ironTable = requestTable("daily_doll_iron", LootTable.lootTable()
				.withPool(lootItem(Items.IRON_INGOT, 6, 8)));

		daily("alice/daily_doll", "Doll Restock", "Collect the cloth, straw and iron Alice needs for her dolls, plus a few dyes.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_SEVEN_COLORS)), 60, 10, 150, 0, 0,
				"I need a few things for a magic project. I have had one of my dolls write the list out for you - gather it for me. In return, I will make a doll of your own.",
				"Very well. Everything I need is on this list.",
				"Very well. Then another time.",
				"Then I will write you a fresh list.",
				null, "Good. I will leave it to you.",
				"I have what you asked for.", "Thank you. Let me just check the materials.",
				"Here.", "The quality is very good, I accept them. As payment, here is a doll and a custom-made cavalry lance - take good care of it.",
				"You're welcome.",
				new TreeMap<>(Map.of(
						"a-materials", new SubmitItemRequirement(List.of(
								item(Items.STRING, 8),
								itemTag(ItemTags.WOOL, 8),
								item(GLNaturalBlocks.BROOM_GRASS.asItem(), 8))),
						"b-iron", rollItem(ironTable),
						"c-dye", rollItem(dyeTable)
				)), LootTable.lootTable()
						.withPool(lootItem(GLItems.DOLL.get(), 1))
						.withPool(lootItem(GLItems.DOLL_LANCE.get(), 1)));
	}

	private void trades() {
		prefix("alice");
		// Alice sews by hand, so the glove is a weekly item at best; the plain
		// dolls she can turn out one at a time. The lance is the cheap one - iron
		// and a day's work - but a day is still a day, so one a day it is.
		trade("offer_doll_glove", new TradeOffer(GLEntities.ALICE.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SEVEN_COLORS)),
				new ItemStack(GLItems.DOLL_GLOVE.get()),
				new TradeRecurrence(1, 168000), gloveIngredients()));
		trade("offer_doll", new TradeOffer(GLEntities.ALICE.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SEVEN_COLORS)),
				new ItemStack(GLItems.DOLL.get()),
				new TradeRecurrence(1, 24000), List.of(item(Items.EMERALD, 32))));
		trade("offer_doll_lance", new TradeOffer(GLEntities.ALICE.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SEVEN_COLORS)),
				new ItemStack(GLItems.DOLL_LANCE.get()),
				new TradeRecurrence(1, 24000), List.of(item(Items.IRON_INGOT, 8))));
	}

	/** One of each of the seven dyes plus three white wool: the glove's price. */
	private List<IngredientEntry> gloveIngredients() {
		List<IngredientEntry> ans = new ArrayList<>(dyeIngredients(1));
		ans.add(item(Items.WHITE_WOOL, 3));
		return ans;
	}

	/** One of each of the seven dyes, {@code count} apiece: the single submit requirement. */
	private Map<String, QuestRequirement<?, ?>> dyeRequirements(int count) {
		return Map.of("a-dyes", new SubmitItemRequirement(dyeIngredients(count)));
	}

	/** One {@code IngredientEntry} per dye, in spectral order. */
	private List<IngredientEntry> dyeIngredients(int count) {
		List<IngredientEntry> ans = new ArrayList<>();
		for (var dye : SPECTRUM_DYES)
			ans.add(item(dye, count));
		return ans;
	}

	private SimpleDialogOption start(String button, String intro,
									 String accept, String acceptLine, String acceptEnd,
									 String reject, String rejectLine, String rejectEnd) {
		return option("start", button,
				dialog("start/dialog_1", intro, THINK_ANIMS,
						option("start/reject", reject, dialog("start/reject/dialog_1", rejectLine, option("start/reject/end", rejectEnd))),
						option("start/accept", accept, new StartQuestAction(),
								dialog("start/accept/dialog_1", acceptLine, option("start/accept/end", acceptEnd)))));
	}

	private SimpleDialogOption follow(String button, String intro, String opt, String optLine, @Nullable String end) {
		var tail = end == null ? optionKey(byeKey) : option("follow_up/end/end", end);
		return option("follow_up", button,
				dialog("follow_up/dialog_1", intro, THINK_ANIMS,
						option("follow_up/end", opt, dialog("follow_up/end/dialog_1", optLine, tail))));
	}

	private SimpleDialogOption complete(String button, String intro, String complete, String completeLine, String completeEnd,
										String reject, String rejectLine, String rejectEnd) {
		return option("complete", button,
				dialog("complete/dialog_1", intro, AGREE_ANIMS,
						option("complete/reject", reject, dialog("complete/reject/dialog_1", rejectLine, option("complete/reject/end", rejectEnd))),
						option("complete/handover", complete, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine, option("complete/handover/end", completeEnd)))));
	}

	private void daily(String id, String title, String desc, QuestRecurrence rec,
					   List<QuestCondition<?>> conditions, int exp, int rep, int softCap, int capIncrease, int maxCap,
					   String intro, String acceptLine, String rejectLine, String followLine, @Nullable String followEndOverride, String optLine,
					   String completeOpener, String gotemLine, String handover, String completeLine, @Nullable String thanks,
					   Map<String, QuestRequirement<?, ?>> reqs, LootTable.Builder loot) {
		quest(id, new Quest(GLEntities.ALICE.get(), conditions,
				questTitle(title), questDesc(desc),
				Optional.of(rec),
				new TreeMap<>(reqs),
				List.of(new ExpReward(exp), new ReputationReward(rep, softCap, capIncrease, maxCap),
						loot(id, loot)),
				dailyStart(intro, acceptLine, rejectLine),
				dailyFollow(followLine, followEndOverride, optLine),
				dailyComplete(completeOpener, gotemLine, handover, completeLine, thanks)));
	}

	private GroupDialogOption dailyStart(String intro, String acceptLine, String rejectLine) {
		return groupKey(dailyGroupKey, dailyStartKey,
				dialog("start/dialog_1", intro, THINK_ANIMS,
						optionKey(dailyRejectKey,
								dialog("start/reject/dialog_1", rejectLine)),
						optionKey(dailyAcceptKey, new StartQuestAction(),
								dialog("start/accept/dialog_1", acceptLine))));
	}

	private GroupDialogOption dailyFollow(String followLine, @Nullable String endOverride, String optLine) {
		var end = endOverride == null
				? optionKey(dailyFollowEndKey, dialog("follow_up/end/dialog_1", optLine))
				: option("follow_up/end", endOverride, dialog("follow_up/end/dialog_1", optLine));
		return groupKey(dailyGroupKey, dailyFollowKey,
				dialog("follow_up/dialog_1", followLine, THINK_ANIMS, end));
	}

	private GroupDialogOption dailyComplete(String opener, String gotemLine, String handover,
											String completeLine, @Nullable String thanks) {
		var done = thanks == null
				? dialog("complete/handover/dialog_1", completeLine)
				: dialog("complete/handover/dialog_1", completeLine, optionKey(dailyThanksKey));
		return new GroupDialogOption(dailyGroupKey, optionText("complete", opener), List.of(),
				Optional.of(dialog("complete/dialog_1", gotemLine, AGREE_ANIMS,
						option("complete/handover", handover, new CompleteQuestAction(), done))));
	}

}
