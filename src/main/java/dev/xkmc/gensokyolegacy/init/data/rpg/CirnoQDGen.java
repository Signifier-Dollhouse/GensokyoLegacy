package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;

import java.util.List;

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
		chat("cirno/chat", GLEntities.CIRNO.get(), List.of(homeBound()),
				starterText("start", "Your collection is... rather unique."),
				dialog("hi", "These frogs are the proof of my training! Don't worry, I'll thaw them out. They've only been frozen for... a few days now?", THINK_ANIMS_ALL,
						option("hi/end", "I'm a little worried about these little guys' future.")),
				1);
	}

}
