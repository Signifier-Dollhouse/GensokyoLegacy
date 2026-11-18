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

import java.util.*;

/**
 * Alice Margatroid, the doll-maker living deep in the Magical Forest. <p>Quest chain: 1.1 ({@code first_doll}) → 2.1
 * ({@code seven_colors}) → {@code daily_doll}. 2.1 is also one of the two ways into Marisa's talisman errand (see
 * {@code MarisaQDGen#talismanQuests}), which is why the quest ids are public.
 */
@SuppressWarnings("SameParameterValue")
public class AliceQDGen extends QuestDialogData {

	public static final ResourceLocation QUEST_FIRST_DOLL = GensokyoLegacy.loc("alice/first_doll");
	public static final ResourceLocation QUEST_SEVEN_COLORS = GensokyoLegacy.loc("alice/seven_colors");

	/**
	 * The seven colors of the Doll Glove, in spectral order. Shared by the 2.1 request, the daily roll and the glove
	 * trade so the three can never drift apart.
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
		dailyThanksKey = text("option", "daily_thanks", "(Accept the reward)");

		chats();
		quests();
		trades();
	}

	private void chats() {
		prefix("alice/chat");
		defaultDialog(GLEntities.ALICE.get(),
				"Oh - a visitor. Do come in. Is there something you need?",
				"...What a coincidence. You are a guest as well.",
				"Thanks for having me. Did you make all of this yourself? Quite impressive.",
				"I would like to trade for something.");
		// home-bound: the reply below is about living out here, which is not what
		// a guest standing on someone else's doorstep should say
		starter("alice/chat", new DialogStarter(GLEntities.ALICE.get(), List.of(homeBound()),
				starterText("start", "Is this mansion just for you?"),
				dialog("hi", "All by myself? Not quite — I have my dolls for company, so the mansion is hardly lonely.", List.of("dialog_talk_01", "dialog_talk_02", "dialog_talk_03", "dialog_talk_04"),
						option("hi/end", "I see."))
		));

		visitChats();

		// Unlocked by 2.1, the quest that hands out the first doll lance - this chat
		// says what a doll fights with, and deliberately points at a house further along.
		prefix("alice/chat_star_wand");
		chat("alice/chat_star_wand", GLEntities.ALICE.get(),
				List.of(hasQuest(QUEST_SEVEN_COLORS)),
				starterText("start", "How do dolls attack?"),
				dialog("talk", "A doll is a delicate piece of magic equipment - it cannot do much on its own. The lance I gave you is the simplest thing I can put in a doll's hand: it charges in like a wasp, and it delivers. But if you would rather keep your distance, the doll has to have a wand in its hand.", TALK_ANIMS,
						option("where", "A wand?",
								dialog("where_ans", "Another resident of the Magical Forest, the owner of the Kirisame Magic "
										+ "Shop, has things like that — but you'll have to get on good terms with her "
										+ "first.", List.of("dialog_talk_01", "dialog_talk_02", "dialog_talk_03", "dialog_talk_04"),
										option("where/end", "(Leave)")))),
				CHAT_INFO);
	}

	/**
	 * Alice out visiting. Marisa's house is the only place she goes, so this is written for that house rather than left
	 * host-agnostic. {@code visitingAt} implies she is a guest at all, so none of it can surface at her own door.
	 */
	private void visitChats() {
		prefix("alice/visit_marisa_house");
		chat("alice/visit_marisa_house_call", GLEntities.ALICE.get(),
				List.of(visitingAt(GLStructureGen.MARISA_HOUSE)),
				starterText("call", "What a coincidence - you are here to see her as well?"),
				dialog("call", "Good day. I came for some magical materials - are you here for the same?", List.of("dialog_greet"),
						option("terms", "I heard she sells more than materials?",
								dialog("terms_ans", "Yes. She does not do everything by herself - she hands out commissions, brews potions, and now and then turns up with tools of unclear provenance. That last part is better left unasked about.", List.of("dialog_talk_01", "dialog_talk_02", "dialog_talk_03", "dialog_talk_04"),
										option("terms/end", "Thank you for the warning.")))),
				CHAT_MISC);

		chat("alice/visit_marisa_house_greet", GLEntities.ALICE.get(),
				List.of(visitingAt(GLStructureGen.MARISA_HOUSE)),
				starterText("greet", "This is awfully far from where you live."),
				dialog("greet", "It is. I rarely go out, but materials still need restocking now and then; a change of "
						+ "scenery is nice for a while.", List.of("dialog_talk_02", "dialog_talk_03"),
						option("greet/end", "Let's talk again some other time.")),
				CHAT_MISC);
	}

