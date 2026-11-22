package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.rpg.action.CompleteQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.DialogAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.GiveMobEffectAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.StartQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.condition.HasQuestCompletedCondition;
import dev.xkmc.gensokyolegacy.content.rpg.condition.SelfReputationCondition;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.GroupDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.SimpleDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestRecurrence;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.*;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ExpReward;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ReputationReward;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLAdvGen;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.gensokyolegacy.util.DummyHolderGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@SuppressWarnings("SameParameterValue")
public class ReimuQDGen extends QuestDialogData {

	private static final int BAD_OMEN_DURATION = 12000;
	private static final int FORTUNE_DURATION = 24000;
	private static final String FORTUNE_KEY = "fortune";
	private static final int FROG_CHAT_COOLDOWN = 1200;
	private static final String FROG_KEY = "frog";

	private static final ResourceLocation QUEST_LOCAL_FOOD = GensokyoLegacy.loc("reimu/local_food");
	private static final ResourceLocation QUEST_HOSTILE_LOOT = GensokyoLegacy.loc("reimu/hostile_loot");
	public static final ResourceLocation QUEST_TALISMAN_MATERIALS = GensokyoLegacy.loc("reimu/talisman_materials");
	private static final ResourceLocation QUEST_ENDER_MATERIALS = GensokyoLegacy.loc("reimu/ender_materials");
	private static final ResourceLocation QUEST_OMINOUS_BANNER = GensokyoLegacy.loc("reimu/ominous_banner");
	private static final ResourceLocation QUEST_RAID = GensokyoLegacy.loc("reimu/raid");

	private final String byeKey;
	private final String leaveKey;
	private final String takeKey;
	private final String dailyGroupKey;
	private final String dailyStartKey;
	private final String dailyAcceptKey;
	private final String dailyFollowKey;
	private final String dailyFollowEndKey;
	private final String dailyHandoverKey;

	public ReimuQDGen() {
		prefix("reimu/shared");
		byeKey = text("option", "bye", "Bye!");
		leaveKey = text("option", "leave", "(Leave)");
		takeKey = text("option", "take_reward", "(Take the reward)");
		dailyGroupKey = text("option", "daily_group", "Daily Tasks");
		dailyStartKey = text("option", "daily_start", "What can I help?");
		dailyAcceptKey = text("option", "daily_accept", "I'll help.");
		dailyFollowKey = text("option", "daily_follow", "Could you go over the task again?");
		dailyFollowEndKey = text("option", "daily_follow_end", "I'm on it!");
		dailyHandoverKey = text("option", "daily_handover", "(Hand over the items)");

		chats();
		fortune();
		quests();
		dailyQuests();
		trades();
	}

