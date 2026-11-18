package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.rpg.action.CompleteQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.StartQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.condition.AnyCondition;
import dev.xkmc.gensokyolegacy.content.rpg.condition.HasAdvancementCondition;
import dev.xkmc.gensokyolegacy.content.rpg.condition.HasQuestCompletedCondition;
import dev.xkmc.gensokyolegacy.content.rpg.condition.SelfReputationCondition;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.GroupDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.SimpleDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestRecurrence;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.KoishiHatRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.QuestRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.SubmitItemRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ExpReward;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ReputationReward;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLAdvGen;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@SuppressWarnings("SameParameterValue")
public class MarisaQDGen extends QuestDialogData {

	private static final ResourceLocation QUEST_FIRST_MUSHROOM = GensokyoLegacy.loc("marisa/first_mushroom");
	private static final ResourceLocation QUEST_HUGE_MUSHROOM = GensokyoLegacy.loc("marisa/huge_mushroom");
	private static final ResourceLocation QUEST_NETHER_MUSHROOM = GensokyoLegacy.loc("marisa/nether_mushroom_prep");
	private static final ResourceLocation QUEST_SHROOMLIGHT = GensokyoLegacy.loc("marisa/shroomlight");
	private static final ResourceLocation QUEST_BREWING = GensokyoLegacy.loc("marisa/brewing");
	public static final ResourceLocation QUEST_HAKKERO = GensokyoLegacy.loc("marisa/hakkero");
	public static final ResourceLocation QUEST_KOISHI = GensokyoLegacy.loc("marisa/koishi_hat");
	public static final ResourceLocation QUEST_TALISMAN_REQUEST = GensokyoLegacy.loc("marisa/talisman_request");
	private static final ResourceLocation QUEST_DAILY_TALISMAN = GensokyoLegacy.loc("marisa/daily_talisman");
	public static final String KOISHI_PROOF = "b-proof";
	private static final ResourceLocation QUEST_REIMU_OMINOUS = GensokyoLegacy.loc("reimu/ominous_banner");

	private static final ResourceLocation ADV_NETHER = ResourceLocation.withDefaultNamespace("nether/root");
	private static final ResourceLocation ADV_FORTRESS = ResourceLocation.withDefaultNamespace("nether/find_fortress");

	private final String byeKey;
	private final String dailyGroupKey;
	private final String dailyStartKey;
	private final String dailyRejectKey;
	private final String dailyThanksKey;

	public MarisaQDGen() {
		prefix("marisa/shared");
		byeKey = text("option", "bye", "Bye!");
		dailyGroupKey = text("option", "daily_group", "Daily Tasks");
		dailyStartKey = text("option", "daily_start", "What can I help?");
		dailyRejectKey = text("option", "daily_reject", "Maybe later.");
		dailyThanksKey = text("option", "daily_thanks", "You're welcome!");

		prefix("marisa/chat");
		defaultDialog(GLEntities.MARISA.get(),
				"Yo, hey~ welcome to the Kirisame Magic Shop!",
				"Hey! You made it too.",
				"Thanks for the treat - time to dig in~",
				"I want to buy something.");
		starter("marisa/chat", new DialogStarter(GLEntities.MARISA.get(), List.of(homeBound()),
				starterText("start", "Business usually busy around here?"),
				dialog("hi", "Used to get plenty, but everything around here changed big-time lately — no idea where my customers went.", TALK_ANIMS,
						option("hi/end", "I see."))
		));

		visitChats();
		chats();
		quests();
		trades();
	}

	/**
	 * Marisa out visiting - Kourindou and the shrine, depending on the day. She visits two houses, so this is written
	 * per host: a joke about cramped rooms does not land twice, and at the shrine she has her own thing to say. {@code
	 * visitingAt} implies she is a guest at all, so none of it can surface in her own shop.
	 */
	private void visitChats() {
		prefix("marisa/visit_kourindou");
		chat("marisa/visit_kourindou_wander", GLEntities.MARISA.get(),
				List.of(visitingAt(GLStructureGen.MORICHIKA_SHOP)),
				starterText("wander", "Not a bad place."),
				dialog("wander", "Kinda cramped, but I like it. Somehow this place gets my brain going better than my own "
						+ "shop.", TALK_ANIMS,
						option("stay", "Not thinking of redecorating your own shop?",
								dialog("stay_ans", "Everyone has their own taste - and maybe I only come out here for the novelty of walking around.", List.of("dialog_decline"),
										option("stay/end", "True enough.")))),
				CHAT_MISC);

		prefix("marisa/visit_shrine");
		chat("marisa/visit_shrine_donate", GLEntities.MARISA.get(),
				List.of(visitingAt(GLStructureGen.HAKUREI_SHRINE)),
				starterText("donate", "Does anyone actually put money in the offering box?"),
				dialog("donate", "The offering box is basically always empty — the coins visitors donate probably don't even "
						+ "match what the youkai toss in when they gather to drink.", List.of("dialog_think_02"),
						option("leave", "Isn't this a place for exterminating youkai? Why do youkai come here to party?",
								dialog("leave_ans", "Youkai are part of the balance too — it's not like I exterminate every "
										+ "youkai I see. In other words, even humans who try to upset the balance "
										+ "become targets.", List.of("dialog_agree"),
										option("leave/end", "Seems my understanding was too one-sided.")))),
				CHAT_MISC);

		chat("marisa/visit_shrine_calm", GLEntities.MARISA.get(),
				List.of(visitingAt(GLStructureGen.HAKUREI_SHRINE)),
				starterText("calm", "It's quiet up here, isn't it?"),
				dialog("calm", "Too quiet, all right. Quiet enough to hear mushrooms growing! A shrine with nothing going on is always this peaceful - might be a good place to retire to, actually.", List.of("dialog_agree"),
						option("bye", "Indeed.")),
				CHAT_MISC);
	}

