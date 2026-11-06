package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.rpg.condition.SelfReputationCondition;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Murasa Minamitsu: no structure of her own, so all of it is guest content — the greeting, the party
 * line, and the one thing a ship ghost can spare once the player is trusted.
 */
public class MinamitsuQDGen extends QuestDialogData {

	public MinamitsuQDGen() {
		prefix("minamitsu/chat");
		defaultDialog(GLEntities.MINAMITSU.get(),
				"You're on my deck now. State your business.",
				"A guest far from the sea. What do you want?",
				"A feast? Fine — I'll leave the ship for one night.",
				"What do you need from me?");
		trades();
	}

	private void trades() {
		prefix("minamitsu");
		// The anchor costs a whole anvil and a heart of the sea, so it is a weekly offer rather than
		// something a player can stock up on in one visit.
		trade("offer_anchor", new TradeOffer(GLEntities.MINAMITSU.get(),
				List.of(new SelfReputationCondition(80)),
				new ItemStack(GLItems.MINAMITSU_ANCHOR.get()),
				new TradeRecurrence(1, 168000),
				List.of(item(Items.CHAIN, 18), item(Items.ANVIL, 3), item(Items.HEART_OF_THE_SEA, 1))));
	}

}
