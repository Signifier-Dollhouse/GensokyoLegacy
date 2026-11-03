package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;

/**
 * Ibuki Suika: no structure of her own, so all of it is guest content - the shrine, her only host, and
 * the parties she turns up at. No quests: the visit system suppresses quest starts for a guest.
 * <p>
 * The greeting is the home line even though she has no home: a visitor to the shrine is not
 * {@code isVisiting()}, so an empty {@code visit_greeting} falls back to it, and one line serves both.
 * Her party line is separate, because a party guest greets differently.
 */
public class SuikaQDGen extends QuestDialogData {

	public SuikaQDGen() {
		prefix("suika/chat");
		// The trade line is empty: with no offers in the registry the trade topic is never offered at all
		// (ServerCharacterDialogManager#getInitialConversation), so there is nothing for it to label.
		defaultDialog(GLEntities.IBUKI_SUIKA.get(),
				"Oh? You came too. I'm enjoying a nice little buzz right now.",
				"",
				"Yo, I'm here for the feast — you'd better show me a good time.",
				"");
	}

}
