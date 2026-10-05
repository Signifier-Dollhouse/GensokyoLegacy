package dev.xkmc.gensokyolegacy.content.rpg.action;

import com.mojang.serialization.MapCodec;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;

public record StartQuestAction() implements DialogAction<StartQuestAction> {

	public static final MapCodec<StartQuestAction> CODEC = MapCodec.unit(new StartQuestAction());

	@Override
	public void execute(ActionContext context) {
		var quest = context.quest();
		if (quest.isEmpty()) return;
		// A visitor never hands out new work. The topic list already withholds
		// it; this covers a click that raced her window closing.
		if (context.character().isVisiting()) return;
		GLMeta.QUEST.type().getOrCreate(context.sp()).start(context.sp(), quest.get());
	}

	@Override
	public MapCodec<StartQuestAction> codec() {
		return CODEC;
	}

}
