package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;

/**
 * Ibuki Suika: no structure of her own, so all of it is guest content - the shrine, her only host, and
 * the parties she turns up at. No quests: the visit system suppresses quest starts for a guest.
 * <p>
 * She has no home of her own, so the shrine uses the home greeting; the visit line covers her as a
 * guest elsewhere, and the party line is separate because a party guest greets differently.
 */
public class SuikaQDGen extends QuestDialogData {

	public SuikaQDGen() {
		prefix("suika/chat");
		// The trade line is empty: with no offers in the registry the trade topic is never offered at all
		// (ServerCharacterDialogManager#getInitialConversation), so there is nothing for it to label.
		defaultDialog(GLEntities.IBUKI_SUIKA.get(),
				"Welcome to my turf! Fancy a drinking contest?",
				"Oh? You came too. I'm enjoying a nice little buzz right now.",
				"Yo, I'm here for the feast — you'd better show me a good time.",
				"");
	}

}
