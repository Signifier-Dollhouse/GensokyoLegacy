package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;

/**
 * Nazrin: a Myouren Temple resident with no trade of her own, so the greeting and the party line are
 * all she needs — her treasure comes out of the party ladder rather than a shop.
 */
public class NazrinQDGen extends QuestDialogData {

	public NazrinQDGen() {
		prefix("nazrin/chat");
		defaultDialog(GLEntities.NAZRIN.get(),
				"Hold still — my dowsers are twitching, and they never twitch for nothing.",
				"Back again? There is always something worth digging up around here.",
				"A feast? Fine, but only if the food is worth the trip.",
				"What do you need from me?");
	}

}
