package dev.xkmc.gensokyolegacy.content.rpg.action;

import com.mojang.serialization.Codec;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;

public record CompleteQuestAction() implements DialogAction<CompleteQuestAction> {

	public static final Codec<CompleteQuestAction> CODEC = Codec.unit(new CompleteQuestAction());

	@Override
	public void execute(ActionContext context) {
		var quest = context.quest();
		if (quest.isEmpty()) return;
		GLMeta.QUEST.type().getOrCreate(context.sp()).complete(context.sp(), quest.get(), context.character());
	}

	@Override
	public Codec<CompleteQuestAction> codec() {
		return CODEC;
	}

}
