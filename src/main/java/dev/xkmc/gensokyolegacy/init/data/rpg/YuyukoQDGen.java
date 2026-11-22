package dev.xkmc.gensokyolegacy.init.data.rpg;

import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;

/**
 * Saigyouji Yuyuko: no structure of her own, so all of it is guest content - the greeting and the
 * line she uses as a party guest. The visit line and the trade label stay empty, so both fall back.
 */
public class YuyukoQDGen extends QuestDialogData {

	public YuyukoQDGen() {
		prefix("yuyuko/chat");
		defaultDialog(GLEntities.SAIGYOUJI_YUYUKO.get(),
				"My, my~ I seem to have wandered quite far, and met such a lively friend.",
				"",
				"I never thought I'd receive an invitation from the outside world. "
						+ "Thank you for your hospitality.",
				"");
	}

}
