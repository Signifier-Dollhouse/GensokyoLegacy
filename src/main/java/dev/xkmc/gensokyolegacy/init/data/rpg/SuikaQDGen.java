package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;

/**
 * Ibuki Suika, the oni of the bamboo forest. She has no structure of her own — the mod
 * has no bamboo forest to put one in — so everything here is written for the visit system
 * and gated with {@code visitingAt}: the Hakurei shrine is the only house she ever stands
 * in, because it sits against the bamboo that is the edge of her own forest.
 * <p>
 * Deliberately near-empty. She has no chats, no trades and no quests yet — only the
 * greeting lines, because a character with no home still needs a {@code visit_greeting}
 * for a first-time player to see (see {@code doc/design/structure/character_visit.md} §9).
 * The topic list she is met with is therefore empty, which is the honest state of a
 * character whose content has not been written.
 * <p>
 * When content is added, every entry must be gated on
 * {@code visitingAt(GLStructureGen.HAKUREI_SHRINE)}: {@code homeBound()} content would be
 * dead — she can never be home-bound — and ungated content would leak into other hosts if
 * she ever visits a second one. No quests either, since the visit system hard-suppresses
 * {@code QuestHandle.Kind.START} for a guest.
 */
public class SuikaQDGen extends QuestDialogData {

	public SuikaQDGen() {
		prefix("suika/chat");
		// The trade line is empty: with no offers in the registry the trade topic is
		// never offered at all (ServerCharacterDialogManager#getInitialConversation),
		// so there is nothing for it to label.
		defaultDialog(GLEntities.IBUKI_SUIKA.get(),
				"Mm? Oh. Someone came all this way.",
				"Oh. It is you.",
				"Care to make an offer?");
	}

}
