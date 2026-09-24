package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
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
	public static final ResourceLocation QUEST_KOISHI = GensokyoLegacy.loc("marisa/koishi_hat");
	public static final ResourceLocation QUEST_TALISMAN_REQUEST = GensokyoLegacy.loc("marisa/talisman_request");
	private static final ResourceLocation QUEST_DAILY_TALISMAN = GensokyoLegacy.loc("marisa/daily_talisman");
	public static final String KOISHI_PROOF = "b-proof";
	private static final ResourceLocation QUEST_REIMU_OMINOUS = GensokyoLegacy.loc("reimu/ominous_banner");

	private static final ResourceLocation ADV_NETHER = ResourceLocation.withDefaultNamespace("nether/root");
	private static final ResourceLocation ADV_FORTRESS = ResourceLocation.withDefaultNamespace("nether/find_fortress");

	private final String byeKey;
	private final String dailyStartKey;
	private final String dailyAcceptKey;
	private final String dailyRejectKey;
	private final String dailyFollowKey;
	private final String dailyFollowEndKey;
	private final String dailyThanksKey;

	public MarisaQDGen() {
		prefix("marisa/shared");
		byeKey = text("option", "bye", "Bye!");
		dailyStartKey = text("option", "daily_start", "What can I help?");
		dailyAcceptKey = text("option", "daily_accept", "I'll do it!");
		dailyRejectKey = text("option", "daily_reject", "Maybe later.");
		dailyFollowKey = text("option", "daily_follow", "Could you go over the task again?");
		dailyFollowEndKey = text("option", "daily_follow_end", "Alright, I'll get on it.");
		dailyThanksKey = text("option", "daily_thanks", "You're welcome!");

		prefix("marisa/chat");
		defaultDialog(GLEntities.MARISA.get(),
				"Yo, hey~ welcome to the Kirisame Magic Shop!",
				"Wanna see the goods? Check out what I just got today!");
		starter("marisa/chat", new DialogStarter(GLEntities.MARISA.get(), List.of(),
				starterText("start", "Business usually busy around here?"),
				dialog("hi", "Used to get plenty, but everything around here changed big-time lately — no idea where my customers went.",
						option("hi/end", "I see."))
		));

		chats();
		quests();
		trades();
	}

	private void chats() {
		prefix("marisa/chat_reimu");
		chat("marisa/chat_reimu", GLEntities.MARISA.get(),
				List.of(missingAdv(GLAdvGen.ENTER_HAKUREI_SHRINE), new SelfReputationCondition(50)),
				starterText("start", "I heard the Hakurei Shrine helped lots of villages — what's that place?"),
				dialog("talk", "You'll find the Hakurei Shrine out in the cherry grove — an old friend of mine's there, a real incident-resolving expert.",
						option("where", "Anything I should know before visiting?",
								dialog("where_ans", "If raiders come at you, go find her — she'll chase them off. Just don't forget the donation, okay?",
										option("where/end", "I'll drop by when I get the chance.")))),
				CHAT_INFO);

		prefix("marisa/chat_morichika");
		chat("marisa/chat_morichika", GLEntities.MARISA.get(),
				List.of(missingAdv(GLAdvGen.ENTER_MORICHIKA_SHOP), new SelfReputationCondition(50)),
				starterText("start", "Did you make all these little trinkets yourself?"),
				dialog("talk", "Not all of 'em — there's another shop in this Magical Forest, Kourindou, run by Rinnosuke Morichika. Got some history with him — anyway, he deals all kinds of curios, kinda a secondhand shop.",
						option("where", "What does he sell?",
								dialog("where_ans", "Sells everything from charms to junk — and he'll buy your spare curios too.",
										option("where/end", "I'll have to pay it a visit sometime.")))),
				CHAT_INFO);
	}

	private void quests() {
		prefix("marisa/first_mushroom");
		quest("marisa/first_mushroom", new Quest(GLEntities.MARISA.get(), List.of(),
				questTitle("First Mushrooms"), questDesc("Bring Marisa red and brown mushrooms from the surface."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-red", new SubmitItemRequirement(List.of(item(Items.RED_MUSHROOM, 8))),
						"b-brown", new SubmitItemRequirement(List.of(item(Items.BROWN_MUSHROOM, 8)))
				)),
				List.of(new ExpReward(50), new ReputationReward(10, 300, 10, 300),
						loot("marisa/first_mushroom", LootTable.lootTable()
								.withPool(lootItem(Items.EMERALD, 4)))),
				start("I saw some requests posted outside your shop?",
						"A new face! Marisa Kirisame, the ordinary magician, reportin' for duty, ze! I'm Marisa Kirisame the magician — just an ordinary human, ze! Say, you live around here, right? This area's still real strange to me — even the forest plants look different. I'm just getting my magic research started and I'm short on honest-to-goodness samples. How about fetching me some red and brown mushrooms?",
						"Sure, I'll gather some.", "You're a lifesaver! — Eight red and eight brown mushrooms'll do. Bring me the good stuff!", "Okay, wait for me to get back.",
						"Ehh, sounds like a hassle.", "Aw, c'mon~ Mushrooms are the foundation of every potion recipe — you'd be helping real magic research here!", "Maybe when I have time."),
				follow("Just double-checking — the request hasn't changed, right?",
						"Hey, I'm not that kind of client — so how's the job going?",
						"Not yet, still looking.", "Take your time — just don't skimp on me, now!", null),
				complete("Here — what you asked for.",
						"Ohoho, these are perfect! Just the right moisture and bite. This'll jump-start my research something fierce!",
						"Here you go.", "Wahoo! Thanks a ton!", "Don't mention it.",
						"Sorry, looks like I don't have enough.", "Sure, no rush. Come back when you're ready.", "Okay.")
		));

		prefix("marisa/huge_mushroom");
		quest("marisa/huge_mushroom", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_FIRST_MUSHROOM)),
				questTitle("Giant Mushrooms"), questDesc("Bring Marisa huge mushroom blocks."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-huge", new SubmitItemRequirement(List.of(itemTag(GLTagGen.HUGE_MUSHROOM, 8)))
				)),
				List.of(new ExpReward(100), new ReputationReward(10, 300, 0, 300),
						loot("marisa/huge_mushroom", LootTable.lootTable()
								.withPool(lootItem(Items.EMERALD, 6)))),
				start("This posted request — mushrooms again?",
						"Speaking of which — have ya seen the mushrooms around here? They're practically trees! I've never seen anything this huge — real whoppers. I tried pluckin' one, but it just shattered into little bits in my hands. There's gotta be a proper way to harvest the solid blocks. Think ya can bring me some whole giant mushroom blocks?",
						"I'll bring you fresh blocks.", "Now that's a good assistant! Bring 'em back intact — caps and stems, either kind is fine. I'll be sitting tight!", "Wait for me to get back.",
						"Uh... why don't you try yourself? It's close by.", "Tried it! They shatter into tiny bits — useless to me. I need 'em in whole blocks — that's your specialty, right?", "I see."),
				follow("Let me double-check the request.",
						"I need whole blocks of those giant mushrooms — got any yet? I can hardly wait!",
						"Still working on it.", "Okay, okay — just don't bring me crumbs. I want whole blocks!", "Okay, okay."),
				complete("Took some doing, but I got it — here you go.",
						"THESE! Feel that? They're packed full of magic!",
						"Glad I could help.", "You're a lifesaver! Now I've got dinner and research!", "Wait — you're gonna eat your research results?")
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
				List.of(new ExpReward(150), new ReputationReward(20, 300, 10, 300),
						loot("marisa/nether_mushroom_prep", LootTable.lootTable()
								.withPool(lootItem(Items.EMERALD, 6))
								.withPool(lootItem(HexBrew.MIASMA_HEXBREW.bottle.get(), 1)))),
				start("I saw the new request — has your mushroom obsession spread to the Nether?",
						"The big-nosed natives around here say the mushrooms down there are something else entirely. No way I'm going there myself — all that fire and lava, and it ain't anything like the Former Hell. But you've been there, right? Bring me back some Nether mushrooms!",
						"I know the place — I can handle this.", "Knew I could count on ya! Bring those mushrooms back safe!", "Wait for good news.",
						"Uh, I don't dare go there yet.", "Heh, fair enough. But that's where the really good research material is! Come back when you're feelin' brave.", "Okay."),
				follow("Let me confirm the request.",
						"I need Nether mushrooms — found any yet? I hear they grow like weeds down there.",
						"Haven't got any yet.", "Okay, stay safe~", "See you."),
				complete("I brought them back.",
						"Wow, look at this — it's like a living chunk of sulfur! That this world's plants can adapt to that place is fascinating. Amazing research material — thank you!",
						"Well — all for research.", "Either way, thanks a bunch!", "You're too kind — I got paid for it, after all.")
		));

		prefix("marisa/shroomlight");
		quest("marisa/shroomlight", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_NETHER_MUSHROOM)),
				questTitle("Glowing Fungi"), questDesc("Bring Marisa shroomlights."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-light", new SubmitItemRequirement(List.of(item(Items.SHROOMLIGHT, 8)))
				)),
				List.of(new ExpReward(150), new ReputationReward(10, 300, 0, 300),
						loot("marisa/shroomlight", LootTable.lootTable()
								.withPool(lootItem(Items.EMERALD, 6))
								.withPool(lootItem(HexBrew.EXPLOSIVE_HEXBREW.bottle.get(), 2)))),
				start("What's this glowing mushroom in the request?",
						"I hear the Nether has giant mushrooms too — with flesh like wood! And a glowing core inside, right? I need that thing — bring me back a few.",
						"I'll take the job.", "Now we're talkin'! Glowing cores — as many as ya can carry!", "Okay.",
						"Why didn't you have me bring it last time...", "Hehe, I didn't know about this stuff last time — one more trip, pretty please.", "Let me get ready first."),
				follow("Let me confirm the request.",
						"You gettin' any of that glowing stuff? I wanna see how it lights up!",
						"Not yet.", "Take care down there — don't get turned into a mushroom yourself!", "There's no such legend!"),
				complete("Hand over the shroomlights.",
						"Ohhh, so pretty! And look at the structure inside this stem! The magic must flow right through here.",
						"Glad you like them.", "Like 'em? I love 'em!", "Just saying — the glowy ones are usually poisonous.")
		));

		prefix("marisa/brewing");
		quest("marisa/brewing", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_SHROOMLIGHT), new HasAdvancementCondition(ADV_FORTRESS)),
				questTitle("Brewing"), questDesc("Bring Marisa blaze rods and nether wart."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-blaze", new SubmitItemRequirement(List.of(item(Items.BLAZE_ROD, 4))),
						"b-wart", new SubmitItemRequirement(List.of(item(Items.NETHER_WART, 12)))
				)),
				List.of(new ExpReward(200), new ReputationReward(20, 300, 10, 300),
						loot("marisa/brewing", LootTable.lootTable()
								.withPool(lootItem(Items.EMERALD, 8))
								.withPool(lootItem(HexBrew.HEXBREW_ELIXIR.bottle.get(), 1)))),
				start("Come to think of it, brewing here is nothing like those witches' — shouldn't it be blazes and nether wart based?",
						"Different schools, y'know — but what you describe interests me too. Fetch me some of both — price is negotiable!",
						"Fine, I'll help you.", "I'll wait for good news.", "Okay.",
						"I'm busy with other stuff.", "Fine, I'm real patient — tell me when you're free?", "You really don't give up."),
				follow("Let me confirm the request.",
						"Blaze rods and nether wart — I'm itchin' to fire up a brewing stand and study 'em!",
						"I haven't actually gone yet — don't rush me.", "Okay, I admit I'm rushing — go on, get going.", "Okay, going now."),
				complete("I got the stuff.",
						"Blaze rods and nether wart — now I can finally study this 'brewing' business. Thanks again this time!",
						"We each get what we need.", "Oh right — here's your reward.", "Thanks.")
		));

		prefix("marisa/golden_apple");
		quest("marisa/golden_apple", new Quest(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_BREWING)),
				questTitle("Enchanted Golden Apple"), questDesc("Bring Marisa an enchanted golden apple."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-apple", new SubmitItemRequirement(List.of(item(Items.ENCHANTED_GOLDEN_APPLE, 1)))
				)),
				List.of(new ExpReward(300), new ReputationReward(10, 300, 0, 300),
						loot("marisa/golden_apple", LootTable.lootTable()
								.withPool(lootItem(Items.EMERALD, 10)))),
				start("Finally a request that's not about mushrooms?",
						"Ever seen those fancy golden apples — the glowy kind? Nobody in this world knows how to make 'em anymore, but I reckon I can figure it out! Find me one as a sample and I'll reverse-engineer the whole thing, then share the knowledge. Whaddaya say?",
						"I'll try to find one.", "No pressure — this request has no deadline.", "Taking it or not, same thing really — wish me luck.",
						"Too hard to find — I don't want it.", "Rare stuff is exactly the fun stuff! No hurry — look around when ya can.", "Depends on my luck."),
				follow("Let me double-check the request.",
						"Any news on the apple?",
						"Haven't found one yet.", "I hear there are lots of abandoned buildings underground — rummage through chests and you'll run into one.", "Okay, I'll keep looking."),
				complete("Finally — never mind how I got it, I found one.",
						"Look at that glow — that's genuine lost technology. I'm gonna take it apart, learn every secret inside, and build my own!",
						"No need to take it apart — you can just bite right in.", "Hey — ever heard of appreciating the moment?!", "That was on purpose.")
		));

		prefix("marisa/koishi_hat");
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
								.withPool(lootItem(Items.EMERALD, 8)))),
				option("start", "What's with the hush-hush request — what's going on?", new StartQuestAction(),
						dialog("start/dialog_1",
								"Heard the news coming outta the Nether lately? Travelers comin' back white as sheets, swearin' somethin' followed 'em home — but none of 'em can say what. Gives me the creeps just thinkin' about it, ze. You're braver than me, though. Poke around down there for me, would ya? And... watch yourself.",
								option("start/end", "Really? I'll go take a look."))),
				follow("Hm — nothing seems to be happening?",
						"Best if nothing's wrong — but while you were gone, travelers started going missing.",
						"That dangerous? Boss, I've suddenly lost my nerve.", "That's fine too — best stay out of the Nether till this blows over.", "I'll keep that in mind."),
				complete("I ran into what you mentioned — and I dealt with it.",
						"Not a scratch? Knew you had it in you — travelers stopped ravin' about bein' followed, too.",
						"Here — look at the loot.", "Haha! Another mystery laid to rest — this is for you.", "Then I'll take it with a clear conscience.")
		));

		dailyQuests();
		talismanQuests();
	}

	private void talismanQuests() {
		prefix("marisa/talisman_request");
		quest("marisa/talisman_request", new Quest(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(100),
						new HasQuestCompletedCondition(ReimuQDGen.QUEST_TALISMAN_MATERIALS)),
				questTitle("Talisman Courier"), questDesc("Bring Marisa healing talisman papers from Reimu."),
				Optional.empty(),
				new TreeMap<>(Map.of(
						"a-talisman", new SubmitItemRequirement(List.of(item(GLTalismans.HEAL_TALISMAN.get(), 4)))
				)),
				List.of(new ExpReward(200), new ReputationReward(20, 300, 10, 300),
						loot("marisa/talisman_request", LootTable.lootTable()
								.withPool(lootItem(GLItems.DOLL_GLOVE.get(), 1))
								.withPool(lootItem(GLItems.DOLL.get(), 1))
								.withPool(lootItem(GLItems.STAR_WAND.get(), 1))
								.withPool(lootItem(Items.EMERALD, 8)))),
				start("A talisman request?",
						"Say — you've seen Reimu's ofuda, right? Those little papers pack a real punch! Lately my experiments keep leaving me singed, and healing's such a hassle. I wanna keep four for myself. Reimu sells 'em, so bring your emeralds. And... don't tell her they're for me. Whaddaya say?",
						"I'll get them from Reimu.", "Great! Four healing papers — no knockoffs, got it?", "As if I would.",
						"Aren't you two close? Why not ask her yourself?", "Reimu charges even me full price — and she'd tease me about it forever. Just help me out here.", "Let me think about it."),
				follow("What was I supposed to buy again?",
						"Healing papers — Reimu sells 'em at the shrine — four of 'em, burn that into your brain!",
						"I remember they weren't cheap.", "Her prices are brutal, huh? But worth every emerald, trust me!", "Ugh, so I'm paying out of pocket after all."),
				complete("Here, the papers.",
						"That's it! One ofuda and every ache is gone — I never could've asked her myself without you.",
						"You didn't ask at all, though.", "Aw, never mind that — I've got good stuff for you too: a doll glove and a fresh doll, handmade by Alice herself, plus a star wand of my own making. Keep it secret for me!", "At this point I kinda have to.")
		));

		prefix("marisa/daily_talisman");
		var talismanTable = requestTable("daily_talisman", LootTable.lootTable().withPool(LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1))
				.add(LootItem.lootTableItem(GLTalismans.HEAL_TALISMAN.get())
						.apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 3))))));
		daily("marisa/daily_talisman", "Talisman Top-Up", "Bring Marisa healing talisman papers from Reimu.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_TALISMAN_REQUEST)), 60, 10, 150, 0, 0,
				"Yo! I've already used up the papers — help me make another trip to Reimu's and buy a few more healing papers, would ya?",
				"Thanks! Still the usual four, got it?",
				"How could you—",
				"Healing papers — I burn through 'em fast with all my experimenting. Gotta keep 'em stocked!",
				"Okay, I'll go buy them.", "Good, waiting for you to get back!",
				"I bought them back.", "You're a huge help!", "Here.", "Thanks a bunch!", "You're welcome.",
				new TreeMap<>(Map.of(
						"a-talisman", rollItem(talismanTable)
				)), LootTable.lootTable()
						.withPool(lootItem(GLItems.DOLL.get(), 1))
						.withPool(lootItem(GLItems.STAR_WAND.get(), 1)));
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
				"Morning! My stock's runnin' low again. Bring me a fresh bundle of forest mushrooms — the glowing ones, the dreamy ones, whatever ya can find. Fresh research material, stat!",
				"That's the spirit! Bring me the good stuff!",
				"Aw, c'mon! The specialty mushrooms are the best part of this world's flora!",
				"Stock's runnin' low again — the glowing ones, the dreamy ones, whatever ya can find. Remember?",
				null, "Good, don't take too long!",
				"How about these mushrooms?", "Oh, you got 'em? Let me see!", "Here you go!",
				"Oh, these are perfect! Thanks, buddy!", null,
				new TreeMap<>(Map.of(
						"a-special", rollItem(myceliumTable)
				)), LootTable.lootTable().withPool(lootItem(Items.EMERALD, 1)));

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
				"Yo! I'm mid-brew and runnin' short on necro-materials. Think ya can scrounge up some rotten flesh, spider eyes, and a few miasma mushrooms? For, uh... research. Yeah. Research.",
				"Thanks — waiting for you to get back!",
				"What, too gross? Magic research can't be picky! Please!",
				"Rotten flesh, spider eyes, miasma mushrooms — don't forget.",
				null, "Good, I'll have the brew ready!",
				"I got them.", "Oh, you got 'em? Let me see!", "Here.",
				"Just what I needed! Thanks!", null,
				new TreeMap<>(Map.of(
						"a-grubby", rollItem(witchcraftTable)
				)), LootTable.lootTable().withPool(lootItem(Items.EMERALD, 1))
						.withPool(lootItem(HexBrew.MIASMA_HEXBREW.bottle.get(), 1)));

		prefix("marisa/daily_shroomlight");
		var shroomlightTable = requestTable("daily_shroomlight", LootTable.lootTable().withPool(LootPool.lootPool()
				.setRolls(ConstantValue.exactly(2))
				.add(LootItem.lootTableItem(Items.SHROOMLIGHT).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6))))
				.add(LootItem.lootTableItem(Items.CRIMSON_FUNGUS).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6))))
				.add(LootItem.lootTableItem(Items.WARPED_FUNGUS).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6))))));
		daily("marisa/daily_shroomlight", "Nether Light Run", "Bring Marisa shroomlights and nether fungus.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_SHROOMLIGHT)), 60, 10, 150, 0, 0,
				"I need more Nether shroomlights and fungus — help me out~",
				"Thanks~ come back safe!",
				"What? The Nether's not that bad! Just watch out for the lava!",
				"Nether fungus and shroomlights — remember, okay?",
				null, "Stay safe down there!",
				"I got them!", "Oh, you got 'em? Let me see.", "Here.",
				"That's the one — thanks!", null,
				new TreeMap<>(Map.of(
						"a-light", rollItem(shroomlightTable)
				)), LootTable.lootTable().withPool(lootItem(Items.EMERALD, 2)));

		prefix("marisa/daily_brewing");
		daily("marisa/daily_brewing", "Brewing Errand", "Bring Marisa blaze rods and nether wart.",
				new QuestRecurrence(24000), List.of(new HasQuestCompletedCondition(QUEST_BREWING)), 60, 20, 150, 5, 130,
				"Brewin' up a storm over here, and I'm fresh outta base ingredients! Please head to the Nether and grab me some blaze rods and nether wart.",
				"Waiting for good news.",
				"Aw, don't leave me hangin'! The brew won't brew itself!",
				"Blaze rods and nether wart — remember!",
				null, "Hurry back — the brew waits for no one!",
				"I got them.", "Oh, you got 'em? Let me see.", "Here.",
				"Thank you!", "You're welcome.",
				new TreeMap<>(Map.of(
						"a-blaze", new SubmitItemRequirement(List.of(item(Items.BLAZE_ROD, 2))),
						"b-wart", new SubmitItemRequirement(List.of(item(Items.NETHER_WART, 8)))
				)), LootTable.lootTable().withPool(lootItem(Items.EMERALD, 8)));
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
		trade("offer_hyphae", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new HasQuestCompletedCondition(QUEST_HUGE_MUSHROOM)), new ItemStack(HexBrew.HYPHAE_HEXBREW.bottle.get()),
				new TradeRecurrence(8, 24000), List.of(item(Items.EMERALD, 2))));
		trade("offer_explosive", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(50)), new ItemStack(HexBrew.EXPLOSIVE_HEXBREW.bottle.get(), 4),
				new TradeRecurrence(4, 24000), List.of(item(Items.EMERALD, 3))));
		trade("offer_starlight", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(120)), new ItemStack(HexBrew.STARLIGHT_HEXBREW.bottle.get()),
				new TradeRecurrence(8, 24000), List.of(item(Items.EMERALD, 4))));
		trade("offer_sealing_pot", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(120)), new ItemStack(GLBlocks.SEALING_POT.asItem()),
				new TradeRecurrence(1, 48000), List.of(item(Items.EMERALD, 24))));
		trade("offer_star_wand", new TradeOffer(GLEntities.MARISA.get(),
				List.of(new SelfReputationCondition(100), new HasQuestCompletedCondition(QUEST_TALISMAN_REQUEST)),
				new ItemStack(GLItems.STAR_WAND.get()),
				new TradeRecurrence(4, 24000), List.of(item(Items.EMERALD, 8))));

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
	                                 String accept, String acceptLine, String acceptEnd,
	                                 String reject, String rejectLine, String rejectEnd) {
		return option("start", button,
				dialog("start/dialog_1", intro,
						option("start/reject", reject, dialog("start/reject/dialog_1", rejectLine, option("start/reject/end", rejectEnd))),
						option("start/accept", accept, new StartQuestAction(),
								dialog("start/accept/dialog_1", acceptLine, option("start/accept/end", acceptEnd)))));
	}

	private SimpleDialogOption follow(String button, String intro, String opt, String optLine, @Nullable String end) {
		var tail = end == null ? optionKey(byeKey) : option("follow_up/end/end", end);
		return option("follow_up", button,
				dialog("follow_up/dialog_1", intro,
						option("follow_up/end", opt, dialog("follow_up/end/dialog_1", optLine, tail))));
	}

	private SimpleDialogOption complete(String button, String intro, String complete, String completeLine, String completeEnd) {
		return option("complete", button,
				dialog("complete/dialog_1", intro,
						option("complete/handover", complete, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine, option("complete/handover/end", completeEnd)))));
	}

	private SimpleDialogOption complete(String button, String intro,
	                                    String complete, String completeLine, String completeEnd,
	                                    String reject, String rejectLine, String rejectEnd) {
		return option("complete", button,
				dialog("complete/dialog_1", intro,
						option("complete/reject", reject, dialog("complete/reject/dialog_1", rejectLine, option("complete/reject/end", rejectEnd))),
						option("complete/handover", complete, new CompleteQuestAction(),
								dialog("complete/handover/dialog_1", completeLine, option("complete/handover/end", completeEnd)))));
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
	                   String intro, String acceptLine, String rejectLine, String followLine, @Nullable String followEndOverride, String optLine,
	                   String completeOpener, String gotemLine, String handover, String completeLine, @Nullable String thanks,
	                   Map<String, QuestRequirement<?, ?>> reqs, LootTable.Builder loot) {
		quest(id, new Quest(GLEntities.MARISA.get(), conditions,
				questTitle(title), questDesc(desc),
				Optional.of(rec),
				new TreeMap<>(reqs),
				List.of(new ExpReward(exp), new ReputationReward(rep, softCap, capIncrease, maxCap),
						loot(id, loot)),
				dailyStart(intro, acceptLine, rejectLine),
				dailyFollow(followLine, followEndOverride, optLine),
				dailyComplete(completeOpener, gotemLine, handover, completeLine, thanks)));
	}

	private SimpleDialogOption dailyStart(String intro, String acceptLine, String rejectLine) {
		return optionKey(dailyStartKey,
				dialog("start/dialog_1", intro,
						optionKey(dailyRejectKey,
								dialog("start/reject/dialog_1", rejectLine)),
						optionKey(dailyAcceptKey, new StartQuestAction(),
								dialog("start/accept/dialog_1", acceptLine))));
	}

	private SimpleDialogOption dailyFollow(String followLine, @Nullable String endOverride, String optLine) {
		var end = endOverride == null
				? optionKey(dailyFollowEndKey, dialog("follow_up/end/dialog_1", optLine))
				: option("follow_up/end", endOverride, dialog("follow_up/end/dialog_1", optLine));
		return optionKey(dailyFollowKey,
				dialog("follow_up/dialog_1", followLine, end));
	}

	private SimpleDialogOption dailyComplete(String opener, String gotemLine, String handover,
	                                         String completeLine, @Nullable String thanks) {
		var done = thanks == null
				? dialog("complete/handover/dialog_1", completeLine)
				: dialog("complete/handover/dialog_1", completeLine, optionKey(dailyThanksKey));
		return option("complete", opener,
				dialog("complete/dialog_1", gotemLine,
						option("complete/handover", handover, new CompleteQuestAction(), done)));
	}

}