	private void chats() {
		prefix("marisa/chat_reimu");
		chat("marisa/chat_reimu", GLEntities.MARISA.get(),
				List.of(homeBound(), missingAdv(GLAdvGen.ENTER_HAKUREI_SHRINE), new SelfReputationCondition(50)),
				starterText("start", "Do you know anyone skilled in a fight?"),
				dialog("talk", "You'll find the Hakurei Shrine in the cherry grove — an old friend of mine is there, good "
						+ "with spells for all sorts of trouble. If you're heading somewhere dangerous, she may give "
						+ "you something useful in a fight.", List.of("dialog_think"),
						option("where", "Anything I should know before visiting?",
								dialog("where_ans", "She's easygoing; I think as long as you don't go too far, there won't be a "
										+ "problem.", TALK_ANIMS,
										option("where/end", "I'll pay a visit when I get the chance.")))),
				CHAT_INFO);

		prefix("marisa/chat_morichika");
		chat("marisa/chat_morichika", GLEntities.MARISA.get(),
				List.of(homeBound(), missingAdv(GLAdvGen.ENTER_MORICHIKA_SHOP), new SelfReputationCondition(50)),
				starterText("start", "I wish I had tools to aid exploration…"),
				dialog("talk", "Funny you mention tools — not all my trinkets are handmade. There's another shop in the "
						+ "Magical Forest, Kourindou, run by an old acquaintance of mine.", TALK_ANIMS,
						option("where", "What does he sell?",
								dialog("where_ans", "He buys and sells all sorts of trinkets and tools — call it a secondhand "
										+ "shop. If you need gear for exploring, he may have something. Everything from "
										+ "charms to junk, and he buys everyday supplies too.", TALK_ANIMS,
										option("where/end", "I'll have to pay it a visit sometime.")))),
				CHAT_INFO);

		// Hidden once you have found Alice's house yourself - a pointer, not a secret,
		// and the "what does she want" follow doubles as a hint at quest 2.1.
		prefix("marisa/chat_alice");
		chat("marisa/chat_alice", GLEntities.MARISA.get(),
				List.of(missingAdv(GLAdvGen.ENTER_ALICE_HOUSE), new SelfReputationCondition(50)),
				starterText("start", "Are there any other magic users in this forest?"),
				dialog("talk", "There's a mansion in the forest, home to Alice. She studies magic like me, but wears a scowl "
						+ "all day; the two of us are always butting heads. But when you actually need help, she's "
						+ "warm-hearted — you should get to know her.", List.of("dialog_talk_02"),
						option("where", "Is she as good at potions as you are?",
								dialog("where_ans", "Not exactly — magic users have their own specialties. She's better with "
										+ "dolls; if that interests you, she's the one to see.", List.of("dialog_decline"),
										option("where/end", "Now I'm curious.")))),
				CHAT_INFO);
	}