	private void quests() {
		prefix("alice/first_doll");
		questAnims("start", List.of("dialog_talk_01", "dialog_talk_02", "dialog_talk_04"), THINK_ANIMS_ALL, "dialog_agree");
		questAnims("follow", List.of("dialog_talk_01", "dialog_talk_02", "dialog_talk_03", "dialog_talk_04"), "dialog_decline");
		questAnims("complete", "dialog_agree", "dialog_decline", TALK_ANIMS);
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
								.withPool(lootItem(GLItems.COIN.get(), 4)))),
				start("(Curiously examine the dolls)",
						"Curious about my dolls? Doll-making is my specialty. You've helped me a great deal — "
								+ "I could make one for you, but I'm short on materials right now. Bring me what I "
								+ "need, and I'll show you what a doll can do.",
						"Really? Then let me prepare the materials.", "These are the materials I need. Once they're gathered, I can start.", "All right.",
						"Just a quick look, maybe later.", "Is that so? I must have misunderstood, then.", "(Leave)"),
				follow("I would like to see the list again.",
						"Let me show you the list again. These materials are actually quite common in the "
								+ "Magical Forest.",
						"I see.", "Take your time. None of those materials are hard to come by.", null),
				complete("I have everything you asked for.",
						"Very well. Let me look at the materials.",
						"Here you go.", "Thank you for your help. This is the payment we agreed on.", "Then I will take it.")
		));

		prefix("alice/seven_colors");
		questAnims("start", TALK_ANIMS, THINK_ANIMS_ALL, "dialog_agree");
		questAnims("follow", TALK_ANIMS, TALK_ANIMS);
		questAnims("complete", List.of("dialog_agree", "dialog_think_02"), "dialog_talk_02", TALK_ANIMS);
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
						"(Shake head)", "Very well. Do think it over.", "(Leave)"),
				follow("I forgot which colors they were.",
						"Red, orange, yellow, light green, cyan, light blue, purple - four of each.",
						"Noted.", "The forest is never short of colors. What it lacks is persistence.", "I'll go on collecting."),
				complete("The dyes are gathered.",
						"Good, this is the last step. Hand me the materials — it will be done soon.",
						"(Hand over the items)",
						"Here: the glove, a doll to match it, and a lance. Go and try it out - I will ask you now and then how it feels in use.",
						"All right.")
		));

		dailyQuest();
	}

	private void dailyQuest() {
		prefix("alice/daily_doll");
		questAnims("daily", "dialog_talk_02", "dialog_decline", "dialog_agree", "dialog_think", TALK_ANIMS, "dialog_agree", List.of("dialog_talk_01", "dialog_talk_02", "dialog_talk_03", "dialog_talk_04"));
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
				"(Hand over the items)", "The quality is very good, I accept them. As payment, here is a doll and a custom-made cavalry lance - take good care of it.",
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
		// Alice sews by hand, so the glove is a weekly item at best while the plain
		// dolls come out one at a time; the lance is iron and a day's work, so one a day.
		trade("offer_doll_glove", new TradeOffer(GLEntities.ALICE.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SEVEN_COLORS)),
				new ItemStack(GLItems.DOLL_GLOVE.get()),
				new TradeRecurrence(1, 168000), gloveIngredients()));
		trade("offer_doll", new TradeOffer(GLEntities.ALICE.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SEVEN_COLORS)),
				new ItemStack(GLItems.DOLL.get()),
				new TradeRecurrence(1, 24000), List.of(item(GLItems.COIN.get(), 32))));
		trade("offer_doll_lance", new TradeOffer(GLEntities.ALICE.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SEVEN_COLORS)),
				new ItemStack(GLItems.DOLL_LANCE.get()),
				new TradeRecurrence(1, 24000), List.of(item(Items.IRON_INGOT, 8))));
		// The mini shield is a doll's shield: half the iron of a lance, gold for its trim, and one a day like it.
		trade("offer_mini_shield", new TradeOffer(GLEntities.ALICE.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SEVEN_COLORS)),
				new ItemStack(GLItems.MINI_SHIELD.get()),
				new TradeRecurrence(1, 24000), List.of(item(Items.IRON_INGOT, 4), item(Items.GOLD_INGOT, 1))));
	}

	/**
	 * One of each of the seven dyes plus three white wool: the glove's price.
	 */
	private List<IngredientEntry> gloveIngredients() {
		List<IngredientEntry> ans = new ArrayList<>(dyeIngredients(1));
		ans.add(item(Items.WHITE_WOOL, 3));
		return ans;
	}

	/**
	 * One of each of the seven dyes, {@code count} apiece: the single submit requirement.
	 */
	private Map<String, QuestRequirement<?, ?>> dyeRequirements(int count) {
		return Map.of("a-dyes", new SubmitItemRequirement(dyeIngredients(count)));
	}

	/**
	 * One {@code IngredientEntry} per dye, in spectral order.
	 */
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
				dialog("start/dialog_1", intro, anims("start", 0, THINK_ANIMS),
						option("start/reject", reject, dialog("start/reject/dialog_1", rejectLine, anims("start", 1, List.of()),
								option("start/reject/end", rejectEnd))),
						option("start/accept", accept, new StartQuestAction(),
								dialog("start/accept/dialog_1", acceptLine, anims("start", 2, List.of()),
										option("start/accept/end", acceptEnd)))));
	}

	private SimpleDialogOption follow(String button, String intro, String opt, String optLine, @Nullable String end) {
		var tail = end == null ? optionKey(byeKey) : option("follow_up/end/end", end);
		return option("follow_up", button,
				dialog("follow_up/dialog_1", intro, anims("follow", 0, THINK_ANIMS),
						option("follow_up/end", opt, dialog("follow_up/end/dialog_1", optLine,
								anims("follow", 1, List.of()), tail))));
	}

	private SimpleDialogOption complete(String button, String intro, String complete, String completeLine, String completeEnd) {
		return option("complete", button,
				dialog("complete/dialog_1", intro, anims("complete", 0, AGREE_ANIMS),
						option("complete/handover", complete, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine, anims("complete", 2, List.of()),
										option("complete/handover/end", completeEnd)))));
	}

	private SimpleDialogOption complete(String button, String intro, String complete, String completeLine, String completeEnd,
										String reject, String rejectLine, String rejectEnd) {
		return option("complete", button,
				dialog("complete/dialog_1", intro, anims("complete", 0, AGREE_ANIMS),
						option("complete/reject", reject, dialog("complete/reject/dialog_1", rejectLine, anims("complete", 1, List.of()),
								option("complete/reject/end", rejectEnd))),
						option("complete/handover", complete, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine, anims("complete", 2, List.of()),
										option("complete/handover/end", completeEnd)))));
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
				dialog("start/dialog_1", intro, anims("daily", 0, THINK_ANIMS),
						optionKey(dailyRejectKey,
								dialog("start/reject/dialog_1", rejectLine, anims("daily", 1, List.of()))),
						optionKey(dailyAcceptKey, new StartQuestAction(),
								dialog("start/accept/dialog_1", acceptLine, anims("daily", 2, List.of())))));
	}

	private GroupDialogOption dailyFollow(String followLine, @Nullable String endOverride, String optLine) {
		var end = endOverride == null
				? optionKey(dailyFollowEndKey, dialog("follow_up/end/dialog_1", optLine, anims("daily", 4, List.of())))
				: option("follow_up/end", endOverride,
				dialog("follow_up/end/dialog_1", optLine, anims("daily", 4, List.of())));
		return groupKey(dailyGroupKey, dailyFollowKey,
				dialog("follow_up/dialog_1", followLine, anims("daily", 3, THINK_ANIMS), end));
	}

	private GroupDialogOption dailyComplete(String opener, String gotemLine, String handover,
											String completeLine, @Nullable String thanks) {
		var done = thanks == null
				? dialog("complete/handover/dialog_1", completeLine, anims("daily", 6, List.of()))
				: dialog("complete/handover/dialog_1", completeLine, anims("daily", 6, List.of()),
				optionKey(dailyThanksKey));
		return new GroupDialogOption(dailyGroupKey, optionText("complete", opener), List.of(),
				Optional.of(dialog("complete/dialog_1", gotemLine, anims("daily", 5, AGREE_ANIMS),
						option("complete/handover", handover, new CompleteQuestAction(), done))));
	}

}
