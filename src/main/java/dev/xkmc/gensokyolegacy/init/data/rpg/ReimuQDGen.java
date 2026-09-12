package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.rpg.action.CompleteQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.DialogAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.GiveBadOmenAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.StartQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.condition.HasQuestCompletedCondition;
import dev.xkmc.gensokyolegacy.content.rpg.condition.SelfReputationCondition;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.SimpleDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestRecurrence;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.KillMobRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.QuestRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.RaidVictoryRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.SubmitItemRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ExpReward;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ReputationReward;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
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

public class ReimuQDGen extends QuestDialogData {

	private static final int BAD_OMEN_DURATION = 12000;

	private static final ResourceLocation QUEST_LOCAL_FOOD = GensokyoLegacy.loc("reimu/local_food");
	private static final ResourceLocation QUEST_HOSTILE_LOOT = GensokyoLegacy.loc("reimu/hostile_loot");
	private static final ResourceLocation QUEST_TALISMAN_MATERIALS = GensokyoLegacy.loc("reimu/talisman_materials");
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
		dailyStartKey = text("option", "daily_start", "I can gather that for you.");
		dailyAcceptKey = text("option", "daily_accept", "I'll do it!");
		dailyRejectKey = text("option", "daily_reject", "Maybe later.");
		dailyFollowKey = text("option", "daily_follow", "How's it going?");
		dailyFollowEndKey = text("option", "daily_follow_end", "I'm on it!");
		dailyCompleteKey = text("option", "daily_complete", "I've got the goods!");
		dailyHandoverKey = text("option", "daily_handover", "Here you go!");
		dailyGotemKey = text("dialog", "daily_gotem", "Oh, you got 'em? Let me see!");

		prefix("reimu/chat");
		defaultDialog(GLEntities.REIMU.get(),
				"Hi! What brings you to the shrine?",
				"I'd like to trade with you!");
		starter("reimu/chat", new DialogStarter(GLEntities.REIMU.get(), List.of(),
				starterText("start", "Hi!"),
				dialog("hi", "Hi!", option("bye", "Bye!"))
		));

