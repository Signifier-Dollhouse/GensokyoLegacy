package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Izayoi Sakuya: no structure of her own, so all of it is visit content gated on {@code visitingAt}
 * (Kourindou, her only host). No quests: a guest can never hand out new work, so one would be dead content.
 */
public class SakuyaQDGen extends QuestDialogData {

	public SakuyaQDGen() {
		prefix("sakuya/chat");
		defaultDialog(GLEntities.IZAYOI_SAKUYA.get(),
				"May I help you?",
				"Nice to see you here",
				"This time I'm the one being entertained. If you're short-handed, I can teach you how to raise an excellent maid one day.",
				"Does the Scarlet Devil Mansion sell anything to outsiders?");

		chats();
		trades();
	}

	private void chats() {
		prefix("sakuya/visit_morichika_shop");
		chat("sakuya/visit_morichika_shop_greet", GLEntities.IZAYOI_SAKUYA.get(),
				List.of(visitingAt(GLStructureGen.MORICHIKA_SHOP)),
				starterText("greet", "Does the mansion do its shopping here as well?"),
				dialog("greet", "The young lady's requests are sometimes more than an ordinary shop can satisfy. One always finds something worthwhile here, if the goods are unusual.", TALK_ANIMS,
						option("greet/end", "Understood.")),
				CHAT_DEFAULT);

	}

	private void trades() {
		prefix("sakuya");
		// The only thing she is ever on site to sell; cheap iron, sixteen a day because that is what a
		// maid can keep sharp, so the number is honest rather than balanced.
		trade("offer_iron_dagger", new TradeOffer(GLEntities.IZAYOI_SAKUYA.get(),
				List.of(),
				new ItemStack(GLItems.IRON_DAGGER.get()),
				new TradeRecurrence(16, 24000), List.of(item(Items.IRON_INGOT, 4))));
	}

}