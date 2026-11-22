package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.attachment.index.MissingTowerManager;
import dev.xkmc.gensokyolegacy.content.rpg.action.CompleteQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.GiveItemAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.StartQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.condition.MissingTowerNearCondition;
import dev.xkmc.gensokyolegacy.content.rpg.condition.SelfReputationCondition;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.ConditionalDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.ConditionalEntry;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.Dialog;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.SimpleDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestRecurrence;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.MissingTowerRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ExpReward;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ReputationReward;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Nazrin: a Myouren Temple resident whose one loop is Shou's lost treasure tower. The greeting, the
 * party line, the lost-tower daily and a dowser trade are all she needs — she has no shop.
 *
 * @see "doc/quest/nazrin.md"
 */
public class NazrinQDGen extends QuestDialogData {

	public static final ResourceLocation QUEST_LOST_TOWER = GensokyoLegacy.loc("nazrin/lost_tower");

	private final String leaveKey;
	private final String startKey;
	private final String acceptKey;
	private final String rejectKey;
	private final String followKey;
	private final String completeKey;
	private final String handoverKey;

	public NazrinQDGen() {
		prefix("nazrin/shared");
		leaveKey = text("option", "leave", "(Leave)");
		startKey = text("option", "daily_start", "[Daily] Need something found?");
		acceptKey = text("option", "daily_accept", "I'll find it.");
		rejectKey = text("option", "daily_reject", "Not right now.");
		followKey = text("option", "daily_follow", "[Daily] Where should I look?");
		completeKey = text("option", "daily_complete", "[Daily] I found the tower.");
		handoverKey = text("option", "daily_handover", "Here it is.");

		chats();
		quests();
		trades();
	}

	private void chats() {
		prefix("nazrin/chat");
		defaultDialog(GLEntities.NAZRIN.get(),
				"Hold still — my dowsers are twitching, and they never twitch for nothing.",
				"Back again? There is always something worth digging up around here.",
				"A feast? Fine, but only if the food is worth the trip.",
				"What do you need from me?");
	}

	private void quests() {
		prefix("nazrin/lost_tower");
		quest("nazrin/lost_tower", new Quest(GLEntities.NAZRIN.get(),
				List.of(),
				questTitle("The Lost Tower"), questDesc("Help Nazrin find Shou's lost treasure tower and bring it back."),
				Optional.of(new QuestRecurrence(24000)),
				new TreeMap<>(Map.of(
						"a-tower", new MissingTowerRequirement(QUEST_LOST_TOWER,
								reqText("tower", "Find and pick up Shou's treasure tower")))),
				List.of(new ExpReward(60), new ReputationReward(50, 150, 10, 300)),
				start(),
				follow(),
				complete()));
	}

	/**
	 * Nazrin says the tower is close or gone depending on where her dowser points. The condition reads
	 * the same {@link MissingTowerManager#MAX_RADIUS} the tower is rolled within, so the two cannot
	 * drift apart. She names no distance: a dowsed twitch is a direction, not a number.
	 */
	private ConditionalDialogOption follow() {
		return new ConditionalDialogOption(followKey, List.of(
				new ConditionalEntry(
						List.of(new MissingTowerNearCondition(QUEST_LOST_TOWER, MissingTowerManager.MAX_RADIUS)),
						List.of(),
						dialog("follow_up/near", "My dowsers say it's close — keep looking around here, on the surface. A treasure tower is not easy to miss.", THINK_ANIMS,
								option("follow_up/near/end", "I'll look around."))),
				new ConditionalEntry(List.of(), List.of(),
						dialog("follow_up/far", "Nothing. Not a twitch. It's not anywhere near here — Shou carried it further than she thinks. Follow the path she took.", THINK_ANIMS,
								option("follow_up/far/end", "I'll go further out.")))));
	}

	private SimpleDialogOption start() {
		return optionKey(startKey,
				dialog("start/dialog_1", "Shou lost her treasure tower again — set it down somewhere and walked off without it. She has been tearing the temple apart, and my dowsers have been twitching since dawn. Lend me a hand: find it and bring it back, and I'll make it worth your while.", THINK_ANIMS,
						optionKey(rejectKey,
								dialog("start/reject/dialog_1", "Tch. Fine. Shou and I will keep looking... after a nap.", List.of("dialog_decline"))),
						optionKey(acceptKey, new StartQuestAction(),
								dialog("start/accept/dialog_1", "Good. It is still out there, and it is above ground — Shou never buries anything. Follow the twitch and you'll have it.", List.of("dialog_agree"), optionKey(leaveKey)))));
	}

	/**
	 * The handover completes the quest, so the reputation is paid before the player picks a reward;
	 * the pick is a separate menu because the quest rewards carry no choice of their own.
	 */
	private SimpleDialogOption complete() {
		return optionKey(completeKey,
				dialog("complete/dialog_1", "Ha! That's Shou's tower — I'd know that gleam anywhere. She'll be insufferable about this. Hand it over.", AGREE_ANIMS,
						optionKey(handoverKey, new CompleteQuestAction(), reward())));
	}

	private Holder<Dialog> reward() {
		return dialog("complete/reward/dialog_1", "Much obliged. Now then — pick your payment. A little tower I put together myself, one of my dowsers, or a few stacks of coins? Choose one.", AGREE_ANIMS,
				option("complete/reward/tower", "The tower replica.", new GiveItemAction(GLItems.TREASURE_TOWER, 1),
						dialog("complete/reward/tower/dialog_1", "An ordinary wooden tower I infused with a spark of Shou's power. It holds only a fraction of the real thing — but a fraction of a treasure tower is still treasure. Don't lose it.", List.of("dialog_agree"), optionKey(leaveKey))),
				option("complete/reward/dowser", "One of your dowsers.", new GiveItemAction(GLItems.DOWSER, 1),
						dialog("complete/reward/dowser/dialog_1", "My spare dowser. It twitches for treasure, and only treasure. Take good care of it, or I'll know.", List.of("dialog_agree"), optionKey(leaveKey))),
				option("complete/reward/coins", "Four coin stacks.", new GiveItemAction(GLItems.COIN_STACK, 4),
						dialog("complete/reward/coins/dialog_1", "Four stacks, counted. Spend it however you like — I know I would.", List.of("dialog_agree"), optionKey(leaveKey))));
	}

	private void trades() {
		prefix("nazrin");
		// Above 50 reputation she will part with a spare dowser, but the iron is the price, not the tool.
		trade("process_dowser", new TradeOffer(GLEntities.NAZRIN.get(),
				List.of(new SelfReputationCondition(51)),
				new ItemStack(GLItems.DOWSER.get()),
				new TradeRecurrence(1, 24000),
				List.of(item(Items.IRON_INGOT, 6))));
	}

}