	private void chats() {
		prefix("reimu/chat");
		defaultDialog(GLEntities.REIMU.get(),
				"Oh? What brings you to the shrine?",
				"What a coincidence - you can be found here too.",
				"A good meal is hard to come by. Oh, can I get this boxed up later?",
				"I've come to ask for ofuda!");

		visitChats();

		prefix("reimu/chat_shrine");
		chat("reimu/chat_shrine", GLEntities.REIMU.get(),
				List.of(homeBound(), new SelfReputationCondition(50, true)),
				starterText("start", "Where is this place?"),
				dialog("talk", "Oh, a visitor. This is the Hakurei Shrine — though if you came to worship, you'll have to wait a while. I still haven't figured out what's going on around here.", TALK_ANIMS,
						option("bye", "I see.")),
				CHAT_DEFAULT);

		prefix("reimu/chat_shrine_close");
		chat("reimu/chat_shrine_close", GLEntities.REIMU.get(),
				List.of(homeBound(), new SelfReputationCondition(50)),
				starterText("start", "Tell me about this shrine?"),
				dialog("talk", "This place is a bit out of the way, but its feng shui is one of a kind — after all, this is "
						+ "the border between Gensokyo and the outside world, or at least that's what Yukari says... "
						+ "But being so remote has a downside: hardly anyone comes to worship.", TALK_ANIMS,
						option("bye", "(I'll visit the shrine more often from now on)")),
				CHAT_MISC);

		prefix("reimu/chat_duty");
		chat("reimu/chat_duty", GLEntities.REIMU.get(),
				List.of(homeBound(), new SelfReputationCondition(100)),
				starterText("start", "What are the Hakurei shrine maiden's duties?"),
				dialog("talk", "To put it simply, I maintain the balance of Gensokyo. Big trouble may need me to step in, "
						+ "but most days are actually quite leisurely and peaceful.", THINK_ANIMS_ALL,
						option("bye", "That sounds like important work.")),
				CHAT_MISC);

		prefix("reimu/chat_guests");
		chat("reimu/chat_guests", GLEntities.REIMU.get(),
				List.of(homeBound(), new SelfReputationCondition(100)),
				starterText("start", "Who usually visits the shrine?"),
				dialog("talk", "Actually, not many guests come to the shrine — just the occasional acquaintance dropping by. "
						+ "It's very quiet... Whatever. Donations barely amount to anything anyway; it's good enough "
						+ "that anyone comes at all. At least it's lively.", THINK_ANIMS_ALL,
						option("bye", "I see.")),
				CHAT_MISC);

		prefix("reimu/chat_power");
		chat("reimu/chat_power", GLEntities.REIMU.get(),
				List.of(homeBound(), new SelfReputationCondition(100)),
				starterText("start", "What are your abilities?"),
				dialog("talk", "My abilities? Differences in strength mean nothing to me — gravity, force, that sort of "
						+ "thing — so I can fly freely and fight anyone on equal footing.", List.of("dialog_confused"),
						option("bye", "That's impressive.")),
				CHAT_MISC);

		prefix("reimu/chat_money");
		chat("reimu/chat_money", GLEntities.REIMU.get(),
				List.of(homeBound(), new SelfReputationCondition(100)),
				starterText("start", "What do you like most?"),
				dialog("talk", "I don't have anything I like that much. If I had to say, I like anything that can cover my "
						+ "living needs.", List.of("dialog_decline"),
						option("bye", "I see.")),
				CHAT_MISC);

		prefix("reimu/chat_frog");
		// Starts the cooldown on the first click inside the chat, the way the
		// fortune chat only spends its timer when the stick is drawn.
		chat("reimu/chat_frog", GLEntities.REIMU.get(),
				List.of(homeBound(), hasItem(item(GLItems.STRAW_HAT.get(), 1)), hasQuest(QUEST_OMINOUS_BANNER), timer(FROG_KEY)),
				starterText("start", "About this straw hat..."),
				dialog("talk", "There's the aura of a native god on this hat.", THINK_ANIMS_ALL,
						option("ask", "I bought it back then because it looked unusual — is there something special about "
								+ "it?",
								setTimer(FROG_KEY, FROG_CHAT_COOLDOWN),
								dialog("idea", "Have you heard the legend of the native god? It is said to appear in the "
										+ "form of a frog, guarding the village the way a frog catches prey, swallowing "
										+ "only those who do evil... But there's no way to verify it; it may just be a "
										+ "folk tale.", TALK_ANIMS,
										option("bye", "That's awfully detailed for a story.")))),
				CHAT_SPECIAL);

		prefix("reimu/chat_marisa");
		chat("reimu/chat_marisa", GLEntities.REIMU.get(),
				List.of(homeBound(), missingAdv(GLAdvGen.ENTER_MARISA_HOUSE), new SelfReputationCondition(50)),
				starterText("start", "Which works better, ofuda or potions?"),
				dialog("talk", "Ofuda borrow power from the gods, so they usually come with strings attached. Speaking of "
						+ "potions — have you met Marisa? She runs a shop deep in the Magical Forest. Her potions work "
						+ "differently from the usual brewing — go take a look if you're curious.", TALK_ANIMS,
						option("where", "Where can I find her?",
								dialog("where_ans", "Her house is deep in the Magical Forest — follow the mushrooms and the explosions, you can't miss her.", List.of("dialog_talk_01", "dialog_talk_04", "dialog_talk_05"),
										option("bye", "Thanks for the directions!")))),
				CHAT_INFO);
	}

	/**
	 * Reimu out visiting. Kourindou is her only host so far, so the set is written for that house
	 * rather than left host-agnostic, because a guest says different things in different rooms.
	 * {@code visitingAt} implies she is a guest at all, so nothing here surfaces on her own doorstep.
	 */
	private void visitChats() {
		prefix("reimu/visit_kourindou");
		chat("reimu/visit_kourindou_stock", GLEntities.REIMU.get(),
				List.of(visitingAt(GLStructureGen.MORICHIKA_SHOP)),
				starterText("stock", "This shop has a good deal more in stock than it looks."),
				dialog("stock", "It's a fairly large secondhand shop, after all — but the goods he sells are pretty unusual.", TALK_ANIMS,
						option("browse", "Anything worth buying?",
								dialog("browse_ans", "Hard to say. Most of it is no use to me... and sometimes the shopkeeper doesn't know how to use his own tools either.", THINK_ANIMS_ALL,
										option("browse/end", "That sounds... unsettling.")))),
				CHAT_MISC);

		chat("reimu/visit_kourindou_stay", GLEntities.REIMU.get(),
				List.of(visitingAt(GLStructureGen.MORICHIKA_SHOP)),
				starterText("stay", "What a coincidence - you are shopping here too?"),
				dialog("stay", "Oh? It's you. I have some things to buy - a good deal of what the shrine uses is actually sourced here.", TALK_ANIMS,
						option("bye", "I see.")),
				CHAT_DEFAULT);
	}

