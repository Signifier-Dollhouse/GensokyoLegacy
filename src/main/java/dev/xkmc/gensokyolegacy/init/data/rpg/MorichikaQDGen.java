package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.rpg.condition.HasAdvancementCondition;
import dev.xkmc.gensokyolegacy.content.rpg.condition.HasQuestCompletedCondition;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLAdvGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.compat.patchouli.PatchouliHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class MorichikaQDGen extends QuestDialogData {

	public MorichikaQDGen() {
		prefix("morichika/chat");
		defaultDialog(GLEntities.MORICHIKA.get(),
				"Welcome to Kourindou! Feel free to look around.",
				"Any special offers for a special customer?");
		starter("morichika/chat", new DialogStarter(GLEntities.MORICHIKA.get(), List.of(),
				starterText("start", "Welcome!"),
				dialog("hi", "Welcome to Kourindou! Let me know if anything catches your eye.",
						option("bye", "Bye!"))
		));

		chats();
		trades();
	}

	private void chats() {
		prefix("morichika/chat_marisa");
		chat("morichika/chat_marisa", GLEntities.MORICHIKA.get(),
				List.of(missingAdv(GLAdvGen.ENTER_MARISA_HOUSE)),
				starterText("start", "Have you met Marisa?"),
				dialog("talk", "Marisa Kirisame, the ordinary magician. Her house is deep in the Magical Forest. She loves mushrooms, magic tools, and borrowing things without asking.",
						option("where", "Where can I find her?",
								dialog("where_ans", "Follow the mushrooms — and the explosions. Tell her Rinnosuke sent you.",
										option("bye", "Thanks!")))),
				CHAT_INFO);

		prefix("morichika/chat_reimu");
		chat("morichika/chat_reimu", GLEntities.MORICHIKA.get(),
				List.of(missingAdv(GLAdvGen.ENTER_HAKUREI_SHRINE)),
				starterText("start", "Have you met Reimu?"),
				dialog("talk", "Reimu Hakurei, the shrine maiden of the Hakurei Shrine in the cherry grove. If raiders trouble you, she's the one to see.",
						option("donation", "Anything I should know?",
								dialog("donation_ans", "Just don't forget a donation. A shrine maiden with an empty donation box is a grumpy shrine maiden.",
										option("bye", "Thanks!")))),
				CHAT_INFO);
	}

	private void trades() {
		prefix("morichika");
		trade("offer_koishi_hat", new TradeOffer(GLEntities.MORICHIKA.get(),
				List.of(new HasAdvancementCondition(GLAdvGen.KOISHI_HAT)),
				new ItemStack(GLItems.KOISHI_HAT.get()),
				new TradeRecurrence(1, 168000), List.of(item(Items.EMERALD, 32))));
		trade("offer_strange_glasses", new TradeOffer(GLEntities.MORICHIKA.get(),
				List.of(),
				new ItemStack(GLItems.STRANGE_GLASSES.get()),
				new TradeRecurrence(1, 24000), List.of(item(Items.EMERALD, 8))));
		trade("offer_doll_glove", new TradeOffer(GLEntities.MORICHIKA.get(),
				List.of(new HasQuestCompletedCondition(MarisaQDGen.QUEST_TALISMAN_REQUEST)),
				new ItemStack(GLItems.DOLL_GLOVE.get()),
				new TradeRecurrence(1, 168000), List.of(item(Items.EMERALD, 32))));
		trade("offer_guide_book", new TradeOffer(GLEntities.MORICHIKA.get(),
				List.of(new HasAdvancementCondition(GLAdvGen.WELCOME)),
				PatchouliHelper.getBook(GensokyoLegacy.loc("tools_guide")),
				new TradeRecurrence(1, 24000), List.of(item(Items.EMERALD, 1))));
	}

}
