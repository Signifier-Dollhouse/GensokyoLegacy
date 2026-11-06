package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;

/**
 * Cirno: her ice nest is a home she can be met at, and she also turns up as a party guest, where she
 * greets the player differently.
 */
public class CirnoQDGen extends QuestDialogData {

	public CirnoQDGen() {
		prefix("cirno/chat");
		defaultDialog(GLEntities.CIRNO.get(),
				"Did you come to play with me?",
				"I'm here to play!",
				"I heard you're treating? Then I won't hold back!",
				"(Trade)");
	}

}
