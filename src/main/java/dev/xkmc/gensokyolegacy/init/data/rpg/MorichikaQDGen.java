package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.rpg.condition.HasAdvancementCondition;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.data.GLAdvGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
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

		trades();
	}

	private void trades() {
		prefix("morichika");
		trade("offer_koishi_hat", new TradeOffer(GLEntities.MORICHIKA.get(),
				List.of(new HasAdvancementCondition(GLAdvGen.KOISHI_HAT)),
				new ItemStack(GLItems.KOISHI_HAT.get()),
				new TradeRecurrence(1, 168000), List.of(item(Items.EMERALD, 32))));
	}

}