	private void quests() {
		prefix("marisa/first_mushroom");
		questAnims("start", List.of("dialog_talk_01", "dialog_talk_03", "dialog_talk_04"), "dialog_decline", "dialog_agree");
		questAnims("follow", TALK_ANIMS, List.of("dialog_talk_01", "dialog_talk_03", "dialog_talk_04"));
		questAnims("complete", TALK_ANIMS, "dialog_think", "dialog_agree");
		quest("marisa/first_mushroom", new Quest(GLEntities.MARISA.get(), List.of(),
				questTitle("First Mushrooms"), questDesc("Bring Marisa red and brown mushrooms from the surface."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-mushrooms", new SubmitItemRequirement(List.of(
								item(Items.RED_MUSHROOM, 8),
								item(Items.BROWN_MUSHROOM, 8)))
				)),
				List.of(new ExpReward(50), new ReputationReward(10, 300, 10, 300),
						loot("marisa/first_mushroom", LootTable.lootTable()
								.withPool(lootItem(GLItems.COIN.get(), 4)))),
				start("I saw some requests posted outside your shop?",
						"This area's still real strange to me — even the forest plants look different. I'm "
								+ "just getting my magic research started and I'm short on honest-to-goodness samples. "
								+ "How about fetching me some local mushrooms?",
						"Sure, I'll gather some.", "You're a lifesaver! — Eight red and eight brown mushrooms'll do. Bring me the good stuff!", "Okay, wait for me to get back.",
						"But I refuse.", "Aw, c'mon~ Mushrooms are the foundation of every potion recipe — you'd be helping real magic research here!", "Maybe when I have time."),
				follow("Just double-checking — the request hasn't changed, right?",
						"Hey, I'm not that kind of client — so how's the job going?",
						"Not yet.", "Take your time, no rush.", "See you."),
				complete("I brought what you asked for.",
						"Really? Then I can start my research.",
						"(Hand over the items)", "These are just right — thanks a ton!", "Don't mention it.")
		));

		prefix("marisa/huge_mushroom");
		questAnims("start", THINK_ANIMS_ALL, TALK_ANIMS, "dialog_agree");
		questAnims("follow", TALK_ANIMS, "dialog_agree");
		questAnims("complete", TALK_ANIMS, null, "dialog_agree");
		quest("marisa/huge_mushroom", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_FIRST_MUSHROOM)),
				questTitle("Giant Mushrooms"), questDesc("Bring Marisa huge mushroom blocks."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-huge", new SubmitItemRequirement(List.of(itemTag(GLTagGen.HUGE_MUSHROOM, 8)))
				)),
				List.of(new ExpReward(100), new ReputationReward(10, 300, 0, 300),
						loot("marisa/huge_mushroom", LootTable.lootTable()
								.withPool(lootItem(GLItems.COIN.get(), 6)))),
				start("This posted request — mushrooms again?",
						"Speaking of which — have ya seen the mushrooms around here? They're practically "
								+ "trees! Never seen anything this huge, real whoppers. I tried pluckin' one and it "
								+ "just shattered into little bits. There's gotta be a proper way to harvest the whole "
								+ "thing. Think ya can bring me some whole giant mushroom blocks?",
						"Can't be helped — I'll get them for you.", "Thanks!", "Wait for me to get back.",
						"This stuff is hard to get without enchantments.", "That's your specialty, right? Give me a hand.", "I don't have the enchantment yet, wait a bit longer."),
				follow("Let me double-check the request.",
						"I need whole blocks of those giant mushrooms — got any yet? I can hardly wait!",
						"Still working on it.", "Waiting for your good news.", "No problem."),
				complete("This is what you asked for.",
						"This is it — thanks a lot.",
						"Glad I could help.", "Now I've got dinner and research sorted!", "What dinner?")
		));

		prefix("marisa/nether_mushroom_prep");
		questAnims("start", THINK_ANIMS_ALL, "dialog_decline", "dialog_agree");
		questAnims("follow", TALK_ANIMS, "dialog_greet");
		questAnims("complete", TALK_ANIMS, null, "dialog_agree");
		quest("marisa/nether_mushroom_prep", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_FIRST_MUSHROOM), new HasAdvancementCondition(ADV_NETHER)),
				questTitle("Nether Mushrooms"), questDesc("Bring Marisa nether mushroom samples."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-fungi", new SubmitItemRequirement(List.of(
								item(Items.CRIMSON_FUNGUS, 4),
								item(Items.WARPED_FUNGUS, 4)))
				)),
				List.of(new ExpReward(150), new ReputationReward(20, 300, 10, 300),
						loot("marisa/nether_mushroom_prep", LootTable.lootTable()
								.withPool(lootItem(GLItems.COIN.get(), 6))
								.withPool(lootItem(HexBrew.MIASMA_HEXBREW.bottle.get(), 1)))),
				start("I saw the new request — has your mushroom obsession spread to the Nether?",
						"The big-nosed residents here say the mushrooms down there are something else "
								+ "entirely. No way I'm going there myself — all that fire and lava, nothing like the "
								+ "Former Hell. But you've been there, right? Bring me some Nether mushrooms!",
						"I know the place — I can handle this.", "Knew I could count on ya! Bring those mushrooms back safe!", "Wait for good news.",
						"Not really free right now.", "Then come back when you're free.", "Fine."),
				follow("Let me confirm the request.",
						"I need Nether mushrooms — found any yet? I hear they grow like weeds down there.",
						"Not yet.", "Okay, stay safe~", "See you."),
				complete("I brought them back.",
						"Wow, look at this — it's like a living chunk of sulfur! That this world's plants can adapt to that place is fascinating. Amazing research material — thank you!",
						"(Hand over the items)", "Either way, thanks a bunch!", "You're too kind — I got paid for it.")
		));

		prefix("marisa/shroomlight");
		questAnims("start", TALK_ANIMS, "dialog_decline", "dialog_agree");
		questAnims("follow", TALK_ANIMS, THINK_ANIMS_ALL);
		questAnims("complete", TALK_ANIMS, null, "dialog_agree");
		quest("marisa/shroomlight", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_NETHER_MUSHROOM)),
				questTitle("Glowing Fungi"), questDesc("Bring Marisa shroomlights."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-light", new SubmitItemRequirement(List.of(item(Items.SHROOMLIGHT, 8)))
				)),
				List.of(new ExpReward(150), new ReputationReward(10, 300, 0, 300),
						loot("marisa/shroomlight", LootTable.lootTable()
								.withPool(lootItem(GLItems.COIN.get(), 6))
								.withPool(lootItem(HexBrew.EXPLOSIVE_HEXBREW.bottle.get(), 2)))),
				start("What's this glowing mushroom in the request?",
						"I hear the Nether has giant mushrooms too, with a glowing core inside, right? I want "
								+ "that thing — bring me back a few.",
						"I'll take the job.", "Now we're talkin'! It's called shroomlight, right? Bring back eight.", "Okay.",
						"Why didn't you have me bring it last time...", "Hehe, I didn't know about this stuff last time — one more trip, pretty please.", "Let me get ready first."),
				follow("Let me confirm the request.",
						"Got the shroomlight yet?",
						"Not yet.", "Take care down there — don't get turned into a mushroom yourself!", "There's no such legend!"),
				complete("I brought the stuff back.",
						"Ohhh, so pretty! Look at the veins inside! The magic must flow right through here.",
						"(Hand over the items)", "Can you eat this?", "The glowy ones are usually poisonous, right?")
		));

		prefix("marisa/brewing");
		questAnims("start", "dialog_think", "dialog_decline", "dialog_agree");
		questAnims("follow", "dialog_think", "dialog_think_02");
		questAnims("complete", "dialog_agree", null, TALK_ANIMS);
		quest("marisa/brewing", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SHROOMLIGHT), new HasAdvancementCondition(ADV_FORTRESS)),
				questTitle("Brewing"), questDesc("Bring Marisa blaze rods and nether wart."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-brewing", new SubmitItemRequirement(List.of(
								item(Items.BLAZE_ROD, 4),
								item(Items.NETHER_WART, 12)))
				)),
				List.of(new ExpReward(200), new ReputationReward(20, 300, 10, 300),
						loot("marisa/brewing", LootTable.lootTable()
								.withPool(lootItem(GLItems.COIN.get(), 8))
								.withPool(lootItem(HexBrew.HEXBREW_ELIXIR.bottle.get(), 1)))),
				start("Come to think of it, brewing here is nothing like those witches' — shouldn't it be blazes and nether wart based?",
						"Different understandings, then — but what you describe interests me too. Fetch me "
								+ "some of both — price is negotiable!",
						"Fine, I'll help you.", "I'll wait for good news.", "Okay.",
						"I'm busy with other stuff.", "Fine, tell me when you're free?", "You really don't give up."),
				follow("Let me confirm the request.",
						"Blaze rods and nether wart — I'm itchin' to fire up a brewing stand and study 'em!",
						"I haven't gone yet.", "Be careful on the way.", "Got it."),
				complete("I got the stuff.",
						"Blaze rods and nether wart — now I can finally study this 'brewing' business. Thanks again this time!",
						"We each get what we need.", "Oh right — here's your reward.", "Thanks.")
		));

		prefix("marisa/hakkero");
		questAnims("start", THINK_ANIMS_ALL, "dialog_decline", TALK_ANIMS);
		questAnims("follow", THINK_ANIMS_ALL, "dialog_agree");
		questAnims("complete", TALK_ANIMS, "dialog_decline", TALK_ANIMS);
		// Offered to anyone holding the prototype who has not been given the
		// repaired one yet, and the inverted second condition closes the quest for good.
		quest("marisa/hakkero", new Quest(GLEntities.MARISA.get(),
				List.of(new HasAdvancementCondition(GLAdvGen.OBTAIN_MINI_HAKKERO), homeBound()),
				questTitle("A Proper Hakkero"), questDesc("Hand Marisa your Mini Hakkero Prototype and the parts to repair it."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-parts", new SubmitItemRequirement(List.of(
								item(Items.REDSTONE, 8),
								item(Items.GOLD_INGOT, 4),
								item(Items.IRON_INGOT, 16),
								item(Items.NETHERITE_SCRAP, 1))),
						"a-prototype", new SubmitItemRequirement(List.of(
								item(GLItems.MINI_HAKKERO_PROTOTYPE.get(), 1)))
				)),
				List.of(new ExpReward(300), new ReputationReward(20, 300, 10, 300),
						loot("marisa/hakkero", LootTable.lootTable()
								.withPool(lootItem(GLItems.MINI_HAKKERO.get(), 1)))),
				start("(Show the prototype hakkero)",
						"I feel nostalgic about this thing — the old man sells it as a curiosity, but the "
								+ "shape is interesting. If I gather the materials, I can improve it for you, at least "
								+ "make it less hot to the touch.",
						"What would you need?", "Eight redstone for conduction, four gold for contacts, sixteen iron to shore up the "
								+ "casing, plus one netherite scrap to anchor the whole structure. Don't worry — it'll "
								+ "be much stronger than the prototype.",
						"All right, I'll go get the stuff.",
						"I think it works fine as is, maybe later.", "Then just find me when you want to upgrade it; I have experience.",
						"I'll think about it."),
				follow("Just to confirm - the parts for the repair?",
						"Eight redstone, four gold, sixteen iron, one netherite scrap — and don't forget to "
								+ "bring the prototype itself.",
						"All right.", "Take your time, no rush.",
						"(Leave)"),
				complete("Here - the parts, and the prototype.",
						"Let's see... redstone, gold, and iron — right, the netherite is the truly crucial "
								+ "part... There! Feel the difference? It runs cooler now, and smelting doesn't take as "
								+ "long.",
						"The furnace really doesn't leak heat now.", "There, it's yours.", "Huh, you know, that thing's practically a workshop in one hand.")
		));

		prefix("marisa/golden_apple");
		questAnims("start", THINK_ANIMS_ALL, "dialog_decline", "dialog_agree");
		questAnims("follow", TALK_ANIMS, TALK_ANIMS);
		questAnims("complete", TALK_ANIMS, null, TALK_ANIMS);
		quest("marisa/golden_apple", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)),
				questTitle("Enchanted Golden Apple"), questDesc("Bring Marisa an enchanted golden apple."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-apple", new SubmitItemRequirement(List.of(item(Items.ENCHANTED_GOLDEN_APPLE, 1)))
				)),
				List.of(new ExpReward(300), new ReputationReward(10, 300, 0, 300),
						loot("marisa/golden_apple", LootTable.lootTable()
								.withPool(lootItem(GLItems.COIN.get(), 10)))),
				start("Finally a request that's not about mushrooms?",
						"Ever seen those fancy golden apples — the enchanted kind? Nobody in this world knows "
								+ "how to make 'em anymore, but I can figure it out! Find me one as a sample, and once "
								+ "I've researched it I can reproduce it — whaddaya say?",
						"I'll try to find one.", "No pressure — the request has no deadline.", "Whether you take it or not is the same, really — don't get your hopes up too high.",
						"Too hard to find — maybe later.", "If you happen to find one, you can reconsider — let's leave it for now.", "Depends on my luck."),
				follow("It's really not that easy to find.",
						"You mean the enchanted golden apple — found one?",
						"Not yet.", "I hear there are lots of abandoned buildings underground — rummage through chests and you'll run into one.", "Okay, I'll keep looking."),
				complete("Finally — never mind how I got it, I found one.",
						"Look at that glow — that's genuine lost technology. I'm gonna learn every secret "
								+ "inside, and build my own!",
						"Good luck.", "Wait for my good news — I'll be able to make these for you later!", "That'd be great.")
		));

		prefix("marisa/koishi_hat");
		questAnims("start", TALK_ANIMS);
		questAnims("follow", "dialog_confused", "dialog_agree");
		questAnims("complete", "dialog_agree", null, TALK_ANIMS);
		quest("marisa/koishi_hat", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING),
						new HasQuestCompletedCondition(QUEST_REIMU_OMINOUS)),
				questTitle("Rumors from the Nether"), questDesc("Marisa heard unsettling rumors about the Nether. Investigate — carefully."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						KOISHI_PROOF, new KoishiHatRequirement()
				)),
				List.of(new ExpReward(300), new ReputationReward(20, 300, 10, 300),
						loot("marisa/koishi_hat", LootTable.lootTable()
								.withPool(lootItem(GLItems.COIN.get(), 8)))),
				option("start", "What's with the hush-hush request — what's going on?", new StartQuestAction(),
						dialog("start/dialog_1",
								"Heard the news coming outta the Nether lately? Travelers comin' back white "
										+ "as sheets, swearin' somethin' followed 'em home — but none can say what. "
										+ "Bring a shield before you go; witnesses claim they heard a strange bell, "
										+ "then took a sharp blow to the back without ever seeing the attacker. You're "
										+ "a real fighter — how about poking around the Nether for me?", TALK_ANIMS,
								option("start/end", "Sounds dangerous — I'll keep an eye out."))),
				follow("Hm — nothing seems to be happening?",
						"Best if nothing's wrong — but if you hear a bell, be careful.",
						"Is this really not just a popular urban legend?", "If nobody had really been hurt, I doubt anyone would've actually commissioned an "
								+ "investigation. Better safe than sorry.", "I'll keep that in mind."),
				complete("I ran into it and got out unscathed. No idea who it was in the end, but judging by the hat "
						+ "they dropped, it seems solved.",
						"As expected of you — I never thought this commission would be completed; I was using "
								+ "it as a notice board.",
						"Here, look at the hat left at the scene.", "Haha! So that's how it was. You keep this hat — I know what's going on now.", "Then I'll take it with a clear conscience.")
		));

		dailyQuests();
		talismanQuests();
	}

	private void talismanQuests() {
		prefix("marisa/talisman_request");
		questAnims("start", TALK_ANIMS, "dialog_decline", "dialog_agree");
		questAnims("follow", THINK_ANIMS_ALL, "dialog_agree");
		questAnims("complete", TALK_ANIMS, null, "dialog_agree");
		// Rep 100 AND (known to Reimu OR to Alice): their stock is what convinces
		// Marisa the errand is worth trusting to the player.
		quest("marisa/talisman_request", new Quest(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(100),
						new AnyCondition(List.of(
								new HasQuestCompletedCondition(ReimuQDGen.QUEST_TALISMAN_MATERIALS),
								new HasQuestCompletedCondition(AliceQDGen.QUEST_SEVEN_COLORS)))),
				questTitle("Talisman Courier"), questDesc("Bring Marisa healing talisman papers from Reimu."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-talisman", new SubmitItemRequirement(List.of(item(GLTalismans.HEAL_TALISMAN.get(), 4)))
				)),
				List.of(new ExpReward(200), new ReputationReward(20, 300, 10, 300),
						loot("marisa/talisman_request", LootTable.lootTable()
								.withPool(lootItem(GLItems.BROOM.get(), 1))
								.withPool(lootItem(GLItems.STAR_WAND.get(), 1))
								.withPool(lootItem(GLItems.COIN.get(), 16)))),
				start("Why commission talisman papers?",
						"Say — you've seen Reimu's talisman papers, right? Lately my experiments keep leaving "
								+ "me a mess; my safety measures still aren't good enough, so I thought of these. Buy "
								+ "me a few. And... don't tell her they're for me, okay?",
						"Fine, I'll take the job.", "Great! Four healing papers — no knockoffs, got it?", "As if I would.",
						"Aren't you two close? Why not just ask her?", "Reimu charges even me full price — and she'd tease me about it forever. Just help me out here.", "Let me get ready first."),
				follow("What was I supposed to buy again?",
						"Healing papers — Reimu sells 'em at the shrine — four of 'em, burn that into your brain!",
						"I remember they weren't cheap.", "Her prices are brutal, huh? I'll pay you back once you've bought them!", "Ugh, so I'm fronting the money after all."),
				complete("Bought the talisman papers.",
						"Haha, I really couldn't have asked her myself without you — sorry for the trouble.",
						"Running errands really is a hassle.", "I've got good stuff for you — a broom I whittled this morning, and a star wand I "
								+ "made. With the broom you won't have to worry about running errands at all~", "This really is much more convenient — I'll take it.")
		));

		prefix("marisa/daily_talisman");
		questAnims("daily", TALK_ANIMS, "dialog_decline", TALK_ANIMS, TALK_ANIMS, List.of("dialog_talk_01", "dialog_talk_03", "dialog_talk_04"), List.of("dialog_talk_01", "dialog_talk_03", "dialog_talk_04"), "dialog_agree");
		var talismanTable = requestTable("daily_talisman", LootTable.lootTable().withPool(LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1))
				.add(LootItem.lootTableItem(GLTalismans.HEAL_TALISMAN.get())
						.apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 3))))));
		daily("marisa/daily_talisman", "Talisman Top-Up", "Bring Marisa healing talisman papers from Reimu.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_REQUEST)), 60, 10, 150, 0, 0,
				"The talisman papers are all used up — help me make another trip to Reimu's and buy a few "
						+ "talismans, would ya?",
				"I'll do it!", "Thanks! This is what the request needs~",
				"How could you—",
				"What talismans did you need again?", "Here, take a look.",
				"I'll go make the trip.", "Good, waiting for you to get back!",
				"Bought them.", "You're a huge help!", "(Hand over the items)", "Thanks a bunch!", "You're welcome.",
				new TreeMap<>(Map.of(
						"a-talisman", rollItem(talismanTable)
				)), LootTable.lootTable()
						.withPool(lootItem(GLItems.STAR_WAND.get(), 1))
						.withPool(lootItem(GLItems.COIN.get(), 16)));
	}

	private void dailyQuests() {
		prefix("marisa/daily_mycelium");
		questAnims("daily", TALK_ANIMS, "dialog_decline", "dialog_agree", THINK_ANIMS_ALL, TALK_ANIMS, TALK_ANIMS, "dialog_agree");
		var myceliumTable = requestTable("daily_mycelium", LootTable.lootTable()
				.withPool(lootItem(GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET.cap.asItem(), 3, 6))
				.withPool(lootItem(GLNaturalBlocks.DREAM_MUSHROOM_SET.cap.asItem(), 3, 6))
				.withPool(lootItem(GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap.asItem(), 2, 4))
		);
		daily("marisa/daily_mycelium", "Specialty Mushrooms", "Bring Marisa fresh specialty mushrooms.",
				new QuestRecurrence(24000), List.of(), 60, 10, 150, 0, 0,
				"My stock's runnin' low again — bring me a batch of forest mushrooms.",
				"I'll go.", "That's the spirit — these are what I need this time.",
				"Ugh, fine.",
				"What mushrooms did you need again?", "Take a look at the list.",
				"Thanks.", "Good, don't take too long!",
				"How about these mushrooms?", "Oh, you got 'em? Let me see!", "Here you go!",
				"These mushrooms look great! Thanks!", null,
				new TreeMap<>(Map.of(
						"a-special", rollItem(myceliumTable)
				)), LootTable.lootTable().withPool(lootItem(GLItems.COIN.get(), 1)));

		prefix("marisa/daily_witchcraft");
		questAnims("daily", TALK_ANIMS, "dialog_decline", List.of("dialog_talk_01", "dialog_talk_03", "dialog_talk_04"), TALK_ANIMS, "dialog_agree", List.of("dialog_talk_01", "dialog_talk_03", "dialog_talk_04"), List.of("dialog_talk_01", "dialog_talk_03", "dialog_talk_04"));
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
				"I'm mid-brew and running short on materials — can you get me some monster materials?",
				"Let me help.", "Thanks — here are the requirements, take a look.",
				"That's a problem — guess I'll take my time on my own.",
				"Could you go over the task again?", "These are the materials I need.",
				"Got it.", "I'll get ready — waiting for you to get back.",
				"I got them", "Thanks for the work — you're not hurt, are you?", "Safe and sound (Hand over the items)",
				"Just what I needed! Thanks!", null,
				new TreeMap<>(Map.of(
						"a-grubby", rollItem(witchcraftTable)
				)), LootTable.lootTable().withPool(lootItem(GLItems.COIN.get(), 1))
						.withPool(lootItem(HexBrew.MIASMA_HEXBREW.bottle.get(), 1)));

		prefix("marisa/daily_shroomlight");
		questAnims("daily", TALK_ANIMS, "dialog_decline", "dialog_agree", TALK_ANIMS, List.of("dialog_talk_01", "dialog_talk_04", "dialog_talk_05"), List.of("dialog_talk_01", "dialog_talk_03", "dialog_talk_04"), List.of("dialog_talk_01", "dialog_talk_02", "dialog_talk_05"));
		var shroomlightTable = requestTable("daily_shroomlight", LootTable.lootTable().withPool(LootPool.lootPool()
				.setRolls(ConstantValue.exactly(2))
				.add(LootItem.lootTableItem(Items.SHROOMLIGHT).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6))))
				.add(LootItem.lootTableItem(Items.CRIMSON_FUNGUS).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6))))
				.add(LootItem.lootTableItem(Items.WARPED_FUNGUS).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6))))));
		daily("marisa/daily_shroomlight", "Nether Light Run", "Bring Marisa shroomlights and nether fungus.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_SHROOMLIGHT)), 60, 10, 150, 0, 0,
				"I need more Nether shroomlights and fungus — help me out~",
				"I'll go gather some.", "Thanks~ come back safe!",
				"Fine, then.",
				"What did I need from the Nether again?", "Nether fungus and shroomlight — don't forget.",
				"All right, I'll get on it.", "Stay safe down there!",
				"I'm back from the Nether.", "Oh? Did you get the materials?", "Here.",
				"That's the one — here, your hard-earned pay~", null,
				new TreeMap<>(Map.of(
						"a-light", rollItem(shroomlightTable)
				)), LootTable.lootTable().withPool(lootItem(GLItems.COIN.get(), 2)));

		prefix("marisa/daily_brewing");
		questAnims("daily", THINK_ANIMS_ALL, "dialog_decline", "dialog_agree", THINK_ANIMS_ALL, "dialog_agree", TALK_ANIMS, "dialog_agree");
		daily("marisa/daily_brewing", "Brewing Errand", "Bring Marisa blaze rods and nether wart.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_BREWING)), 60, 20, 150, 5, 130,
				"Brewin' up a storm over here, and I'm fresh outta base ingredients! Please head to the Nether and grab me some blaze rods and nether wart.",
				"I'll handle it.", "Waiting for good news.",
				"Fine, I'll wait a bit longer.",
				"Could you go over the task again?", "Blaze rods and nether wart — remember!",
				"All right, I'll get on it.", "Hurry back — the brew waits for no one!",
				"I got them.", "Oh, you got 'em? Let me see.", "Here.",
				"Thank you!", "You're welcome.",
				new TreeMap<>(Map.of(
						"a-brewing", new SubmitItemRequirement(List.of(
								item(Items.BLAZE_ROD, 2),
								item(Items.NETHER_WART, 8)))
				)), LootTable.lootTable().withPool(lootItem(GLItems.COIN.get(), 8)));
	}

	private void trades() {
		// Restocking trades (player sells to Marisa)
		prefix("marisa");
		trade("sell_mod_shroom", GLEntities.MARISA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(10, 24000), item(GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET.cap, 8));
		trade("sell_dream_shroom", GLEntities.MARISA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(10, 24000), item(GLNaturalBlocks.DREAM_MUSHROOM_SET.cap, 8));
		trade("sell_miasma_shroom", GLEntities.MARISA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(8, 24000), item(GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap, 8));
		trade("sell_spider_eye", GLEntities.MARISA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(4, 24000), item(Items.SPIDER_EYE, 8));
		trade("sell_shroomlight", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SHROOMLIGHT)), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(4, 24000), List.of(item(Items.SHROOMLIGHT, 4))));
		trade("sell_nether_fungus", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SHROOMLIGHT)), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(4, 24000), List.of(item(Items.CRIMSON_FUNGUS, 8))));
		trade("sell_blaze_rod", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), new ItemStack(GLItems.COIN.get(), 2),
				new TradeRecurrence(4, 24000), List.of(item(Items.BLAZE_ROD, 3))));
		trade("sell_nether_wart", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(4, 24000), List.of(item(Items.NETHER_WART, 8))));

		// Offering trades (player buys hexbrews from Marisa), gated by reputation or quests
		trade("offer_miasma", new TradeOffer(GLEntities.MARISA.get(), List.of(),
				new ItemStack(HexBrew.MIASMA_HEXBREW.bottle.get(), 4),
				new TradeRecurrence(4, 24000), List.of(item(GLItems.COIN.get(), 3))));
		// Witch hexbrew: variety of potion effects, unlocked by the brewing quest, no rep gate; half-day restock.
		// Strong variants sell 1 bottle at 4 coin, 16 stock; no-strong effects fall back to long, 4 bottles.
		trade("offer_witch_speed", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), witchStrong(Potions.STRONG_SWIFTNESS),
				new TradeRecurrence(16, 12000), List.of(item(GLItems.COIN.get(), 4))));
		trade("offer_witch_strength", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), witchStrong(Potions.STRONG_STRENGTH),
				new TradeRecurrence(16, 12000), List.of(item(GLItems.COIN.get(), 4))));
		trade("offer_witch_regen", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), witchStrong(Potions.STRONG_REGENERATION),
				new TradeRecurrence(16, 12000), List.of(item(GLItems.COIN.get(), 4))));
		trade("offer_witch_leaping", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), witchStrong(Potions.STRONG_LEAPING),
				new TradeRecurrence(16, 12000), List.of(item(GLItems.COIN.get(), 4))));
		trade("offer_witch_fire", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)), witchLong(Potions.LONG_FIRE_RESISTANCE),
				new TradeRecurrence(4, 12000), List.of(item(GLItems.COIN.get(), 4))));
		trade("offer_shield", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(50)), new ItemStack(HexBrew.SHIELD_HEXBREW.bottle.get()),
				new TradeRecurrence(8, 24000), List.of(item(GLItems.COIN.get(), 2))));
		trade("offer_hyphae", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_HUGE_MUSHROOM)), new ItemStack(HexBrew.HYPHAE_HEXBREW.bottle.get()),
				new TradeRecurrence(8, 24000), List.of(item(GLItems.COIN.get(), 2))));
		trade("offer_explosive", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(50)), new ItemStack(HexBrew.EXPLOSIVE_HEXBREW.bottle.get(), 4),
				new TradeRecurrence(4, 24000), List.of(item(GLItems.COIN.get(), 3))));
		trade("offer_starlight", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(120)), new ItemStack(HexBrew.STARLIGHT_HEXBREW.bottle.get()),
				new TradeRecurrence(8, 24000), List.of(item(GLItems.COIN.get(), 4))));
		trade("offer_sealing_pot", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(120)), new ItemStack(GLBlocks.SEALING_POT.asItem()),
				new TradeRecurrence(1, 48000), List.of(item(GLItems.COIN.get(), 24))));
		trade("offer_star_wand", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(100), new HasQuestCompletedCondition(QUEST_TALISMAN_REQUEST)),
				new ItemStack(GLItems.STAR_WAND.get()),
				new TradeRecurrence(4, 24000), List.of(item(GLItems.COIN.get(), 8))));
		// The broom: a day's whittling for a handful of straw. Same gate as the wand,
		// and the same one-a-day ceiling, so neither reward walks out on the other.
		trade("offer_broom", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_REQUEST)),
				new ItemStack(GLItems.BROOM.get()),
				new TradeRecurrence(1, 24000), List.of(item(GLItems.MYSTICAL_STRAW.get(), 8))));

		// The repaired hakkero, once she has shown she can build one: same bill of
		// materials as the quest, but one a week, the netherite scrap being the bottleneck.
		trade("offer_hakkero", new TradeOffer(GLEntities.MARISA.get(),
				List.of(homeBound(), new HasQuestCompletedCondition(QUEST_HAKKERO)),
				new ItemStack(GLItems.MINI_HAKKERO.get()),
				new TradeRecurrence(1, 168000),
				List.of(item(GLItems.MINI_HAKKERO_PROTOTYPE, 1),
						item(Items.REDSTONE, 8), item(Items.GOLD_INGOT, 4),
						item(Items.IRON_INGOT, 16), item(Items.NETHERITE_SCRAP, 1))));

		// Processing trades - these need her own workshop, so she only offers them
		// from the shop rather than from wherever she happens to be visiting.
		trade("process_golden_apple", new TradeOffer(GLEntities.MARISA.get(),
				List.of(homeBound(), new SelfReputationCondition(30)),
				new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),
				new TradeRecurrence(1, 24000),
				List.of(item(Items.GOLDEN_APPLE, 1), item(Items.GOLD_BLOCK, 1))));
		trade("process_elixir", new TradeOffer(GLEntities.MARISA.get(),
				List.of(homeBound(), new SelfReputationCondition(80)),
				new ItemStack(HexBrew.HEXBREW_ELIXIR.bottle.get()),
				new TradeRecurrence(16, 24000),
				List.of(item(HexBrew.MUNDANE_HEXBREW.bottle, 4))));
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

	private SimpleDialogOption complete(String button, String intro,
										String complete, String completeLine, String completeEnd,
										String reject, String rejectLine, String rejectEnd) {
		return option("complete", button,
				dialog("complete/dialog_1", intro, anims("complete", 0, AGREE_ANIMS),
						option("complete/reject", reject, dialog("complete/reject/dialog_1", rejectLine, anims("complete", 1, List.of()),
								option("complete/reject/end", rejectEnd))),
						option("complete/handover", complete, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine, anims("complete", 2, List.of()),
										option("complete/handover/end", completeEnd)))));
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
		var effects = potion.value().getEffects().stream()
				.map(e -> new MobEffectInstance(e.getEffect(), e.getDuration() * durationMul, e.getAmplifier()))
				.toList();
		for (var e : effects)
			e.getCures().clear();
		return new PotionContents(Optional.empty(), Optional.of(PotionContents.getColor(potion)),
				effects);
	}

	private void daily(String id, String title, String desc, QuestRecurrence rec,
					   List<QuestCondition<?>> conditions, int exp, int rep, int softCap, int capIncrease, int maxCap,
					   String intro, String accept, String acceptLine, String rejectLine,
					   String follow, String followLine, String followButton, String optLine,
					   String completeOpener, String gotemLine, String handover, String completeLine, @Nullable String thanks,
					   Map<String, QuestRequirement<?, ?>> reqs, LootTable.Builder loot) {
		quest(id, new Quest(GLEntities.MARISA.get(), conditions,
				questTitle(title), questDesc(desc),
				Optional.of(rec),
				new TreeMap<>(reqs),
				List.of(new ExpReward(exp), new ReputationReward(rep, softCap, capIncrease, maxCap),
						loot(id, loot)),
				dailyStart(intro, accept, acceptLine, rejectLine),
				dailyFollow(follow, followLine, followButton, optLine),
				dailyComplete(completeOpener, gotemLine, handover, completeLine, thanks)));
	}

	private GroupDialogOption dailyStart(String intro, String accept, String acceptLine, String rejectLine) {
		return groupKey(dailyGroupKey, dailyStartKey,
				dialog("start/dialog_1", intro, anims("daily", 0, THINK_ANIMS),
						optionKey(dailyRejectKey,
								dialog("start/reject/dialog_1", rejectLine, anims("daily", 1, List.of()))),
						option("start/accept", accept, new StartQuestAction(),
								dialog("start/accept/dialog_1", acceptLine, anims("daily", 2, List.of())))));
	}

	private GroupDialogOption dailyFollow(String follow, String followLine, String followButton, String optLine) {
		return groupOption(dailyGroupKey, "follow_up", follow,
				dialog("follow_up/dialog_1", followLine, anims("daily", 3, THINK_ANIMS),
						option("follow_up/end", followButton,
								dialog("follow_up/end/dialog_1", optLine, anims("daily", 4, List.of())))));
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
