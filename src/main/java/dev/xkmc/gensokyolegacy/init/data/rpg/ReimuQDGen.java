package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.rpg.action.CompleteQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.DialogAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.GiveMobEffectAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.StartQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.condition.HasQuestCompletedCondition;
import dev.xkmc.gensokyolegacy.content.rpg.condition.SelfReputationCondition;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.SimpleDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestRecurrence;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.KillEnemyRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.KillMobRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.QuestRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.RaidVictoryRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.SubmitItemRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ExpReward;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ReputationReward;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLAdvGen;
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

	private static final ResourceLocation QUEST_LOCAL_FOOD = GensokyoLegacy.loc("reimu/local_food");
	private static final ResourceLocation QUEST_HOSTILE_LOOT = GensokyoLegacy.loc("reimu/hostile_loot");
	public static final ResourceLocation QUEST_TALISMAN_MATERIALS = GensokyoLegacy.loc("reimu/talisman_materials");
	private static final ResourceLocation QUEST_ENDER_MATERIALS = GensokyoLegacy.loc("reimu/ender_materials");
	private static final ResourceLocation QUEST_OMINOUS_BANNER = GensokyoLegacy.loc("reimu/ominous_banner");
	private static final ResourceLocation QUEST_RAID = GensokyoLegacy.loc("reimu/raid");

	private final String byeKey;
	private final String dailyStartKey;
	private final String dailyAcceptKey;
	private final String dailyRejectKey;
	private final String dailyFollowKey;
	private final String dailyFollowEndKey;
	private final String dailyCompleteKey;
	private final String dailyHandoverKey;
	private final String dailyGotemKey;

	public ReimuQDGen() {
		prefix("reimu/shared");
		byeKey = text("option", "bye", "Bye!");
		dailyStartKey = text("option", "daily_start", "What can I help?");
		dailyAcceptKey = text("option", "daily_accept", "I'll do it!");
		dailyRejectKey = text("option", "daily_reject", "Maybe later.");
		dailyFollowKey = text("option", "daily_follow", "Could you go over the task again?");
		dailyFollowEndKey = text("option", "daily_follow_end", "I'm on it!");
		dailyCompleteKey = text("option", "daily_complete", "I've got the goods!");
		dailyHandoverKey = text("option", "daily_handover", "Here you go!");
		dailyGotemKey = text("dialog", "daily_gotem", "Oh, you got 'em? Let me see!");

		chats();
		fortune();
		quests();
		dailyQuests();
		trades();
	}

	private void chats() {
		prefix("reimu/chat");
		defaultDialog(GLEntities.REIMU.get(),
				"Hi! What brings you to the shrine?",
				"I'd like to trade with you!");

		prefix("reimu/chat_shrine");
		chat("reimu/chat_shrine", GLEntities.REIMU.get(),
				List.of(new SelfReputationCondition(50, true)),
				starterText("start", "Where is this place?"),
				dialog("talk", "Oh, a visitor. This is the Hakurei Shrine — though if you came to worship, you'll have to wait a while. I still haven't figured out what's going on around here.",
						option("bye", "Got it, bye!")),
				CHAT_DEFAULT);

		prefix("reimu/chat_shrine_close");
		chat("reimu/chat_shrine_close", GLEntities.REIMU.get(),
				List.of(new SelfReputationCondition(50)),
				starterText("start", "Tell me more about the shrine."),
				dialog("talk", "This shrine sits in the middle of nowhere, but back in Gensokyo its fusui was one of a kind. After all, this place is the border between Gensokyo and the outside world — at least, that's what Yukari says... But being this remote has a downside: hardly anyone comes to worship, and lately not even the youkai show up... What am I supposed to do? Moving isn't an option.",
						option("bye", "I'll keep visiting.")),
				CHAT_MISC);

		prefix("reimu/chat_duty");
		chat("reimu/chat_duty", GLEntities.REIMU.get(),
				List.of(new SelfReputationCondition(100)),
				starterText("start", "What are your duties, exactly?"),
				dialog("talk", "Back in Gensokyo, my job was simple, really — keeping Gensokyo in order. Put bluntly: wherever trouble broke out, I'd rush over and beat up whoever started it... Sounds exhausting, right? And it's year-round with no pay.",
						option("bye", "I see.")),
				CHAT_MISC);

		prefix("reimu/chat_guests");
		chat("reimu/chat_guests", GLEntities.REIMU.get(),
				List.of(new SelfReputationCondition(100)),
				starterText("start", "Who usually visits the shrine?"),
				dialog("talk", "Visitors? Back then it was mostly youkai — I barely saw any humans. A place meant for human worship, yet every day a crowd of youkai gathered to mooch food and drink. Over time the youkai grew more numerous and the humans fewer... Whatever. Donations barely amounted to anything anyway. Whoever comes, at least it's lively.",
						option("bye", "I see.")),
				CHAT_MISC);

		prefix("reimu/chat_power");
		chat("reimu/chat_power", GLEntities.REIMU.get(),
				List.of(new SelfReputationCondition(100)),
				starterText("start", "What are your abilities?"),
				dialog("talk", "My abilities? Why ask all of a sudden... It's the ability to fly through the sky, of course... Besides that, my intuition is scary accurate. How exactly it works, I couldn't tell you myself.",
						option("bye", "I see.")),
				CHAT_MISC);

		prefix("reimu/chat_money");
		chat("reimu/chat_money", GLEntities.REIMU.get(),
				List.of(new SelfReputationCondition(100)),
				starterText("start", "Do you care a lot about money?"),
				dialog("talk", "Isn't that obvious? Running a shrine nobody visits, I can't even cover basic living expenses. Of course I need money...",
						option("bye", "I see.")),
				CHAT_MISC);

		prefix("reimu/chat_frog");
		chat("reimu/chat_frog", GLEntities.REIMU.get(),
				List.of(hasItem(item(GLItems.STRAW_HAT.get(), 1)), hasQuest(QUEST_OMINOUS_BANNER)),
				starterText("start", "About this straw hat..."),
				dialog("talk", "That straw hat... it would look funny on a frog, wouldn't it?",
						option("ask", "A frog?",
								dialog("idea", "Suwako is a frog goddess, after all. If she blessed frogs like that, maybe they'd develop a taste for raiders. Faith from frogs... heh, that'd be one way to gather it.",
										option("bye", "Heh, maybe.")))),
				CHAT_SPECIAL);

		prefix("reimu/chat_marisa");
		chat("reimu/chat_marisa", GLEntities.REIMU.get(),
				List.of(missingAdv(GLAdvGen.ENTER_MARISA_HOUSE), new SelfReputationCondition(50)),
				starterText("start", "Have you met Marisa?"),
				dialog("talk", "Have you met Marisa yet? Ordinary magician, lives deep in the Magical Forest. Loud, nosy, always borrowing things.",
						option("where", "Where can I find her?",
								dialog("where_ans", "Her house is deep in the Magical Forest. Follow the mushrooms — and the explosions. You can't miss her.",
										option("bye", "Got it!")))),
				CHAT_INFO);
	}

	private void fortune() {
		prefix("reimu/chat_fortune");
		var good = dialog("good",
				"Great blessing! Even I'm jealous. Take this luck and put it to work — rare drops will find you for a while.",
				option("good_bye", "Lucky!"));
		var mid = dialog("mid",
				"Middle blessing. Not bad, not great. Your hands will just move faster for a while — don't waste it.",
				option("mid_bye", "I'll take it."));
		var bad = dialog("bad",
				"Ugh, worst fortune. Dark clouds ahead — trouble finds people with luck like this. Keep your head down for a while.",
				option("bad_bye", "You've got to be kidding..."));
		chat("reimu/chat_fortune", GLEntities.REIMU.get(),
				List.of(hasQuest(QUEST_LOCAL_FOOD), timer(FORTUNE_KEY)),
				starterText("start", "Draw a fortune stick?"),
				dialog("talk",
						"The shrine finally has its own fortune sticks. I blessed them myself, so they actually work. Good luck sticks around, bad luck... also sticks around. One draw per day — the gods get tired too.",
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
		quest("reimu/local_food", new Quest(GLEntities.REIMU.get(), List.of(),
				questTitle("Shrine Provisions"), questDesc("Bring Reimu some bread and mushroom stew so she can learn this world's food."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-bread", new SubmitItemRequirement(List.of(item(Items.BREAD, 8))),
						"b-stew", new SubmitItemRequirement(List.of(item(Items.MUSHROOM_STEW, 3)))
				)),
				List.of(new ExpReward(50), new ReputationReward(10, 300, 10, 300)),
				start("Is the food here to your liking?",
						"Let's not talk about whether I like it — I don't even know what's edible around here. The shrine's stockpiled rations are almost gone. Could you bring me some edible food? If you help out, I can share a little with you — er, as a reward.",
						"Let me prepare some food.", "Thanks for the generous aid. I hope you can get it ready before I faint from hunger.",
						"Maybe next time.", "Ah — is that so. Never mind, the shrine's rations should last me a while. I'll figure out what's edible around here myself."),
				follow("Ask about food preferences.",
						"Marisa always shares mushroom dishes at feasts, and they're pretty tasty. Got any mushrooms? How about mushroom stew and bread?",
						"Still preparing.", "The rations should last a while longer... though they taste awful. I'll be waiting to hear from you."),
				complete("Hand over the food.",
						"Oh, you're back. So, how did it go?",
						"Not yet.", "Mm... alright.",
						"Here, I brought it.", "Not bad, that was quick. Here, your reward. Don't complain it's small — you know how things are at the shrine these days. There used to be youkai visiting; now there's not even a shadow of one...")
		));

		prefix("reimu/hostile_loot");
		quest("reimu/hostile_loot", new Quest(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_LOCAL_FOOD)),
				questTitle("Hostile Analysis"), questDesc("Hunt zombies and skeletons for Reimu and collect their flesh and bones."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-zombie", new KillMobRequirement(reqText("zombie", "Kill zombies"), EntityTypeTags.ZOMBIES, 10),
						"b-skeleton", new KillMobRequirement(reqText("skeleton", "Kill skeletons"), EntityTypeTags.SKELETONS, 10),
						"c-flesh", new SubmitItemRequirement(List.of(item(Items.ROTTEN_FLESH, 8))),
						"d-bone", new SubmitItemRequirement(List.of(item(Items.BONE, 8)))
				)),
				List.of(new ExpReward(100), new ReputationReward(20, 300, 10, 300)),
				start("Ask if the shrine needs help.",
						"Now that you mention it, something's been bothering me lately. It's no incident, but it's annoying — what are those green-skinned things and walking bones? They don't look like youkai, and I can't exterminate them. Drive some off for me and I can share a little... reward with you. So, in or out? You look pretty free anyway.",
						"Sure.", "Alright, I'm counting on you. Don't mess it up — if you do, I'm not cleaning up after you... Just kidding. Good luck.",
						"Maybe next time.", "Ah — really? Fine, forget it. It's not like I was counting on you — I'm just too lazy to move myself."),
				follow("Ask about the hunting.",
						"How's the zombie and skeleton hunting going?",
						"Still working on it.", "Oh — I see. While you're driving them off, bring back some of their materials. I want to study what kind of creatures they are, so I can sleep well at night."),
				complete("Hand over the samples.",
						"Oh, you're back. How did it go?",
						"Not quite done yet.", "No rush, really. But watch yourself while hunting — don't get bitten. Zombies are a lot of trouble.",
						"Done. Here are the materials.", "That was quick. Guess I can count on you after all. You didn't get bitten, did you? I wonder how the zombies here differ from the ones back home... With these I can study them properly.")
		));

		prefix("reimu/talisman_materials");
		quest("reimu/talisman_materials", new Quest(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_HOSTILE_LOOT)),
				questTitle("Talisman Materials"), questDesc("Bring Reimu paper and redstone for her new-world talismans."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-paper", new SubmitItemRequirement(List.of(item(Items.PAPER, 16))),
						"b-redstone", new SubmitItemRequirement(List.of(item(Items.REDSTONE, 8)))
				)),
				List.of(new ExpReward(100), new ReputationReward(10, 300, 0, 300),
						loot("reimu/talisman_materials", LootTable.lootTable()
								.withPool(lootItem(GLTalismans.TALISMAN_POCKET.get(), 1))
								.withPool(lootItem(GLTalismans.HEAL_TALISMAN.get(), 2)))),
				start("Talk about the talismans.",
						"You can't exterminate youkai without ofuda. I bet you've run into some nasty youkai on your travels — if you don't like them, beating them up works, but ofuda drive them off even better. I wonder if this place has materials for making them... redstone dust should do.",
						"I'll gather paper and redstone.", "Alright, I'll leave it to you. Once the materials are gathered, I'll draw some ofuda for you as a reward.",
						"Why redstone?", "If you had cinnabar here it'd work even better, but redstone dust is cheaper."),
				follow("Ask about the ofuda.",
						"You're back. How's the progress?",
						"Still gathering.", "Then I'll wait for good news."),
				complete("Hand over the materials.",
						"You're back. Got all the materials?",
						"Not ready yet.", "No problem. Come find me once the materials are ready, and I'll draw the ofuda for you.",
						"Here's the paper and redstone.", "Good, you're reliable after all. With these materials the shrine's ofuda stock is much fuller... Don't worry, I haven't forgotten your share. Here, take these.")
		));

		prefix("reimu/ender_materials");
		quest("reimu/ender_materials", new Quest(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				questTitle("Ender Materials"), questDesc("Bring Reimu an ender pearl and an ender eye for her gap research."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-pearl", new SubmitItemRequirement(List.of(item(Items.ENDER_PEARL, 1))),
						"b-eye", new SubmitItemRequirement(List.of(item(Items.ENDER_EYE, 1)))
				)),
				List.of(new ExpReward(150), new ReputationReward(10, 300, 0, 300)),
				start("Mention the endermen teleporting around.",
						"They fold space to blink across the world, right? I want that trick for my gap portal. This world's ender pearl bends space like a miniature gap, and an eye can lock onto the way home. Bring me one of each — I think I can anchor a route through them.",
						"I'll find ender materials.", "One pearl and one eye, then. Rare stuff, but that's exactly what I need for the binding.",
						"Maybe next time.", "They're hard to come by. Come back when you've got some to spare."),
				follow("Ask about the gap.",
						"Found any ender materials yet?",
						"Still looking.", "Pearls drop from those little purple rascals in the dark. The eyes you'll have to find deeper down."),
				complete("Hand over the ender materials.",
						"Let's see what you've brought.",
						"Not yet.", "No rush. So long as you keep them safe.",
						"Here's the pearl and eye.", "Perfect. With these I can anchor a real gap. The road home just got a little closer.")
		));

		prefix("reimu/ominous_banner");
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
				start("Talk about the raiders.",
						"People keep whispering about raiders—armed groups with black-and-white banners. Bring me one from their captain. I want to know what it means.",
						"I'll find a raid captain.", "Good. Their captains carry those ominous banners. Take one down and bring it to me. Just watch for the mark they leave behind.",
						"Maybe next time.", "They're still just humans with weapons. Come back when you're ready to deal with them."),
				follow("Ask about the banner.",
						"Found their captain yet?",
						"Still searching.", "Check outposts and patrols. That's where you're most likely to find one."),
				complete("Hand over the banner.",
						"That's the one. Let me see what this strange banner is saying.",
						"Not yet.", "Take your time. I'll be here.",
						"Here's the banner.", "There it is. I'll study what binds these raiders together. Then we'll see what I can do about them.")
		));

		prefix("reimu/raid");
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
				startRaid("Talk about the plan.",
						"I figured out the mark. My ofuda can recreate it. I'll put it on you; enter a village, draw the raiders in, and drive them out.",
						"I'll repel the raid.", "There. The mark is on you. Walk into a village, draw them out, and drive every raider away.",
						"Can't you do it yourself?", "I've got shrine duties. You're the one who knows this world, so I'll leave the fighting to you."),
				follow("Ask about the raid.",
						"The village still stands, but the mark is fading. Care to go again? I can set a fresh one.",
						"Mark me again.", "There. Walk into a village, draw them out, and drive every raider away.",
						new GiveMobEffectAction(MobEffects.BAD_OMEN, BAD_OMEN_DURATION, 0)),
				complete("Tell Reimu the raid is over.",
						"The waves stopped? Good. So the plan worked.",
						"Not yet.", "Not yet? Then stay sharp. They won't give up easily.",
						"The raiders are gone.", "You routed them. I knew baiting them into the village would work. Here—take this border umbrella. It's ready for whatever comes next.")
		));
	}

	private void dailyQuests() {
		prefix("reimu/daily_food");
		var foodTable = requestTable("daily_food", LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(2))
						.add(LootItem.lootTableItem(Items.WHITE_WOOL).apply(SetItemCountFunction.setCount(UniformGenerator.between(6, 8))))
						.add(LootItem.lootTableItem(Items.LEATHER).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 6))))
						.add(LootItem.lootTableItem(Items.IRON_INGOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(6, 8))))
						.add(LootItem.lootTableItem(Items.GOLD_INGOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 4))))
						.add(LootItem.lootTableItem(Items.BREAD).apply(SetItemCountFunction.setCount(UniformGenerator.between(6, 8))))
						.add(LootItem.lootTableItem(Items.APPLE).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 6))))));
		daily("reimu/daily_food", "Shrine Provisions", "Bring Reimu a few supplies for the shrine.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_LOCAL_FOOD)), 60, 10, 150, 0, 150,
				"The donation box is empty again, and I'm hungry. Bring me a few things for the shrine.",
				"Good. Bring back a decent haul.",
				"A hungry miko is a distracted miko. Your call.",
				"The donation box is empty again — bring a decent haul of supplies, remember.",
				"Take your time. Just bring back something useful.",
				"Just what the shrine needed. Thanks.",
				new TreeMap<>(Map.of(
						"a-supplies", rollItem(foodTable))),
				LootTable.lootTable().withPool(lootItem(Items.EMERALD, 1)));

		prefix("reimu/daily_hunt");
		var huntTable = requestTable("daily_hunt", LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 8))))
						.add(LootItem.lootTableItem(Items.BONE).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 8))))
						.add(LootItem.lootTableItem(Items.GUNPOWDER).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))
						.add(LootItem.lootTableItem(Items.SPIDER_EYE).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))));
		daily("reimu/daily_hunt", "Monster Thinning", "Thin out the monsters and bring Reimu some loot.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_HOSTILE_LOOT)), 80, 10, 150, 5, 120,
				"Something's been stirring up trouble near the shrine. Thin out the monsters and bring me some loot.",
				"That's the spirit. Go clear them out.",
				"They'll only grow bolder. But it's your call.",
				"Something's been stirring near the shrine again — thin them out and bring back their loot, remember.",
				"Stay careful out there. I need you in one piece.",
				"Another night's sleep saved. Thanks.",
				new TreeMap<>(Map.of(
						"a-kill", new KillEnemyRequirement(reqText("kill", "Kill hostile mobs"), 12),
						"b-loot", rollItem(huntTable))),
				LootTable.lootTable().withPool(lootItem(Items.EMERALD, 3)));

		prefix("reimu/daily_talisman");
		daily("reimu/daily_talisman", "Talisman Restock", "Bring Reimu paper and redstone to restock her talismans.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)), 60, 10, 150, 0, 150,
				"Talisman stock is running low again. Paper and redstone, just like before.",
				"Great. Restock me and I'll keep the shrine warded.",
				"No stock, no warding. Your call.",
				"Talisman stock is low again — paper for folding, redstone for ink, remember.",
				"The ofuda shelves are starting to look empty.",
				"Good. The shelves are stocked again.",
				new TreeMap<>(Map.of(
						"a-paper", new SubmitItemRequirement(List.of(item(Items.PAPER, 16))),
						"b-redstone", new SubmitItemRequirement(List.of(item(Items.REDSTONE, 8))))),
				LootTable.lootTable().withPool(lootItem(GLTalismans.HEAL_TALISMAN.get(), 2)));

		prefix("reimu/daily_raid");
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
				dailyRaidStart("A new mark is ready, and another village could use protecting. Ready for another raid?",
						"Here's the mark, same as before. Enter the village, hold the line, and drive them out.",
						"The mark will keep. The raiders will still be there when you're ready."),
				dailyFollow("The mark is fading. Ready for another round? I can set a fresh one.",
						"There. Enter the village, hold the line, and drive them out. I'll be here after.",
						new GiveMobEffectAction(MobEffects.BAD_OMEN, BAD_OMEN_DURATION, 0)),
				dailyComplete("The village is still standing. You're getting good at this.")
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
				new ItemStack(Items.EMERALD),
				new TradeRecurrence(1, 24000),
				List.of(item(Items.BREAD, 16))));
		trade("sell_chicken", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_LOCAL_FOOD)),
				new ItemStack(Items.EMERALD),
				new TradeRecurrence(1, 24000),
				List.of(item(Items.COOKED_CHICKEN, 4))));
		trade("sell_string", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_LOCAL_FOOD)),
				new ItemStack(Items.EMERALD),
				new TradeRecurrence(4, 96000),
				List.of(item(Items.STRING, 6))));
		trade("sell_wool", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_LOCAL_FOOD)),
				new ItemStack(Items.EMERALD),
				new TradeRecurrence(4, 96000),
				List.of(itemTag(ItemTags.WOOL, 6))));
		trade("sell_paper", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(Items.EMERALD),
				new TradeRecurrence(16, 24000),
				List.of(item(Items.PAPER, 12))));
		trade("sell_redstone", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(Items.EMERALD),
				new TradeRecurrence(16, 24000),
				List.of(item(Items.REDSTONE, 4))));
		trade("sell_gunpowder", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(Items.EMERALD),
				new TradeRecurrence(4, 24000),
				List.of(item(Items.GUNPOWDER, 4))));
		trade("offer_heal_talisman", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.HEAL_TALISMAN.get()),
				new TradeRecurrence(16, 24000), List.of(item(Items.EMERALD, 8))));
		trade("offer_shelter_talisman", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.SHELTER_TALISMAN.get()),
				new TradeRecurrence(16, 24000), List.of(item(Items.EMERALD, 8))));
		trade("offer_speed_talisman", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.SPEED_TALISMAN.get()),
				new TradeRecurrence(16, 24000), List.of(item(Items.EMERALD, 4))));
		trade("offer_hydrophobic_talisman", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.HYDROPHOBIC_TALISMAN.get()),
				new TradeRecurrence(16, 24000), List.of(item(Items.EMERALD, 4))));
		trade("offer_lava_talisman", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.LAVA_TALISMAN.get()),
				new TradeRecurrence(16, 24000), List.of(item(Items.EMERALD, 4))));
		trade("offer_talisman_pocket", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.TALISMAN_POCKET.get()),
				new TradeRecurrence(4, 24000), List.of(item(Items.EMERALD, 16))));
		trade("offer_border_umbrella", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_RAID)),
				new ItemStack(GLItems.BORDER_UMBRELLA.get()),
				new TradeRecurrence(1, 168000), List.of(item(Items.EMERALD, 48))));
	}

	private SimpleDialogOption start(String button, String intro,
	                                 String accept, String acceptLine,
	                                 String reject, String rejectLine) {
		return option("start", button,
				dialog("start/dialog_1", intro,
						option("start/reject", reject, dialog("start/reject/dialog_1", rejectLine, optionKey(byeKey))),
						option("start/accept", accept, new StartQuestAction(),
								dialog("start/accept/dialog_1", acceptLine, optionKey(byeKey)))));
	}

	private SimpleDialogOption startRaid(String button, String intro,
	                                     String accept, String acceptLine,
	                                     String reject, String rejectLine) {
		return option("start", button,
				dialog("start/dialog_1", intro,
						option("start/reject", reject, dialog("start/reject/dialog_1", rejectLine, optionKey(byeKey))),
						option("start/accept", accept, List.of(new StartQuestAction(), new GiveMobEffectAction(MobEffects.BAD_OMEN, BAD_OMEN_DURATION, 0)),
								dialog("start/accept/dialog_1", acceptLine, optionKey(byeKey)))));
	}

	private SimpleDialogOption follow(String button, String intro, String opt, String optLine) {
		return option("follow_up", button,
				dialog("follow_up/dialog_1", intro,
						option("follow_up/end", opt, dialog("follow_up/end/dialog_1", optLine, optionKey(byeKey)))));
	}

	private SimpleDialogOption follow(String button, String intro, String opt, String optLine, DialogAction<?> action) {
		return option("follow_up", button,
				dialog("follow_up/dialog_1", intro,
						option("follow_up/end", opt, List.of(action),
								dialog("follow_up/end/dialog_1", optLine, optionKey(byeKey)))));
	}

	private SimpleDialogOption complete(String button, String intro,
	                                    String reject, String rejectLine,
	                                    String complete, String completeLine) {
		return option("complete", button,
				dialog("complete/dialog_1", intro,
						option("complete/reject", reject, dialog("complete/reject/dialog_1", rejectLine, optionKey(byeKey))),
						option("complete/handover", complete, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine, optionKey(byeKey)))));
	}

	private void daily(String id, String title, String desc, QuestRecurrence rec,
	                   List<QuestCondition<?>> conditions, int exp, int rep, int softCap, int capIncrease, int maxCap,
	                   String intro, String acceptLine, String rejectLine, String followLine, String optLine,
	                   String completeLine,
	                   Map<String, QuestRequirement<?, ?>> reqs, LootTable.Builder loot) {
		quest(id, new Quest(GLEntities.REIMU.get(), conditions,
				questTitle(title), questDesc(desc),
				Optional.of(rec),
				new TreeMap<>(reqs),
				List.of(new ExpReward(exp), new ReputationReward(rep, softCap, capIncrease, maxCap),
						loot(id, loot)),
				dailyStart(intro, acceptLine, rejectLine),
				dailyFollow(followLine, optLine),
				dailyComplete(completeLine)));
	}

	private SimpleDialogOption dailyStart(String intro, String acceptLine, String rejectLine) {
		return optionKey(dailyStartKey,
				dialog("start/dialog_1", intro,
						optionKey(dailyRejectKey,
								dialog("start/reject/dialog_1", rejectLine, optionKey(byeKey))),
						optionKey(dailyAcceptKey, new StartQuestAction(),
								dialog("start/accept/dialog_1", acceptLine, optionKey(byeKey)))));
	}

	private SimpleDialogOption dailyRaidStart(String intro, String acceptLine, String rejectLine) {
		return optionKey(dailyStartKey,
				dialog("start/dialog_1", intro,
						optionKey(dailyRejectKey,
								dialog("start/reject/dialog_1", rejectLine, optionKey(byeKey))),
						optionKey(dailyAcceptKey,
								List.of(new StartQuestAction(), new GiveMobEffectAction(MobEffects.BAD_OMEN, BAD_OMEN_DURATION, 0)),
								dialog("start/accept/dialog_1", acceptLine, optionKey(byeKey)))));
	}

	private SimpleDialogOption dailyFollow(String followLine, String optLine) {
		return optionKey(dailyFollowKey,
				dialog("follow_up/dialog_1", followLine,
						optionKey(dailyFollowEndKey,
								dialog("follow_up/end/dialog_1", optLine, optionKey(byeKey)))));
	}

	private SimpleDialogOption dailyFollow(String followLine, String optLine, DialogAction<?> action) {
		return optionKey(dailyFollowKey,
				dialog("follow_up/dialog_1", followLine,
						optionKey(dailyFollowEndKey, List.of(action),
								dialog("follow_up/end/dialog_1", optLine, optionKey(byeKey)))));
	}

	private SimpleDialogOption dailyComplete(String completeLine) {
		return optionKey(dailyCompleteKey,
				dialogKey("complete/dialog_1", dailyGotemKey,
						optionKey(dailyHandoverKey, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine, optionKey(byeKey)))));
	}

}