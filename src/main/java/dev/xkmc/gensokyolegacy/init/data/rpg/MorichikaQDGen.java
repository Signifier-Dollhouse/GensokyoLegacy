package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.rpg.condition.HasAdvancementCondition;
import dev.xkmc.gensokyolegacy.content.rpg.condition.SelfReputationCondition;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeRecurrence;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLAdvGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.compat.patchouli.PatchouliHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class MorichikaQDGen extends QuestDialogData {

	public MorichikaQDGen() {
		prefix("morichika/chat");
		defaultDialog(GLEntities.MORICHIKA.get(),
				"Welcome to Kourindou! Feel free to look around.",
				"Good to see you again. Everything going alright?",
				"Let me see what you are selling?");
		starter("morichika/chat", new DialogStarter(GLEntities.MORICHIKA.get(), List.of(homeBound()),
				starterText("start", "A shop like this, out in the forest?"),
				dialog("hi", "Welcome to Kourindou! Let me know if anything catches your eye.", List.of("dialog_talk_03"),
						option("bye", "I'll browse the shelves."))
		));

		chats();
		trades();
	}

	private void chats() {
		prefix("morichika/chat_marisa");
		chat("morichika/chat_marisa", GLEntities.MORICHIKA.get(),
				List.of(homeBound(), missingAdv(GLAdvGen.ENTER_MARISA_HOUSE)),
				starterText("start", "Brewing potions is such a hassle…"),
				dialog("talk", "There's a magician deep in the Magical Forest. She loves mushrooms, magic tools, and borrowing things without asking — if brewing troubles you, go find her; she may make it easier for you.", List.of("dialog_talk_05"),
						option("where", "Where can I find her?",
								dialog("where_ans", "Follow the mushrooms — and the explosions. Tell her I sent you.", List.of("dialog_talk_02"),
										option("bye", "Thanks!")))),
				CHAT_INFO);

		prefix("morichika/chat_reimu");
		chat("morichika/chat_reimu", GLEntities.MORICHIKA.get(),
				List.of(homeBound(), missingAdv(GLAdvGen.ENTER_HAKUREI_SHRINE)),
				starterText("start", "It's dangerous out there…"),
				dialog("talk", "There's a shrine maiden in the cherry grove. If you feel short of ways to protect yourself, go and ask her for a talisman - it will keep you alive when danger comes.", List.of("dialog_talk_03"),
						option("donation", "Anything I should know?",
								dialog("donation_ans", "She's easygoing. As long as you don't do anything out of the ordinary, there are no real restrictions.", List.of("dialog_think"),
										option("bye", "Thanks!")))),
				CHAT_INFO);
	}

	private void trades() {
		prefix("morichika");
		// Restocking: the shop buys everyday supplies from the player. No gate: a shopkeeper
		// buys from anyone, and these are the goods a fresh player can actually gather.
		trade("sell_bread", GLEntities.MORICHIKA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(16, 24000), item(Items.BREAD, 12));
		trade("sell_potato", GLEntities.MORICHIKA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(16, 24000), item(Items.POTATO, 24));
		trade("sell_carrot", GLEntities.MORICHIKA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(16, 24000), item(Items.CARROT, 24));
		trade("sell_cooked_chicken", GLEntities.MORICHIKA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(16, 24000), item(Items.COOKED_CHICKEN, 4));
		trade("sell_cooked_porkchop", GLEntities.MORICHIKA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(16, 24000), item(Items.COOKED_PORKCHOP, 3));
		trade("sell_cooked_beef", GLEntities.MORICHIKA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(16, 24000), item(Items.COOKED_BEEF, 3));
		trade("sell_cooked_mutton", GLEntities.MORICHIKA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(16, 24000), item(Items.COOKED_MUTTON, 3));
		trade("sell_wool", GLEntities.MORICHIKA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(16, 24000), itemTag(ItemTags.WOOL, 6));
		trade("sell_leather", GLEntities.MORICHIKA.get(), new ItemStack(GLItems.COIN.get()),
				new TradeRecurrence(16, 24000), item(Items.LEATHER, 1));
		// Emerald is no longer our currency, but old worlds and villagers still have it, so the
		// shop exchanges it at the coin-stack rate. Both directions are lossless by design.
		trade("swap_emerald_to_coin_stack", GLEntities.MORICHIKA.get(),
				new ItemStack(GLItems.COIN_STACK.get()),
				new TradeRecurrence(16, 24000), item(Items.EMERALD, 8));
		trade("swap_coin_to_emerald", GLEntities.MORICHIKA.get(), new ItemStack(Items.EMERALD),
				new TradeRecurrence(16, 24000), item(GLItems.COIN.get(), 1));

		trade("offer_koishi_hat", new TradeOffer(GLEntities.MORICHIKA.get(),
				List.of(new HasAdvancementCondition(GLAdvGen.KOISHI_HAT)),
				new ItemStack(GLItems.KOISHI_HAT.get()),
				new TradeRecurrence(1, 168000), List.of(item(GLItems.COIN.get(), 32))));
		trade("offer_strange_glasses", new TradeOffer(GLEntities.MORICHIKA.get(),
				List.of(),
				new ItemStack(GLItems.STRANGE_GLASSES.get()),
				new TradeRecurrence(1, 24000), List.of(item(GLItems.COIN.get(), 8))));
		trade("offer_guide_book", new TradeOffer(GLEntities.MORICHIKA.get(),
				List.of(new HasAdvancementCondition(GLAdvGen.WELCOME)),
				PatchouliHelper.getBook(GensokyoLegacy.loc("tools_guide")),
				new TradeRecurrence(1, 24000), List.of(item(GLItems.COIN.get(), 1))));
		// The glove is a weapon, and worthless to someone who cannot yet throw a dagger,
		// so the gate is Sakuya's iron dagger advancement rather than the glove's own one.
		trade("offer_dagger_glove", new TradeOffer(GLEntities.MORICHIKA.get(),
				List.of(new HasAdvancementCondition(GLAdvGen.OBTAIN_IRON_DAGGER)),
				new ItemStack(GLItems.DAGGER_GLOVE.get()),
				new TradeRecurrence(1, 168000), List.of(item(GLItems.COIN.get(), 24))));
		// The head is a curiosity he has no reason to part with for a stranger, so it waits
		// until the shopkeeper likes the customer enough: rep 80 is inside the starting cap,
		// so it is reachable by gifts alone and no quest is needed to unlock it.
		trade("offer_sekibanki_head", new TradeOffer(GLEntities.MORICHIKA.get(),
				List.of(new SelfReputationCondition(80)),
				new ItemStack(GLItems.SEKIBANKI_HEAD.get()),
				new TradeRecurrence(1, 168000), List.of(item(GLItems.COIN.get(), 48))));
	}

}