	private void fortune() {
		prefix("reimu/chat_fortune");
		var good = dialog("good",
				"Great blessing! Even I'm jealous. Take this luck — why not try opening a treasure chest?", TALK_ANIMS,
				option("good_bye", "(Take the fortune slip)"));
		var mid = dialog("mid",
				"Small blessing — nothing special. They say heaven rewards the diligent, so don't waste your "
						+ "time.", List.of("dialog_think"),
				option("mid_bye", "(Take the fortune slip)"));
		var bad = dialog("bad",
				"Ah, a bad slip... try to visit fewer people today and keep a low profile.", THINK_ANIMS_ALL,
				option("bad_bye", "No way..."));
		chat("reimu/chat_fortune", GLEntities.REIMU.get(),
				List.of(homeBound(), hasQuest(QUEST_LOCAL_FOOD), timer(FORTUNE_KEY)),
				starterText("start", "[Fortune Slip]"),
				dialog("talk",
						"You can draw a fortune slip here once a day. It will show your fortune — want to "
								+ "take a look?", TALK_ANIMS,
						randomOption("draw", "Draw a fortune.",
								weighted(2, good,
										new GiveMobEffectAction(GLEffects.LOOTING, FORTUNE_DURATION, 0),
										setTimer(FORTUNE_KEY, FORTUNE_DURATION)),
								weighted(2, mid,
										new GiveMobEffectAction(MobEffects.DIG_SPEED, FORTUNE_DURATION, 0),
										setTimer(FORTUNE_KEY, FORTUNE_DURATION)),
								weighted(1, bad,
										new GiveMobEffectAction(MobEffects.BAD_OMEN, FORTUNE_DURATION, 0),
										setTimer(FORTUNE_KEY, FORTUNE_DURATION)))),
				CHAT_INFO);
	}

