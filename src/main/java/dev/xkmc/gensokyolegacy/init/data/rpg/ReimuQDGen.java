package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.rpg.action.CompleteQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.StartQuestAction;
import dev.xkmc.gensokyolegacy.content.rpg.condition.SelfReputationCondition;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestRecurrence;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.KillMobRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.SubmitItemRequirement;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ExpReward;
import dev.xkmc.gensokyolegacy.content.rpg.reward.ReputationReward;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.registrate.GLBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class ReimuQDGen extends QuestDialogData {

	public ReimuQDGen() {

		prefix("reimu/chat");
		defaultDialog(GLEntities.REIMU.get(),
				"Hi! What brings you to the shrine?",
				"I'd like to trade with you!");
		starter("reimu/chat", new DialogStarter(GLEntities.REIMU.get(), List.of(),
				starterText("start", "Hi!"),
				dialog("hi", "Hi!", option("bye", "Bye!"))
		));

		prefix("reimu/kill_zombie");
		quest("reimu/kill_zombie", new Quest(GLEntities.REIMU.get(),
				List.of(new SelfReputationCondition(-100)),
				questTitle("Zombie Slayer"), questDesc("Kill some zombies and collect rotten flesh for Reimu"),
				Optional.of(new QuestRecurrence(1000)),
				new TreeMap<>(Map.of(
						"a-kill", new KillMobRequirement(reqText("kill", "Kill zombies"), EntityTypeTags.ZOMBIES, 10),
						"b-item", new SubmitItemRequirement(List.of(item(Items.ROTTEN_FLESH, 10)))
				)),
				List.of(
						new ExpReward(100),
						new ReputationReward(50, 100, 10, 200),
						loot("reimu/kill_zombie", LootTable.lootTable()
								.withPool(lootItem(Items.BOOK, 2)))
				),
				option("start", "Anything I can help with?", dialog(
						"start/dialog_1", "Could you help me to kill some zombies?",
						option("start/reject", "Maybe next time"),
						option("start/accept", "Sure!", new StartQuestAction())
				)),
				option("follow_up", "I'm half way through killing zombies!", dialog(
						"follow_up/dialog_1", "Thanks for your effort! I need more zombies killed",
						option("follow_up/end", "Let me kill a few more")
				)),
				option("complete", "I killed all the zombies!", dialog(
						"complete/dialog_1", "Thanks! Do you have flesh with you?",
						option("complete/reject", "Maybe next time"),
						option("complete/handover", "Sure!", new CompleteQuestAction())
				))
		));

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

	}


}
