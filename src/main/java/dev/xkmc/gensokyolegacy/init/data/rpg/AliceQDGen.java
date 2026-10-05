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
		byeKey = text("option", "bye", "Farewell.");
		dailyGroupKey = text("option", "daily_group", "Daily Tasks");
		dailyStartKey = text("option", "daily_start", "Is there work today?");
		dailyAcceptKey = text("option", "daily_accept", "I'll see to it.");
		dailyRejectKey = text("option", "daily_reject", "Another time.");
		dailyFollowKey = text("option", "daily_follow", "Could you repeat the task?");
		dailyFollowEndKey = text("option", "daily_follow_end", "Understood. I'll get to it.");
		dailyThanksKey = text("option", "daily_thanks", "The pleasure was mine.");

		chats();
		quests();
		trades();
	}

	private void chats() {
		prefix("alice/chat");
		defaultDialog(GLEntities.ALICE.get(),
				"Oh - a visitor. Do come in; the Magical Forest is loud enough already.",
				"...Forgive me. I had not expected company today.",
				"Is there something you'd like made?");
		// home-bound: the reply below is about living out here, which is not what
		// a guest standing on someone else's doorstep should say
		starter("alice/chat", new DialogStarter(GLEntities.ALICE.get(), List.of(homeBound()),
				starterText("start", "You live out here all by yourself?"),
				dialog("hi", "All by myself? Not quite. The forest keeps me company, and the dolls keep me busy. I live here in the Magical Forest - a quiet stretch of it, far from the noise. Was there something you needed?",
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
				starterText("start", "How does a doll of yours actually fight?"),
				dialog("talk", "Badly, on its own. Cloth and straw will not throw a punch. The lance I gave you is the simplest thing I can put in a doll's hand - it has to come to the fight, and it does. If you would rather keep your distance instead, a magician lives deeper in this forest and trades star wands: a focus that carries its will out to where it can be seen. I cannot make one; she is the only one I would trust with the work.",
						option("where", "Which magician?",
								dialog("where_ans", "The one with the house full of mushrooms and the roof that keeps exploding. She does not advertise, but she always has stock. Point a doll at what you want it to hit, and the wand will do the rest.",
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
				starterText("call", "Is she in today, do you think?"),
				dialog("call", "...She is at her own house, I am afraid. I came for the mushrooms - the large ones she keeps in the east room. I would not normally ask a stranger for leave to rifle through a magician's shelves, but you may know the terms on which one does that.",
						option("terms", "What terms?",
								dialog("terms_ans", "Bring something she wants, ask plainly, and take only what was offered. She is not unfriendly - she simply does not do business from behind her own door. You will find she is reasonable.",
										option("terms/end", "Then I shall ask her myself.")))),
				CHAT_MISC);

		chat("alice/visit_marisa_house_greet", GLEntities.ALICE.get(),
				List.of(visitingAt(GLStructureGen.MARISA_HOUSE)),
				starterText("greet", "This is all rather far from my workshop."),
				dialog("greet", "It is. I am not often away - the dolls do not sew themselves, and the mushrooms here are not the ones I want. Still. It is restful, being somewhere that is not mine to keep tidy.",
						option("greet/end", "Take your time.")),
				CHAT_MISC);
	}

	private void quests() {
		prefix("alice/first_doll");
		quest("alice/first_doll", new Quest(GLEntities.ALICE.get(), List.of(),
				questTitle("Doll Materials"), questDesc("Bring Alice the string, wool and mystical straw every doll is made of."),
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
				start("Are those dolls really handmade?",
						"Every one of them. Cloth, thread, straw - that is the whole of a doll, and all of it has to come from somewhere. My stores are empty. Bring me string, some wool, and a little of the mystical straw that grows under the canopy, and I will put something back in your hands.",
						"I'll gather the materials.", "Thank you. String, wool, mystical straw - I will have a doll finished before you can blink.", "I'll be right back.",
						"Why not just make them yourself?", "I could, if the forest handed over its thread. Bring me the raw cloth and the straw and this stops being tedious.", "As you like."),
				follow("What was it you needed again?",
						"String, wool, and mystical straw. The straw is the awkward one - it grows in patches beneath the trees.",
						"I'm still looking.", "Take your time. Break the broom grass by hand and it gives up the straw; cut it with shears and you would only get the grass back.", null),
				complete("I have everything you asked for.",
						"Let me see... ah, good. This thread is tight and this wool is soft - you have been careful with it.",
						"Here you go.", "Then take these, and a little payment for the trouble. Emeralds, since you will want something practical.", "Thank you.",
						"Some of it is still missing.", "No rush. Bring it all at once and I will start the moment you walk in.", "Understood.")
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
				start("What are the seven colors for, exactly?",
						"For the glove. Seven strands, seven colors - red, orange, yellow, green, cyan, blue, purple. Give me four of each dye and I can bind the whole set into a single piece. It is slow work, so I will not hand over anything half finished: the glove, a doll to put in it, and a lance to give it something to fight with. Worth the trip?",
						"Consider it done.", "Then it is settled. Four of each - red, orange, yellow, light green, cyan, light blue, purple. Take your time finding them.", "I'll be back.",
						"That's a lot of hunting for one glove.", "It is. But I would rather make one glove properly than ten carelessly. Do think it over.", "I'll think about it."),
				follow("Remind me of the seven colors.",
						"Red, orange, yellow, light green, cyan, light blue, purple - four of each. I only need the complete set, so spend the dyes carefully.",
						"Noted.", "Good. The forest is not short of colors. Patience, on the other hand, may run out.", null),
				complete("All seven colors, four of each.",
						"...Red. Orange. Yellow. Green. Cyan. Blue. Purple. Every one present.",
						"Here - your reward.",
						"The glove, a doll to match it, and a lance. A doll with an empty hand is a doll in danger - the lance is the least I can send her out with.",
						"Then I'll put them to good use.",
						"These aren't what you asked for.", "...Let me look again. Show me what you brought and I will tell you which are short.", "Very well.")
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

		daily("alice/daily_doll", "Doll Restock", "Bring Alice the cloth and straw for one day's dolls, iron for her spear heads, plus three dyes of her choosing.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_SEVEN_COLORS)), 60, 10, 150, 0, 0,
				"I have more orders than doll parts today. Bring me string, wool, some broom grass, iron for the spear heads - enough of it, and no more than I can use - and three dyes, any three you like, I am not particular. In return you may keep whatever I finish.",
				"Very well. String, wool, broom grass, iron, and three dyes.",
				"Then I shall make do with what I have.",
				"String, wool, broom grass, iron for the spear heads - I asked for six, and not one more than eight - and three dyes. Any three; I have no preference.",
				null, "Very good. Do not spend the dyes on anything else.",
				"I have what you asked for.", "Oh, good. Let me look at the colors you picked.",
				"Here.", "Then these are yours. A doll, and a lance for its hand - that is all she needs. Keep them close.",
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
