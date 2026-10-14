package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;

/** Ibuki Suika: no structure of her own, so all of it is visit content gated on {@code visitingAt} - the shrine,
 *  her only host. No quests: the visit system suppresses quest starts for a guest. Otherwise empty on purpose:
 *  a character with no home still needs a {@code visit_greeting}, while {@code homeBound()} would be dead and
 *  ungated content would leak to other hosts. */
public class SuikaQDGen extends QuestDialogData {

	public SuikaQDGen() {
		prefix("suika/chat");
		// The trade line is empty: with no offers in the registry the trade topic is never offered at all
		// (ServerCharacterDialogManager#getInitialConversation), so there is nothing for it to label.
		defaultDialog(GLEntities.IBUKI_SUIKA.get(),
				"Mm? Oh. Someone came all this way.",
				"Oh. It is you.",
				"Care to make an offer?");
	}

}
