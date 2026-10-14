package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Izayoi Sakuya: no structure of her own, so all of it is visit content gated on {@code visitingAt}
 *  (Kourindou, her only host). No quests: a guest can never hand out new work, so one would be dead content. */
public class SakuyaQDGen extends QuestDialogData {

	public SakuyaQDGen() {
		prefix("sakuya/chat");
		defaultDialog(GLEntities.IZAYOI_SAKUYA.get(),
				"May I help you?",
				"Nice to see you here",
				"Nice stuff. Care to make an offer?");

		chats();
		trades();
	}

	private void chats() {
		prefix("sakuya/visit_morichika_shop");
		chat("sakuya/visit_morichika_shop_greet", GLEntities.IZAYOI_SAKUYA.get(),
				List.of(visitingAt(GLStructureGen.MORICHIKA_SHOP)),
				starterText("greet", "Is it open? I heard this is the only shop in the forest."),
				dialog("greet", "It is. I have been here before - Rinnosuke's stock is the only thing in Gensokyo I have no objection to.",
						option("greet/end", "Understood.")),
				CHAT_DEFAULT);

	}

	private void trades() {
		prefix("sakuya");
		// The only thing she is ever on site to sell; cheap iron, sixteen a day because that is what a
		// maid can keep sharp, so the number is honest rather than balanced.
		trade("offer_iron_dagger", new TradeOffer(GLEntities.IZAYOI_SAKUYA.get(),
				List.of(visitingAt(GLStructureGen.MORICHIKA_SHOP)),
				new ItemStack(GLItems.IRON_DAGGER.get()),
				new TradeRecurrence(16, 24000), List.of(item(Items.IRON_INGOT, 4))));
	}

}