	private void quests() {
		prefix("reimu/local_food");
		questAnims("start", TALK_ANIMS, "dialog_decline", TALK_ANIMS);
		questAnims("follow", THINK_ANIMS_ALL, "dialog_decline");
		questAnims("complete", "dialog_agree", "dialog_talk_05", "dialog_agree");
		quest("reimu/local_food", new Quest(GLEntities.REIMU.get(), List.of(),
				questTitle("Shrine Provisions"), questDesc("Bring Reimu some bread and mushroom stew so she can learn this world's food."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-food", new SubmitItemRequirement(List.of(
								item(Items.BREAD, 8),
								item(Items.MUSHROOM_STEW, 3)))
				)),
				List.of(new ExpReward(50), new ReputationReward(10, 300, 10, 300)),
				start("Is the food here to your liking?",
						"Let's not talk about whether I like it — I don't even know what's edible around "
								+ "here. The shrine's stockpiled rations are almost gone. Could you bring me some food?",
						"I'll get some food ready.", "Thanks for the generous aid. I hope you can get it ready before I faint from hunger.", "Hang in there!",
						"Maybe next time.", "Ah — is that so. Never mind, the shrine's rations should last me a while. I'll figure out what's edible around here myself.", "(Leave)"),
				follow("What do you like to eat?",
						"Marisa always shares mushroom dishes at feasts — I kind of miss that taste. Are "
								+ "there any edible mushrooms around here? How about mushroom stew and bread?",
						"That shouldn't be hard to find.", "The rations should last a while longer... though they taste awful. I'll wait for good news from you.", "(Leave)"),
				complete("I'm back.",
						"You're back. How did it go?",
						"Brought it.", "Not bad, that was quick. Here, your reward.", "(Take the reward)")
		));

		prefix("reimu/hostile_loot");
		questAnims("start", TALK_ANIMS, TALK_ANIMS, "dialog_agree");
		questAnims("follow", TALK_ANIMS, "dialog_confused");
		questAnims("complete", TALK_ANIMS, "dialog_decline", "dialog_agree");
		quest("reimu/hostile_loot", new Quest(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_LOCAL_FOOD)),
				questTitle("Hostile Analysis"), questDesc("Clear out zombies and skeletons for Reimu and collect their flesh and bones."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-zombie", new KillMobRequirement(reqText("zombie", "Exterminate zombies"), EntityTypeTags.ZOMBIES, 10),
						"b-skeleton", new KillMobRequirement(reqText("skeleton", "Exterminate skeletons"), EntityTypeTags.SKELETONS, 10),
						"c-loot", new SubmitItemRequirement(List.of(
								item(Items.ROTTEN_FLESH, 8),
								item(Items.BONE, 8)))
				)),
				List.of(new ExpReward(100), new ReputationReward(20, 300, 10, 300)),
				start("You look worried — something on your mind?",
						"Speaking of which, those monsters wandering outside don't look like youkai, and "
								+ "sweeping them up is boring work. If you help drive some off, I can give you a reward "
								+ "— how about it, will you help?",
						"Sure.", "Alright, I'm counting on you. Don't mess it up — if you do, I'm not cleaning up after you— Just kidding. Good luck.", "(Leave)",
						"No.", "Ah — is that so? Never mind if you don't want to.", "(Leave)"),
				follow("(Check clearing progress)",
						"How's the monster hunting going?",
						"Not done yet.", "Oh — I see. While you're driving them off, bring back some of their materials. I want to see what kind of beings they are, so I can work out a better way to deal with them.", "Got it."),
				complete("I'm back.",
						"Oh, you're back. How did it go?",
						"Done. Here are the materials.", "That was quick. Guess you're no pushover. You didn't get hurt, did you? I wonder how the zombies here differ from the ones I know... With these I can confirm properly.", "(Take the reward)")
		));

		prefix("reimu/talisman_materials");
		questAnims("start", TALK_ANIMS, TALK_ANIMS, TALK_ANIMS);
		questAnims("follow", TALK_ANIMS, TALK_ANIMS);
		questAnims("complete", TALK_ANIMS, "dialog_decline", "dialog_agree");
		quest("reimu/talisman_materials", new Quest(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_HOSTILE_LOOT)),
				questTitle("Talisman Materials"), questDesc("Bring Reimu paper and redstone for her new-world talismans."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-supplies", new SubmitItemRequirement(List.of(
								item(Items.PAPER, 16),
								item(Items.REDSTONE, 8)))
				)),
				List.of(new ExpReward(100), new ReputationReward(10, 300, 0, 300),
						loot("reimu/talisman_materials", LootTable.lootTable()
								.withPool(lootItem(GLTalismans.TALISMAN_POCKET.get(), 1))
								.withPool(lootItem(GLTalismans.HEAL_TALISMAN.get(), 2)))),
				start("What are these ofuda you mentioned?",
						"You can't exterminate youkai without ofuda. I bet you've run into some nasty youkai on your travels — blades work, sure, but ofuda drive them off even better. The materials aren't too picky — redstone dust will do. I'm just running short. Wish I had a helper.",
						"I'll gather the materials for you.", "Alright, I'll leave it to you. Once the materials are gathered, I'll draw some ofuda "
								+ "as a reward.", "(Leave)",
						"If it's not picky, why not red dye?", "Because it takes natural ore to commune with the gods. The best ofuda are dotted with cinnabar — dye just won't cut it.", "That sounds pretty picky."),
				follow("(Check gathering progress)",
						"You're back. How's the progress?",
						"Still gathering.", "Then I'll wait for good news.", "(Take my leave)"),
				complete("I'm back.",
						"You're back. Got all the materials?",
						"Here's what you asked for.", "So reliable. With these the shrine's ofuda stock is much fuller... Don't worry, I haven't forgotten your share. Here, take these.", "(Take the reward)")
		));

		prefix("reimu/ender_materials");
		questAnims("start", THINK_ANIMS_ALL, "dialog_decline", "dialog_agree");
		questAnims("follow", TALK_ANIMS, TALK_ANIMS);
		questAnims("complete", "dialog_agree", "dialog_decline", TALK_ANIMS);
		quest("reimu/ender_materials", new Quest(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				questTitle("Ender Materials"), questDesc("Bring Reimu an ender pearl and an ender eye for her gap research."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-materials", new SubmitItemRequirement(List.of(
								item(Items.ENDER_PEARL, 1),
								item(Items.ENDER_EYE, 1)))
				)),
				List.of(new ExpReward(150), new ReputationReward(10, 300, 0, 300)),
				start("The village says you arrive almost instantly when exterminating — how do you do it?",
						"I travel through gaps, anchoring two places to each other. Bring me the medium it "
								+ "needs — I can teach you how to use it too.",
						"I'll go look for them.", "One pearl and one eye, then. They're not easy for me to get — danmaku doesn't do much against them.", "(Leave)",
						"I've got other things to do.", "No worries. We can talk when you're free.", "(Leave)"),
				follow("(Check gathering progress)",
						"Found any ender materials yet?",
						"Still looking.", "I hear they're common in a place called the warped forest. Give it a try.", "(Leave)"),
				complete("I'm back.",
						"Did you get them?",
						"The pearl and eye you asked for", "Once placed, this gate will produce a linked gate in your hand. Place it elsewhere "
								+ "and it's done.", "(Take the reward)")
		));

		prefix("reimu/ominous_banner");
		questAnims("start", THINK_ANIMS_ALL, TALK_ANIMS, TALK_ANIMS);
		questAnims("follow", TALK_ANIMS, TALK_ANIMS);
		questAnims("complete", "dialog_agree", "dialog_decline", "dialog_agree");
		quest("reimu/ominous_banner", new Quest(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_HOSTILE_LOOT)),
				questTitle("Raider's Banner"), questDesc("Bring Reimu an ominous banner from a raid captain."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-banner", new SubmitItemRequirement(List.of(new IngredientEntry(
								DataComponentIngredient.of(false, Raid.getLeaderBannerInstance(DummyHolderGetter.create())), 1, Optional.of("Ominous Banner"))))
				)),
				List.of(new ExpReward(200), new ReputationReward(20, 300, 10, 300),
						loot("reimu/ominous_banner", LootTable.lootTable()
								.withPool(lootItem(GLTalismans.SHELTER_TALISMAN.get(), 2)))),
				start("Is it true what the raiders preach about defiling souls?",
						"I've exterminated them before — nothing special. They're upsetting the balance. If you're willing, bring me one of their banners. I don't think it's just a symbol.",
						"I'll go get one.", "Good. Bring the banner back, and be careful.", "I'll be careful.",
						"Maybe when I have time.", "Fine. They're just heretics anyway — I can handle them myself.", "(Leave)"),
				follow("(Check gathering progress)",
						"Found the banner yet?",
						"Not yet.", "Check outposts and patrols. That's where you're most likely to find one.", "Got it."),
				complete("I'm back.",
						"Got it? Let me see the banner.",
						"Here's the banner.", "That's it. I'll study the calamity behind these raiders, and put an end to their balance-breaking.", "(Leave)")
		));

		prefix("reimu/raid");
		questAnims("start", TALK_ANIMS, "dialog_confused", TALK_ANIMS);
		questAnims("follow", TALK_ANIMS, TALK_ANIMS);
		quest("reimu/raid", new Quest(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_OMINOUS_BANNER)),
				questTitle("Repel the Raid"), questDesc("Enter a village carrying Reimu's mark and repel the raiders."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-raid", new RaidVictoryRequirement(reqText("raid", "Win a raid"), 1)
				)),
				List.of(new ExpReward(400), new ReputationReward(30, 300, 20, 300),
						loot("reimu/raid", LootTable.lootTable()
								.withPool(lootItem(GLItems.BORDER_UMBRELLA.get(), 1)))),
				startRaidEx("What's your plan for the raiders attacking villages lately?",
						"I've figured out that ominous mark. My ofuda can recreate it. I want to put it on you — can you draw the raiders out and drive them away?",
						"I'll repel the raid.", "There. The mark is on you. Draw them out and destroy this balance-breaking force.", "(Leave)",
						"Isn't that too risky?", "I think you're strong enough. If you're not confident, forget it.", "(Leave)"),
				followEx("(Reapply the mark)",
						"The mark is fading. Want another? I can set a fresh one.",
						"Please set the mark again.", "There. Enter the village, draw them out — you know what to do.", "(Leave)",
						new GiveMobEffectAction(MobEffects.BAD_OMEN, BAD_OMEN_DURATION, 0)),
				completeSingle("The raid is over.",
						"They stopped coming? Good — that saves a lot of work. Here — take this border umbrella, and be ready for what's coming.", "(Take the reward)")
		));
	}

	private void dailyQuests() {
		prefix("reimu/daily_food");
		questAnims("dailyEx", TALK_ANIMS, "dialog_decline", TALK_ANIMS, TALK_ANIMS, TALK_ANIMS, TALK_ANIMS, "dialog_agree");
		var foodTable = requestTable("daily_food", LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(2))
						.add(LootItem.lootTableItem(Items.WHITE_WOOL).apply(SetItemCountFunction.setCount(UniformGenerator.between(6, 8))))
						.add(LootItem.lootTableItem(Items.LEATHER).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 6))))
						.add(LootItem.lootTableItem(Items.IRON_INGOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(6, 8))))
						.add(LootItem.lootTableItem(Items.GOLD_INGOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 4))))
						.add(LootItem.lootTableItem(Items.BREAD).apply(SetItemCountFunction.setCount(UniformGenerator.between(6, 8))))
						.add(LootItem.lootTableItem(Items.APPLE).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 6))))));
		dailyEx("reimu/daily_food", "Shrine Provisions", "Bring Reimu a few supplies for the shrine.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_LOCAL_FOOD)), 60, 10, 150, 0, 150,
				"The offering box is bone dry — at this rate I won't eat. Bring me a few things the shrine can use.",
				"I have other things to do.", "Great. I need these things.", "Then I'll think of something — maybe mooch a meal off her.",
				"The donation box is empty again — bring a decent haul of supplies, remember.",
				"Okay.", "Don't forget.", "(Leave)",
				"I've got the goods.", "Oh, you got 'em? Let me see!", "Just what was needed. Thanks.",
				new TreeMap<>(Map.of(
						"a-supplies", rollItem(foodTable))),
				LootTable.lootTable().withPool(lootItem(GLItems.COIN.get(), 1)));

		prefix("reimu/daily_hunt");
		questAnims("dailyEx", TALK_ANIMS, THINK_ANIMS_ALL, "dialog_agree", THINK_ANIMS_ALL, TALK_ANIMS, "dialog_agree", TALK_ANIMS);
		var huntTable = requestTable("daily_hunt", LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 8))))
						.add(LootItem.lootTableItem(Items.BONE).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 8))))
						.add(LootItem.lootTableItem(Items.GUNPOWDER).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))
						.add(LootItem.lootTableItem(Items.SPIDER_EYE).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))));
		quest("reimu/daily_hunt", new Quest(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_HOSTILE_LOOT)),
				questTitle("Monster Thinning"), questDesc("Thin out the monsters and bring Reimu some loot."),
				Optional.of(new QuestRecurrence(24000)),
				new TreeMap<>(Map.of(
						"a-kill", new KillEnemyRequirement(reqText("kill", "Kill hostile mobs"), 12),
						"b-loot", rollItem(huntTable))),
				List.of(new ExpReward(80), new ReputationReward(10, 150, 5, 120),
						loot("reimu/daily_hunt", LootTable.lootTable().withPool(lootItem(GLItems.COIN.get(), 3)))),
				groupKey(dailyGroupKey, dailyStartKey,
						dialog("start/dialog_1", "Something's always stirring trouble near the shrine. Clear out those monsters and bring back their loot.", TALK_ANIMS,
								option("start/reject", "I have other things to do.",
										dialog("start/reject/dialog_1", "Ugh, looks like I'll have to fight them myself.", List.of("dialog_think", "dialog_think_02"), option("start/reject/end", "(Leave)"))),
								option("start/accept", "I'll handle it.", new StartQuestAction(),
										dialog("start/accept/dialog_1", "Good, go clear them out.", List.of("dialog_agree"), option("start/accept/end", "(Leave)"))))),
				dailyFollowEx("Something's been stirring near the shrine again — thin them out and bring back their loot, remember.",
						"Okay.", "Be careful out there.", "(Leave)"),
				dailyCompleteEx("I've got the goods.", "Oh, you got 'em? Let me see!",
						"Finally, I can get a peaceful night's sleep.")
		));

		prefix("reimu/daily_talisman");
		questAnims("dailyEx", TALK_ANIMS, "dialog_decline", "dialog_agree", TALK_ANIMS, TALK_ANIMS, "dialog_agree", TALK_ANIMS);
		dailyEx("reimu/daily_talisman", "Talisman Restock", "Bring Reimu paper and redstone to restock her talismans.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)), 60, 10, 150, 0, 150,
				"Ofuda stock is running low again — paper and redstone, same as before. Gather some for me?",
				"Forget it.", "Good. Restock me and I'll keep a steady supply of ofuda.",
				"There's still a little left. We'll talk when you're free.",
				"Ofuda stock is low again — paper and redstone, remember.",
				"Got it.", "The stock is almost empty.", "I'll be right back.",
				"I've got the goods.", "Let me see.", "Good. That'll last a while. Here's your share.",
				new TreeMap<>(Map.of(
						"a-supplies", new SubmitItemRequirement(List.of(
								item(Items.PAPER, 16),
								item(Items.REDSTONE, 8))))),
				LootTable.lootTable().withPool(lootItem(GLTalismans.HEAL_TALISMAN.get(), 2)));

		prefix("reimu/daily_raid");
		questAnims("dailyEx", TALK_ANIMS, "dialog_decline", "dialog_agree", TALK_ANIMS, "dialog_agree", "dialog_agree", null);
		quest("reimu/daily_raid", new Quest(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_RAID)),
				questTitle("Raid Defense"), questDesc("Win a raid with Reimu's mark to protect a village."),
				Optional.of(new QuestRecurrence(24000)),
				new TreeMap<>(Map.of(
						"a-raid", new RaidVictoryRequirement(reqText("raid", "Win a raid"), 1)
				)),
				List.of(new ExpReward(200), new ReputationReward(20, 200, 10, 150),
						loot("reimu/daily_raid", LootTable.lootTable()
								.withPool(lootItem(GLTalismans.SHELTER_TALISMAN.get(), 2)))),
				groupKey(dailyGroupKey, dailyStartKey,
						dialog("start/dialog_1", "Another village has caught the eye of a raider band. I've got the bait ready — up "
								+ "for another cleanup?", TALK_ANIMS,
								option("start/reject", "Not ready yet.",
										dialog("start/reject/dialog_1", "The mark will keep. We'll talk when you're ready.", List.of("dialog_decline"), option("start/reject/end", "(Leave)"))),
								option("start/accept", "I'll help.",
										List.of(new StartQuestAction(), new GiveMobEffectAction(MobEffects.BAD_OMEN, BAD_OMEN_DURATION, 0)),
										dialog("start/accept/dialog_1", "Same as before — lure them in, hold the line, drive them out.", List.of("dialog_agree"), option("start/accept/end", "(Leave)"))))),
				groupOption(dailyGroupKey, "follow_up", "My mark seems gone?",
						dialog("follow_up/dialog_1", "Is that so? I can set a fresh one on you.", TALK_ANIMS,
								option("follow_up/end", "Please do.", new GiveMobEffectAction(MobEffects.BAD_OMEN, BAD_OMEN_DURATION, 0),
										dialog("follow_up/end/dialog_1", "There. Same plan — I'll wait for good news.", List.of("dialog_agree"), option("follow_up/end/leave", "(Leave)"))))),
				option("complete", "The cleanup went well.",
						dialog("complete/dialog_1", "Not bad — you're getting better at this.", AGREE_ANIMS,
								option("complete/handover", "I couldn't have drawn them out so smoothly without your help.", new CompleteQuestAction())))
		));
	}

	private void trades() {
		prefix("reimu");
		trade("gap_portal", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_ENDER_MATERIALS)),
				new ItemStack(GLBlocks.GAP_PORTAL.get()),
				new TradeRecurrence(1, 6000),
				List.of(
						item(Items.PURPLE_WOOL, 2),
						item(Items.ENDER_EYE, 2),
						item(Items.ENDER_PEARL, 2),
						item(Items.CRYING_OBSIDIAN, 2))));
		trade("sell_bread", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_LOCAL_FOOD)),
				new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(1, 24000),
				List.of(item(Items.BREAD, 16))));
		trade("sell_chicken", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_LOCAL_FOOD)),
				new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(1, 24000),
				List.of(item(Items.COOKED_CHICKEN, 4))));
		trade("sell_string", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_LOCAL_FOOD)),
				new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(4, 96000),
				List.of(item(Items.STRING, 6))));
		trade("sell_wool", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_LOCAL_FOOD)),
				new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(4, 96000),
				List.of(itemTag(ItemTags.WOOL, 6))));
		trade("sell_paper", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(16, 24000),
				List.of(item(Items.PAPER, 12))));
		trade("sell_redstone", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(16, 24000),
				List.of(item(Items.REDSTONE, 4))));
		trade("sell_gunpowder", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(4, 24000),
				List.of(item(Items.GUNPOWDER, 4))));
		trade("offer_heal_talisman", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.HEAL_TALISMAN.get()),
				new TradeRecurrence(16, 24000), List.of(item(GLItems.COIN.get(), 8))));
		trade("offer_shelter_talisman", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.SHELTER_TALISMAN.get()),
				new TradeRecurrence(16, 24000), List.of(item(GLItems.COIN.get(), 8))));
		trade("offer_speed_talisman", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.SPEED_TALISMAN.get()),
				new TradeRecurrence(16, 24000), List.of(item(GLItems.COIN.get(), 4))));
		trade("offer_hydrophobic_talisman", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.HYDROPHOBIC_TALISMAN.get()),
				new TradeRecurrence(16, 24000), List.of(item(GLItems.COIN.get(), 4))));
		trade("offer_lava_talisman", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.LAVA_TALISMAN.get()),
				new TradeRecurrence(16, 24000), List.of(item(GLItems.COIN.get(), 4))));
		trade("offer_talisman_pocket", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.TALISMAN_POCKET.get()),
				new TradeRecurrence(4, 24000), List.of(item(GLItems.COIN.get(), 16))));
		// She only parts with her own tea once you are more than a stranger: strictly above 80.
		trade("offer_lucky_tea", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new SelfReputationCondition(81)),
				new ItemStack(GLItems.LUCKY_TEA.get()),
				new TradeRecurrence(16, 24000), List.of(item(GLItems.COIN.get(), 16))));
		trade("offer_border_umbrella", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_RAID)),
				new ItemStack(GLItems.BORDER_UMBRELLA.get()),
				new TradeRecurrence(1, 168000), List.of(item(GLItems.COIN.get(), 48))));
	}

	private SimpleDialogOption start(String button, String intro,
									 String accept, String acceptLine, String acceptEnd,
									 String reject, String rejectLine, String rejectEnd) {
		return option("start", button,
				dialog("start/dialog_1", intro, anims("start", 0, THINK_ANIMS),
						option("start/reject", reject, dialog("start/reject/dialog_1", rejectLine,
								anims("start", 1, List.of()), option("start/reject/end", rejectEnd))),
						option("start/accept", accept, new StartQuestAction(),
								dialog("start/accept/dialog_1", acceptLine,
										anims("start", 2, List.of()), option("start/accept/end", acceptEnd)))));
	}

	private SimpleDialogOption startRaidEx(String button, String intro,
										   String accept, String acceptLine, String acceptEnd,
										   String reject, String rejectLine, String rejectEnd) {
		return option("start", button,
				dialog("start/dialog_1", intro, anims("start", 0, THINK_ANIMS),
						option("start/reject", reject, dialog("start/reject/dialog_1", rejectLine,
								anims("start", 1, List.of()), option("start/reject/end", rejectEnd))),
						option("start/accept", accept, List.of(new StartQuestAction(), new GiveMobEffectAction(MobEffects.BAD_OMEN, BAD_OMEN_DURATION, 0)),
								dialog("start/accept/dialog_1", acceptLine,
										anims("start", 2, List.of()), option("start/accept/end", acceptEnd)))));
	}

	private SimpleDialogOption follow(String button, String intro, String opt, String optLine, String optEnd) {
		return option("follow_up", button,
				dialog("follow_up/dialog_1", intro, anims("follow", 0, THINK_ANIMS),
						option("follow_up/end", opt, dialog("follow_up/end/dialog_1", optLine,
								anims("follow", 1, List.of()), option("follow_up/end/leave", optEnd)))));
	}

	private SimpleDialogOption followEx(String button, String intro, String opt, String optLine, String optEnd, DialogAction<?> action) {
		return option("follow_up", button,
				dialog("follow_up/dialog_1", intro, anims("follow", 0, THINK_ANIMS),
						option("follow_up/end", opt, List.of(action),
								dialog("follow_up/end/dialog_1", optLine,
										anims("follow", 1, List.of()), option("follow_up/end/leave", optEnd)))));
	}

	private SimpleDialogOption complete(String button, String intro,
										String complete, String completeLine, String completeEnd) {
		return option("complete", button,
				dialog("complete/dialog_1", intro, anims("complete", 0, AGREE_ANIMS),
						option("complete/handover", complete, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine,
										anims("complete", 2, List.of()), option("complete/handover/end", completeEnd)))));
	}

	private SimpleDialogOption complete(String button, String intro,
										String reject, String rejectLine, String rejectEnd,
										String complete, String completeLine, String completeEnd) {
		return option("complete", button,
				dialog("complete/dialog_1", intro, anims("complete", 0, AGREE_ANIMS),
						option("complete/reject", reject, dialog("complete/reject/dialog_1", rejectLine,
								anims("complete", 1, List.of()), option("complete/reject/end", rejectEnd))),
						option("complete/handover", complete, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine,
										anims("complete", 2, List.of()), option("complete/handover/end", completeEnd)))));
	}

	private SimpleDialogOption completeSingle(String button, String intro, String takeEnd) {
		return option("complete", button,
				dialog("complete/dialog_1", intro, AGREE_ANIMS,
						option("complete/handover", takeEnd, new CompleteQuestAction())));
	}

	private void dailyEx(String id, String title, String desc, QuestRecurrence rec,
						 List<QuestCondition<?>> conditions, int exp, int rep, int softCap, int capIncrease, int maxCap,
						 String intro, String reject, String acceptLine, String rejectLine, String followLine,
						 String followOpt, String optLine, String optEnd,
						 String complete, String gotem, String completeLine,
						 Map<String, QuestRequirement<?, ?>> reqs, LootTable.Builder loot) {
		quest(id, new Quest(GLEntities.REIMU.get(), conditions,
				questTitle(title), questDesc(desc),
				Optional.of(rec),
				new TreeMap<>(reqs),
				List.of(new ExpReward(exp), new ReputationReward(rep, softCap, capIncrease, maxCap),
						loot(id, loot)),
				groupKey(dailyGroupKey, dailyStartKey,
						dialog("start/dialog_1", intro, anims("dailyEx", 0, THINK_ANIMS),
								groupOption(dailyGroupKey, "start/reject", reject,
										dialog("start/reject/dialog_1", rejectLine,
												anims("dailyEx", 1, List.of()), option("start/reject/end", "(Leave)"))),
								groupKey(dailyGroupKey, dailyAcceptKey, new StartQuestAction(),
										dialog("start/accept/dialog_1", acceptLine,
												anims("dailyEx", 2, List.of()), option("start/accept/end", "(Leave)"))))),
				dailyFollowEx(followLine, followOpt, optLine, optEnd),
				dailyCompleteEx(complete, gotem, completeLine)));
	}

	private GroupDialogOption dailyFollowEx(String followLine, String followOpt, String optLine, String optEnd) {
		return groupKey(dailyGroupKey, dailyFollowKey,
				dialog("follow_up/dialog_1", followLine, anims("dailyEx", 3, THINK_ANIMS),
						option("follow_up/end", followOpt,
								dialog("follow_up/end/dialog_1", optLine,
										anims("dailyEx", 4, List.of()), option("follow_up/end/leave", optEnd)))));
	}

	private GroupDialogOption dailyCompleteEx(String complete, String gotem, String completeLine) {
		return groupOption(dailyGroupKey, "complete", complete,
				dialog("complete/dialog_1", gotem, anims("dailyEx", 5, AGREE_ANIMS),
						optionKey(dailyHandoverKey, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine,
										anims("dailyEx", 6, List.of()), option("complete/handover/end", "(Take the reward)")))));
	}

}