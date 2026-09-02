package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.content.rpg.action.CompleteQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.StartQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.condition.HasAdvancementCondition;
import dev.xkmc.gensokyolegacy.content.rpg.condition.HasQuestCompletedCondition;
import dev.xkmc.gensokyolegacy.content.rpg.condition.SelfReputationCondition;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
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
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class MarisaQDGen extends QuestDialogData {

	private static final ResourceLocation QUEST_FIRST_MUSHROOM = GensokyoLegacy.loc("marisa/first_mushroom");
	private static final ResourceLocation QUEST_NETHER_MUSHROOM = GensokyoLegacy.loc("marisa/nether_mushroom_prep");
	private static final ResourceLocation QUEST_SHROOMLIGHT = GensokyoLegacy.loc("marisa/shroomlight");
	private static final ResourceLocation QUEST_BREWING = GensokyoLegacy.loc("marisa/brewing");

	private static final ResourceLocation ADV_NETHER = ResourceLocation.withDefaultNamespace("nether/root");
	private static final ResourceLocation ADV_FORTRESS = ResourceLocation.withDefaultNamespace("nether/find_fortress");

	private final String byeKey;
	private final String dailyStartKey;
	private final String dailyAcceptKey;
	private final String dailyRejectKey;
	private final String dailyFollowKey;
	private final String dailyFollowEndKey;
	private final String dailyCompleteKey;
	private final String dailyHandoverKey;
	private final String dailyGotemKey;

	public MarisaQDGen() {
		prefix("marisa/shared");
		byeKey = text("option", "bye", "Bye!");
		dailyStartKey = text("option", "daily_start", "I can gather that for you.");
		dailyAcceptKey = text("option", "daily_accept", "I'll do it!");
		dailyRejectKey = text("option", "daily_reject", "Maybe later.");
		dailyFollowKey = text("option", "daily_follow", "How's it going?");
		dailyFollowEndKey = text("option", "daily_follow_end", "I'm on it!");
		dailyCompleteKey = text("option", "daily_complete", "I've got the goods!");
		dailyHandoverKey = text("option", "daily_handover", "Here you go!");
		dailyGotemKey = text("dialog", "daily_gotem", "Oh, you got 'em? Let me see!");

		prefix("marisa/chat");
		defaultDialog(GLEntities.MARISA.get(),
				"Yo! Marisa Kirisame, the ordinary magician, at your service! Ze!",
				"Take a look! I've got some good stuff today, ze.");
		starter("marisa/chat", new DialogStarter(GLEntities.MARISA.get(), List.of(),
				starterText("start", "Yo! Marisa Kirisame, the ordinary magician, at your service! Ze!"),
				dialog("hi", "Yo! What can I do for ya?",
						optionKey("chat/bye", byeKey))
		));

		quests();
		trades();
	}

	private void quests() {
		prefix("marisa/first_mushroom");
		quest("marisa/first_mushroom", new Quest(GLEntities.MARISA.get(), List.of(),
				questTitle("First Mushrooms"), questDesc("Bring Marisa red and brown mushrooms from the surface."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-red", new SubmitItemRequirement(List.of(item(Items.RED_MUSHROOM, 4))),
						"b-brown", new SubmitItemRequirement(List.of(item(Items.BROWN_MUSHROOM, 4)))
				)),
				List.of(new ExpReward(50), new ReputationReward(10, 300, 10, 300)),
				start("Talk about her mushroom research.",
						"Ah, a newcomer! Marisa Kirisame, the ordinary magician, zo! This whole world's still fresh to me — even the grass smells different. Say, you live around here, right? I'm just getting my magic research started, and I need some honest-to-goodness samples. Bring me some red and brown mushrooms from the surface, would ya?",
						"Sure, I'll gather some.", "That's the spirit, ze! Eight red or brown mushrooms'll do. Bring me the good stuff!",
						"Ehh, sounds like a hassle.", "Aw, c'mon! Mushrooms are the foundation of every good potion recipe. You'd be doin' real science here!"),
				follow("Ask if she got what she needed.",
						"Got 'em yet? I can practically taste the potion potential!",
						"Not yet, still looking.", "Take your time — just don't skimp on me, now!"),
				complete("Hand over the mushrooms.",
						"Ohoho, these are perfect! Just the right moisture and bite. This'll jump-start my research something fierce!",
						"Here you go.", "Wahoo! Thanks a ton!",
						"Not yet.", "Sure, no rush. Come back when you're ready.")
		));

		prefix("marisa/huge_mushroom");
		quest("marisa/huge_mushroom", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_FIRST_MUSHROOM)),
				questTitle("Giant Mushrooms"), questDesc("Bring Marisa huge mushroom blocks."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-huge", new SubmitItemRequirement(List.of(itemTag(GLTagGen.HUGE_MUSHROOM, 8)))
				)),
				List.of(new ExpReward(100), new ReputationReward(10, 300, 0, 300)),
				start("Talk about the giant mushrooms.",
						"Whoa, hold on. Have ya seen the mushrooms around here? They're practically trees! I've never seen anything this huge — real whoppers. I tried pluckin' one, but it just shattered into little bits in my hands. There's gotta be a proper way to harvest the solid blocks. Think ya can bring me some whole giant mushroom blocks?",
						"I'll bring you fresh blocks.", "That's what I like to hear! Bring 'em intact — caps and stems, either kind is fine.",
						"Can't you just break them yourself?", "Tried it! They shatter into tiny caps — useless to me. I need 'em whole, stem and all. That's your specialty, right?"),
				follow("About those mushroom blocks.",
						"Got any of those big blocks yet? I wanna see how the cap connects to the stem!",
						"Still working on it.", "Okay, okay — just don't bring me crumbs. I want the good stuff!"),
				complete("Hand over the mushroom blocks.",
						"THESE! These are exactly what I needed! Feel that density, ze? There's some serious magic packed in here!",
						"Glad I could help.", "You're a lifesaver! Now I've got dinner *and* research!")
		));

		prefix("marisa/nether_mushroom_prep");
		quest("marisa/nether_mushroom_prep", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_FIRST_MUSHROOM), new HasAdvancementCondition(ADV_NETHER)),
				questTitle("Nether Mushrooms"), questDesc("Bring Marisa nether mushroom samples."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-crimson", new SubmitItemRequirement(List.of(item(Items.CRIMSON_FUNGUS, 4))),
						"b-warped", new SubmitItemRequirement(List.of(item(Items.WARPED_FUNGUS, 4)))
				)),
				List.of(new ExpReward(150), new ReputationReward(20, 300, 10, 300)),
				start("Talk about the Nether.",
						"Say — you've been to that creepy red world under the rock, right? The Nether, the fiery one? I hear the 'shrooms down there are somethin' else entirely. Never had the guts to go myself — all that heat and lava, yikes. But you've been there, haven't ya? Bring me some genuine Nether mushroom samples!",
						"I've been to the Nether. I can do this.", "Then I knew I could count on ya! Bring back the weird stuff!",
						"The Nether scares even me.", "Heh, fair enough. But that's where the really good research material is! Come back when you're feelin' brave."),
				follow("About the nether mushrooms.",
						"Found any of that red Nether stuff yet? I hear it grows like a weed down there.",
						"The hoglins are guarding them.", "Hoglin trouble, huh? Just bring me what ya can!"),
				complete("Hand over the nether mushrooms.",
						"Oh man, look at this! You can practically feel the fire in it! The flora in this world adapts to *everything*. That's amazing research material, ze!",
						"Everything for science.", "Science! You get it! Thanks a million!")
		));

		prefix("marisa/shroomlight");
		quest("marisa/shroomlight", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_NETHER_MUSHROOM)),
				questTitle("Shroomlight & Fungus Trees"), questDesc("Bring Marisa shroomlights."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-light", new SubmitItemRequirement(List.of(item(Items.SHROOMLIGHT, 8)))
				)),
				List.of(new ExpReward(150), new ReputationReward(10, 300, 0, 300)),
				start("Talk about the fungus trees.",
						"Okay, okay! You mentioned those giant *fungus trees* in the Nether — whole towers of mushroom! And they glow, right? The shroomlights? I've gotta see one up close. I need some samples: a few of those light-up shroomlight blocks and a chunk of the tree itself. Best research material money can't buy!",
						"I'll bring back samples.", "Now we're talkin'! Shroomlights and fungus — as many as ya can carry!",
						"They're dangerous to climb.", "Everything good is a little dangerous! Just grab a few blocks, then scoot. Easy!"),
				follow("About the shroomlights.",
						"You gettin' any of that glowing stuff? I wanna see how it lights up!",
						"Still in the Nether.", "Take care down there — don't get turned into a mushroom yourself!"),
				complete("Hand over the shroomlights.",
						"Ohhh, these little lights are *beautiful*! And look at the structure inside this stem! The magic must practically flow through here. This is gonna make my potions glow like nobody's business!",
						"Glad you like them.", "Like 'em? I love 'em! You've got a real eye for research!")
		));

		prefix("marisa/brewing");
		quest("marisa/brewing", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SHROOMLIGHT), new HasAdvancementCondition(ADV_FORTRESS)),
				questTitle("Brewing the Minecraft Way"), questDesc("Bring Marisa blaze rods and nether wart."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-blaze", new SubmitItemRequirement(List.of(item(Items.BLAZE_ROD, 4))),
						"b-wart", new SubmitItemRequirement(List.of(item(Items.NETHER_WART, 12)))
				)),
				List.of(new ExpReward(200), new ReputationReward(20, 300, 10, 300)),
				start("Talk about the native potion system.",
						"Hold up! You mentioned *blazes* down there, right? And nether wart? The folks in this world figured out how to brew potions from scratch — a whole native potion system! Can ya imagine? I *gotta* understand it. I hear ya need a blaze rod to power a brewing stand, and nether wart to make the base. Bring me samples of both, and I'll reverse-engineer this 'Minecraft brewing' thing in no time!",
						"I'll get you the samples.", "Right on! Blaze rods and nether wart — the key ingredients!",
						"Blazes are tough to fight.", "So are goblins, and I've survived those bars for years! C'mon, a smart one like you can handle it!"),
				follow("About the brewing ingredients.",
						"Got any blaze rods or wart yet? I'm itchin' to fire up a brewing stand!",
						"Blazes keep melting me.", "Craft some fire resistance first! Don't be reckless."),
				complete("Hand over the brewing ingredients.",
						"Jackpot! Blaze rods and nether wart — now I can finally study this 'brewstand' business. You're the best research assistant a magician could ask for, ze!",
						"Anything for magic.", "Magic! That's the spirit! Thanks a bundle!")
		));

		prefix("marisa/golden_apple");
		quest("marisa/golden_apple", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)),
				questTitle("Enchanted Golden Apple"), questDesc("Bring Marisa an enchanted golden apple."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-apple", new SubmitItemRequirement(List.of(item(Items.ENCHANTED_GOLDEN_APPLE, 1)))
				)),
				List.of(new ExpReward(300), new ReputationReward(10, 300, 0, 300)),
				start("Talk about the lost golden apple tech.",
						"One more thing, one more thing! Ever seen those fancy golden apples — the glowy ones, the 'enchanted' ones? The recipe's completely lost to this world. Nobody can craft 'em anymore. But *I* can figure it out! Bring me one as a prime sample, and I'll reverse-engineer the whole thing and share the knowledge. Whaddaya say?",
						"I'll try to find one.", "That's the Marisa-approved spirit! One enchanted golden apple, comin' right up!",
						"Those are really rare.", "Rare stuff is exactly the fun stuff! They're hidden in ruins and loot, so come back when you've sniffed one out."),
				follow("About the enchanted golden apple.",
						"Any sign of one of those glowy apples yet? Check dungeon chests — they like to hide in there!",
						"Haven't found one yet.", "Keep lookin'! It's gotta be out there somewhere. I just know it!"),
				complete("Hand over the enchanted golden apple.",
						"THIS! This is a treasure! Look at that glow — that's *real* lost technology. I'm gonna take this apart, learn every secret, and build it myself. You just made a huge breakthrough possible!",
						"I knew you could do it.", "Heh! With me around, ain't nothin' impossible!")
		));

		dailyQuests();
	}

	private void dailyQuests() {
		prefix("marisa/daily_mycelium");
		var myceliumTable = requestTable("daily_mycelium", LootTable.lootTable().withPool(LootPool.lootPool()
				.setRolls(ConstantValue.exactly(3))
				.add(LootItem.lootTableItem(GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET.cap).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6))))
				.add(LootItem.lootTableItem(GLNaturalBlocks.DREAM_MUSHROOM_SET.cap).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6))))
				.add(LootItem.lootTableItem(GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6))))));
		daily("marisa/daily_mycelium", "Specialty Mushrooms", "Bring Marisa fresh specialty mushrooms.",
				new QuestRecurrence(24000), List.of(), 60, 10, 150, 0, 0,
				"Morning! My stock's runnin' low again. Bring me a fresh bundle of this world's specialty mushrooms — the glowing ones, the dreamy ones, whatever ya can find. Fresh research material, stat!",
				"That's the spirit! Bring me the good stuff!",
				"Aw, c'mon! The specialty mushrooms are the best part of this world's flora!",
				"Take your time — but don't skimp on me, now!",
				"Good, don't take too long!",
				"Oh, these are perfect! Thanks, buddy!",
				new TreeMap<>(Map.of(
						"a-special", rollItem(myceliumTable)
				)));

		prefix("marisa/daily_witchcraft");
		var witchcraftTable = requestTable("daily_witchcraft", LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
						.add(LootItem.lootTableItem(GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 3)))))
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(SetItemCountFunction.setCount(UniformGenerator.between(6, 12)))))
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
						.add(LootItem.lootTableItem(Items.SPIDER_EYE).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 3))))
						.add(LootItem.lootTableItem(Items.BONE).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 3))))
						.add(LootItem.lootTableItem(Items.GUNPOWDER).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 3))))));
		daily("marisa/daily_witchcraft", "Witchcraft Bits", "Bring Marisa rotten flesh, spider eyes, and miasma mushrooms.",
				new QuestRecurrence(24000), List.of(), 60, 10, 150, 0, 0,
				"Yo! I'm mid-brew and I'm runnin' short on the gross stuff. Think ya can scrounge up some rotten flesh, spider eyes, and a few of those miasma mushrooms? For, uh... research. Yeah. Research.",
				"Right on! Bring me the grubby bits!",
				"What, too good for a little research? C'mon, it'll be worth it!",
				"Take your time — but don't skimp on me, now!",
				"Good, I'll have the brew ready!",
				"Just what I needed for the brew! Thanks!",
				new TreeMap<>(Map.of(
						"a-grubby", rollItem(witchcraftTable)
				)));

		prefix("marisa/daily_shroomlight");
		var shroomlightTable = requestTable("daily_shroomlight", LootTable.lootTable().withPool(LootPool.lootPool()
				.setRolls(ConstantValue.exactly(2))
				.add(LootItem.lootTableItem(Items.SHROOMLIGHT).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6))))
				.add(LootItem.lootTableItem(Items.CRIMSON_FUNGUS).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6))))
				.add(LootItem.lootTableItem(Items.WARPED_FUNGUS).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6))))));
		daily("marisa/daily_shroomlight", "Nether Light Run", "Bring Marisa shroomlights and nether fungus.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_SHROOMLIGHT)), 60, 10, 150, 0, 0,
				"Hey! I need more of those glowing mushrooms and shroomlights from the Nether. You're my personal Nether scout now, ze! Make a quick trip and bring 'em back.",
				"That's my scout! Fetch me the glowy stuff!",
				"What? The Nether's not that bad! Just watch out for the lava!",
				"Still out in the Nether? Don't become a mushroom!",
				"Stay safe down there!",
				"Ah, perfect! Thanks, scout!",
				new TreeMap<>(Map.of(
						"a-light", rollItem(shroomlightTable)
				)));

		prefix("marisa/daily_brewing");
		daily("marisa/daily_brewing", "Brewing Errand", "Bring Marisa blaze rods and nether wart.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_BREWING)), 60, 20, 150, 5, 130,
				"Brewin' up a storm over here, and I'm fresh outta base ingredients! Skedaddle to the Nether and grab me some blaze rods and nether wart, willya? There's a good buddy!",
				"That's my buddy! Fetch me the brew bits!",
				"Aw, don't leave me hangin'! The brew won't brew itself!",
				"Still gatherin'? The brew's waitin' on ya!",
				"Hurry back, the brew's waiting!",
				"The brew thanks you!",
				new TreeMap<>(Map.of(
						"a-blaze", new SubmitItemRequirement(List.of(item(Items.BLAZE_ROD, 2))),
						"b-wart", new SubmitItemRequirement(List.of(item(Items.NETHER_WART, 8)))
				)));
	}

	private void trades() {
		// Restocking trades (player sells to Marisa)
		prefix("marisa");
		trade("sell_mod_shroom", GLEntities.MARISA.get(), new ItemStack(Items.EMERALD),
				new TradeRecurrence(10, 24000), item(GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET.cap, 8));
		trade("sell_dream_shroom", GLEntities.MARISA.get(), new ItemStack(Items.EMERALD),
				new TradeRecurrence(10, 24000), item(GLNaturalBlocks.DREAM_MUSHROOM_SET.cap, 8));
		trade("sell_miasma_shroom", GLEntities.MARISA.get(), new ItemStack(Items.EMERALD),
				new TradeRecurrence(8, 24000), item(GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap, 8));
		trade("sell_spider_eye", GLEntities.MARISA.get(), new ItemStack(Items.EMERALD),
				new TradeRecurrence(4, 24000), item(Items.SPIDER_EYE, 8));
		trade("sell_shroomlight", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SHROOMLIGHT)), new ItemStack(Items.EMERALD),
				new TradeRecurrence(4, 24000), List.of(item(Items.SHROOMLIGHT, 4))));
		trade("sell_nether_fungus", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SHROOMLIGHT)), new ItemStack(Items.EMERALD),
				new TradeRecurrence(4, 24000), List.of(item(Items.CRIMSON_FUNGUS, 8))));
		trade("sell_blaze_rod", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), new ItemStack(Items.EMERALD, 2),
				new TradeRecurrence(4, 24000), List.of(item(Items.BLAZE_ROD, 3))));
		trade("sell_nether_wart", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), new ItemStack(Items.EMERALD),
				new TradeRecurrence(4, 24000), List.of(item(Items.NETHER_WART, 8))));

		// Offering trades (player buys hexbrews from Marisa), gated by reputation or quests
		trade("offer_miasma", new TradeOffer(GLEntities.MARISA.get(), List.of(),
				new ItemStack(HexBrew.MIASMA_HEXBREW.bottle.get(), 4),
				new TradeRecurrence(4, 24000), List.of(item(Items.EMERALD, 3))));
		// Witch hexbrew: variety of potion effects, unlocked by the brewing quest, no rep gate; half-day restock.
		// Strong variants sell 1 bottle at 4 emerald, 16 stock. No-strong effects fall back to long, 4 bottles at 4 emerald.
		trade("offer_witch_speed", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), witchStrong(Potions.STRONG_SWIFTNESS),
				new TradeRecurrence(16, 12000), List.of(item(Items.EMERALD, 4))));
		trade("offer_witch_strength", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), witchStrong(Potions.STRONG_STRENGTH),
				new TradeRecurrence(16, 12000), List.of(item(Items.EMERALD, 4))));
		trade("offer_witch_regen", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), witchStrong(Potions.STRONG_REGENERATION),
				new TradeRecurrence(16, 12000), List.of(item(Items.EMERALD, 4))));
		trade("offer_witch_leaping", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), witchStrong(Potions.STRONG_LEAPING),
				new TradeRecurrence(16, 12000), List.of(item(Items.EMERALD, 4))));
		trade("offer_witch_fire", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), witchLong(Potions.LONG_FIRE_RESISTANCE),
				new TradeRecurrence(4, 12000), List.of(item(Items.EMERALD, 4))));
		trade("offer_shield", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(50)), new ItemStack(HexBrew.SHIELD_HEXBREW.bottle.get()),
				new TradeRecurrence(8, 24000), List.of(item(Items.EMERALD, 2))));
		trade("offer_explosive", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(50)), new ItemStack(HexBrew.EXPLOSIVE_HEXBREW.bottle.get(), 4),
				new TradeRecurrence(4, 24000), List.of(item(Items.EMERALD, 3))));
		trade("offer_starlight", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(120)), new ItemStack(HexBrew.STARLIGHT_HEXBREW.bottle.get()),
				new TradeRecurrence(8, 24000), List.of(item(Items.EMERALD, 4))));

		// Processing trades
		trade("process_golden_apple", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(30)),
				new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),
				new TradeRecurrence(1, 24000),
				List.of(item(Items.GOLDEN_APPLE, 1), item(Items.GOLD_BLOCK, 1))));
		trade("process_elixir", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(80)),
				new ItemStack(HexBrew.HEXBREW_ELIXIR.bottle.get()),
				new TradeRecurrence(16, 24000),
				List.of(item(HexBrew.MUNDANE_HEXBREW.bottle, 4))));
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

	private SimpleDialogOption follow(String button, String intro, String opt, String optLine) {
		return option("follow_up", button,
				dialog("follow_up/dialog_1", intro,
						optionKey("follow_up/end", opt, dialog("follow_up/end/dialog_1", optLine, optionKey("follow_up/end/bye", byeKey)))));
	}

	private SimpleDialogOption complete(String button, String intro, String complete, String completeLine) {
		return option("complete", button,
				dialog("complete/dialog_1", intro,
						optionKey("complete/handover", complete, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine, optionKey("complete/handover/bye", byeKey)))));
	}

	private SimpleDialogOption complete(String button, String intro,
	                                    String complete, String completeLine,
	                                    String reject, String rejectLine) {
		return option("complete", button,
				dialog("complete/dialog_1", intro,
						optionKey("complete/reject", reject, dialog("complete/reject/dialog_1", rejectLine, optionKey("complete/reject/bye", byeKey))),
						optionKey("complete/handover", complete, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine, optionKey("complete/handover/bye", byeKey)))));
	}

	private ItemStack witchStrong(Holder<Potion> potion) {
		var stack = new ItemStack(HexBrew.WITCH_HEXBREW.bottle.get(), 1);
		stack.set(DataComponents.POTION_CONTENTS, witchContents(potion, 2));
		return stack;
	}

	private ItemStack witchLong(Holder<Potion> potion) {
		var stack = new ItemStack(HexBrew.WITCH_HEXBREW.bottle.get(), 4);
		stack.set(DataComponents.POTION_CONTENTS, witchContents(potion, 1));
		return stack;
	}

	private PotionContents witchContents(Holder<Potion> potion, int durationMul) {
		return new PotionContents(Optional.empty(), Optional.of(PotionContents.getColor(potion)),
				potion.value().getEffects().stream()
						.map(e -> new MobEffectInstance(e.getEffect(), e.getDuration() * durationMul, e.getAmplifier()))
						.toList());
	}

	private void daily(String id, String title, String desc, QuestRecurrence rec,
	                   List<QuestCondition<?>> conditions, int exp, int rep, int softCap, int capIncrease, int maxCap,
	                   String intro, String acceptLine, String rejectLine, String followLine, String optLine,
	                   String completeLine,
	                   Map<String, QuestRequirement<?, ?>> reqs) {
		quest(id, new Quest(GLEntities.MARISA.get(), conditions,
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
