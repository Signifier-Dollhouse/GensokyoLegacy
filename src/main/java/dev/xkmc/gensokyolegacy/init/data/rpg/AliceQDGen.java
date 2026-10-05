package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogStarter;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;

import java.util.List;

/**
 * Alice has no home of her own yet - her bed binds to any custom room - so she
 * has no quests and no stock. What she does have is a habit of turning up at
 * Marisa's, which is the only place the visitor feature needs her for: without
 * a greeting and one chat she would show up with an empty topic list.
 */
public class AliceQDGen extends QuestDialogData {

	public AliceQDGen() {
		prefix("alice");
		defaultDialog(GLEntities.ALICE.get(),
				"...Oh. A visitor. Welcome.",
				"...Oh. Hello. I did not mean to startle you.",
				"Would you like to look around?");

		starter("alice/chat", new DialogStarter(GLEntities.ALICE.get(), List.of(homeBound()),
				starterText("start", "Are the dolls behaving?"),
				dialog("hi", "They are as well-behaved as they ever are, which is to say: only because I am watching. One of them bit a customer last month.",
						option("hi/end", "Charming."))));

		prefix("alice/visit");
		chat("alice/visit_marisa", GLEntities.ALICE.get(),
				List.of(visiting()),
				starterText("marisa", "Is Marisa in today?"),
				dialog("marisa", "...She is at her own house, I am afraid. I came for the mushrooms — the big ones she keeps in the east room. May I look?",
						option("look", "Of course. Knock yourself out.",
								dialog("look_ans", "Thank you. ...Please do not tell her I came for the mushrooms rather than for her. She worries.",
										option("look/end", "Your secret's safe.")))),
				CHAT_MISC);
	}
}