		quests();
		dailyQuests();
		trades();
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
				start("Talk about food.",
						"I'm still figuring out what's safe to eat here. Bring me some bread and mushroom stew so I can learn the local food.",
						"I'll bring you a meal.", "Great. Bread and mushroom stew. Nothing fancy, just enough to keep this shrine maiden fed.",
						"Maybe next time.", "Foraging in an unfamiliar world wasn't in the shrine maiden handbook. But I suppose I'll manage."),
				follow("Ask about the food.",
						"How's the food hunt going?",
						"Still looking.", "I keep watching the villagers eat and feeling left out."),
				complete("Hand over the food.",
						"Welcome back. Please tell me you brought food, not another donation.",
						"Not yet.", "No rush. I'll just keep dreaming about mushroom stew.",
						"Here you go.", "Ahh, real food. Thanks. You're the first person I can count on here.")
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
				start("Offer to hunt for samples.",
						"The walking dead and clattering skeletons keep ambushing me. They're not exactly youkai, but I want to study them. Bring me some flesh and bones.",
						"I'll hunt zombies and skeletons.", "Good. Ten of each, plus rotten flesh and bones. Bring me enough samples to study.",
						"Maybe next time.", "It is dangerous. But you've survived here longer than I have. You'll manage when you're ready."),
				follow("Ask about the hunt.",
						"How's the hunt going?",
						"Still hunting.", "Got those samples yet? Flesh and bones should tell me plenty."),
				complete("Hand over the samples.",
						"You're back. Please tell me you didn't become one of them.",
						"Not yet.", "No rush. Just don't let them nibble on you.",
						"Here are the samples.", "Perfect. Now I can compare these against the youkai I know. Thanks for the fieldwork.")
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
								.withPool(lootItem(GLTalismans.FOLDED_PAPER_TALISMAN.get(), 1))
								.withPool(lootItem(GLTalismans.HEAL_TALISMAN.get(), 2)))),
				start("Talk about the talismans.",
						"Paper here folds nicely, and this redstone dust glows with power. I can use it as vermilion ink for new-world ofuda. Bring me some.",
						"I'll gather paper and redstone.", "Good. Paper for folding, redstone for ink. That should keep the shrine stocked.",
						"Why redstone?", "It's red, it glows, and it's full of power. That's good enough for vermilion ink."),
				follow("Ask about the ofuda.",
						"Got the materials yet?",
						"Still gathering.", "I want to see how well the new ofuda work."),
				complete("Hand over the materials.",
						"You brought them. Let me see.",
						"Not yet.", "Okay, okay. The ofuda can wait a little longer.",
						"Here's the paper and redstone.", "Excellent. I can make the new ofuda now. I'll make a few for you, too.")
		));

		prefix("reimu/ominous_banner");
		quest("reimu/ominous_banner", new Quest(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				questTitle("Raider's Banner"), questDesc("Bring Reimu an ominous banner from a raid captain."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-banner", new SubmitItemRequirement(List.of(new IngredientEntry(
								DataComponentIngredient.of(false, DataComponents.ITEM_NAME,
										Component.translatable("block.minecraft.ominous_banner").withStyle(ChatFormatting.GOLD), Items.WHITE_BANNER),
								1, Optional.of("Ominous Banner"))))
				)),
				List.of(new ExpReward(200), new ReputationReward(20, 300, 10, 300)),
				start("Talk about the raiders.",
						"People keep whispering about *raiders*—armed groups with black-and-white banners. Bring me one from their captain. I want to know what it means.",
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
								.withPool(lootItem(GLItems.REIMU_SPELL.get(), 1)))),
				startRaid("Talk about the plan.",
						"I figured out the mark. My ofuda can recreate it. I'll put it on you; enter a village, draw the raiders in, and drive them out.",
						"I'll repel the raid.", "There. The mark is on you. Walk into a village, draw them out, and drive every raider away.",
						"Can't you do it yourself?", "I've got shrine duties. You're the one who knows this world, so I'll leave the fighting to you."),
				follow("Ask about the raid.",
						"Is the village still standing?",
						"Still fighting.", "They'll keep coming in waves. Hold the line."),
				complete("Tell Reimu the raid is over.",
						"The waves stopped? Good. So the plan worked.",
						"Not yet.", "Not yet? Then stay sharp. They won't give up easily.",
						"The raiders are gone.", "You routed them. I knew baiting them into the village would work. Here—take my *Innate Dream* spellcard.")
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
				"How's the haul coming along?",
				"Take your time. Just bring back something useful.",
				"Just what the shrine needed. Thanks.",
				new TreeMap<>(Map.of(
						"a-supplies", rollItem(foodTable))));

		prefix("reimu/daily_hunt");
		var huntTable = requestTable("daily_hunt", LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 8))))
						.add(LootItem.lootTableItem(Items.BONE).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 8))))
						.add(LootItem.lootTableItem(Items.GUNPOWDER).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))
						.add(LootItem.lootTableItem(Items.SPIDER_EYE).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))));
		daily("reimu/daily_hunt", "Monster Thinning", "Thin out the monsters and bring Reimu some loot.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_HOSTILE_LOOT)), 80, 10, 150, 5, 130,
				"Something's been stirring up trouble near the shrine. Thin out the monsters and bring me some loot.",
				"That's the spirit. Go clear them out.",
				"They'll only grow bolder. But it's your call.",
				"How's the thrashing going?",
				"Stay careful out there. I need you in one piece.",
				"Another night's sleep saved. Thanks.",
				new TreeMap<>(Map.of(
						"a-kill", new KillMobRequirement(reqText("kill", "Kill hostile mobs"),
								TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("monster")), 12),
						"b-loot", rollItem(huntTable))));

		prefix("reimu/daily_talisman");
		daily("reimu/daily_talisman", "Talisman Restock", "Bring Reimu paper and redstone to restock her talismans.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)), 60, 10, 150, 0, 150,
				"Talisman stock is running low again. Paper and redstone, just like before.",
				"Great. Restock me and I'll keep the shrine warded.",
				"No stock, no warding. Your call.",
				"Any paper and redstone yet?",
				"The ofuda shelves are starting to look empty.",
				"Good. The shelves are stocked again.",
				new TreeMap<>(Map.of(
						"a-paper", new SubmitItemRequirement(List.of(item(Items.PAPER, 16))),
						"b-redstone", new SubmitItemRequirement(List.of(item(Items.REDSTONE, 8))))));

		prefix("reimu/daily_raid");
		quest("reimu/daily_raid", new Quest(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_RAID)),
				questTitle("Raid Defense"), questDesc("Win a raid with Reimu's mark to protect a village."),
				Optional.of(new QuestRecurrence(24000)),
				new TreeMap<>(Map.of(
						"a-raid", new RaidVictoryRequirement(reqText("raid", "Win a raid"), 1)
				)),
				List.of(new ExpReward(200), new ReputationReward(20, 200, 5, 250)),
				dailyRaidStart("A new mark is ready, and another village could use protecting. Ready for another raid?",
						"Here's the mark, same as before. Enter the village, hold the line, and drive them out.",
						"The mark will keep. The raiders will still be there when you're ready."),
				dailyFollow("Is the village holding up?", "They keep coming in waves. Don't drop your guard."),
				dailyComplete("The village is still standing. You're getting good at this.")
		));
	}

	private void trades() {
		prefix("reimu");
		trade("rotten_flesh", GLEntities.REIMU.get(),
				new ItemStack(Items.EMERALD),
				new TradeRecurrence(10, 1200),
				item(Items.ROTTEN_FLESH, 8));
		trade("gap_portal", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new SelfReputationCondition(100)),
				new ItemStack(GLBlocks.GAP_PORTAL.get()),
				new TradeRecurrence(1, 6000),
				List.of(
						item(Items.EMERALD, 10),
						item(Items.ENDER_PEARL, 4),
						item(Items.CRYING_OBSIDIAN, 4))));
		trade("sell_bone", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_HOSTILE_LOOT)),
				new ItemStack(Items.EMERALD),
				new TradeRecurrence(4, 24000),
				List.of(item(Items.BONE, 8))));
		trade("process_talisman", new TradeOffer(GLEntities.REIMU.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_MATERIALS)),
				new ItemStack(GLTalismans.FOLDED_PAPER_TALISMAN.get()),
				new TradeRecurrence(4, 24000),
				List.of(item(Items.PAPER, 4), item(Items.REDSTONE, 2))));
	}

	private SimpleDialogOption start(String button, String intro,
	                                 String accept, String acceptLine,
	                                 String reject, String rejectLine) {
		return option("start", button,
				dialog("start/dialog_1", intro,
						optionKey("start/reject", reject, dialog("start/reject/dialog_1", rejectLine, optionKey("start/reject/bye", byeKey))),
						optionKey("start/accept", accept, new StartQuestAction(),
								dialog("start/accept/dialog_1", acceptLine, optionKey("start/accept/bye", byeKey)))));
	}

	private SimpleDialogOption startRaid(String button, String intro,
	                                     String accept, String acceptLine,
	                                     String reject, String rejectLine) {
		return option("start", button,
				dialog("start/dialog_1", intro,
						optionKey("start/reject", reject, dialog("start/reject/dialog_1", rejectLine, optionKey("start/reject/bye", byeKey))),
						optionKey("start/accept", accept, List.of(new StartQuestAction(), new GiveBadOmenAction(BAD_OMEN_DURATION)),
								dialog("start/accept/dialog_1", acceptLine, optionKey("start/accept/bye", byeKey)))));
	}

	private SimpleDialogOption follow(String button, String intro, String opt, String optLine) {
		return option("follow_up", button,
				dialog("follow_up/dialog_1", intro,
						optionKey("follow_up/end", opt, dialog("follow_up/end/dialog_1", optLine, optionKey("follow_up/end/bye", byeKey)))));
	}

	private SimpleDialogOption complete(String button, String intro,
	                                    String reject, String rejectLine,
	                                    String complete, String completeLine) {
		return option("complete", button,
				dialog("complete/dialog_1", intro,
						optionKey("complete/reject", reject, dialog("complete/reject/dialog_1", rejectLine, optionKey("complete/reject/bye", byeKey))),
						optionKey("complete/handover", complete, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine, optionKey("complete/handover/bye", byeKey)))));
	}

	private void daily(String id, String title, String desc, QuestRecurrence rec,
	                   List<QuestCondition<?>> conditions, int exp, int rep, int softCap, int capIncrease, int maxCap,
	                   String intro, String acceptLine, String rejectLine, String followLine, String optLine,
	                   String completeLine,
	                   Map<String, QuestRequirement<?, ?>> reqs) {
		quest(id, new Quest(GLEntities.REIMU.get(), conditions,
				questTitle(title), questDesc(desc),
				Optional.of(rec),
				new TreeMap<>(reqs),
				List.of(new ExpReward(exp), new ReputationReward(rep, softCap, capIncrease, maxCap)),
				dailyStart(intro, acceptLine, rejectLine),
				dailyFollow(followLine, optLine),
				dailyComplete(completeLine)));
	}

	private SimpleDialogOption dailyStart(String intro, String acceptLine, String rejectLine) {
		return optionKey("start", dailyStartKey,
				dialog("start/dialog_1", intro,
						optionKey("start/reject", dailyRejectKey,
								dialog("start/reject/dialog_1", rejectLine, optionKey("start/reject/bye", byeKey))),
						optionKey("start/accept", dailyAcceptKey, new StartQuestAction(),
								dialog("start/accept/dialog_1", acceptLine, optionKey("start/accept/bye", byeKey)))));
	}

	private SimpleDialogOption dailyRaidStart(String intro, String acceptLine, String rejectLine) {
		return optionKey("start", dailyStartKey,
				dialog("start/dialog_1", intro,
						optionKey("start/reject", dailyRejectKey,
								dialog("start/reject/dialog_1", rejectLine, optionKey("start/reject/bye", byeKey))),
						optionKey("start/accept", dailyAcceptKey,
								List.of(new StartQuestAction(), new GiveBadOmenAction(BAD_OMEN_DURATION)),
								dialog("start/accept/dialog_1", acceptLine, optionKey("start/accept/bye", byeKey)))));
	}

	private SimpleDialogOption dailyFollow(String followLine, String optLine) {
		return optionKey("follow_up", dailyFollowKey,
				dialog("follow_up/dialog_1", followLine,
						optionKey("follow_up/end", dailyFollowEndKey,
								dialog("follow_up/end/dialog_1", optLine, optionKey("follow_up/end/bye", byeKey)))));
	}

	private SimpleDialogOption dailyComplete(String completeLine) {
		return optionKey("complete", dailyCompleteKey,
				dialogKey("complete/dialog_1", dailyGotemKey,
						optionKey("complete/handover", dailyHandoverKey, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine, optionKey("complete/handover/bye", byeKey)))));
	}

}