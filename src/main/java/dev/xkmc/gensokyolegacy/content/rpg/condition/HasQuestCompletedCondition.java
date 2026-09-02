package dev.xkmc.gensokyolegacy.content.rpg.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record HasQuestCompletedCondition(
		ResourceLocation quest
) implements QuestCondition<HasQuestCompletedCondition> {

	public static final MapCodec<HasQuestCompletedCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ResourceLocation.CODEC.fieldOf("quest").forGetter(HasQuestCompletedCondition::quest)
	).apply(i, HasQuestCompletedCondition::new));

	@Override
	public MapCodec<HasQuestCompletedCondition> codec() {
		return CODEC;
	}

	@Override
	public boolean test(ServerPlayer pl, YoukaiEntity ch) {
		return GLMeta.QUEST.type().getOrCreate(pl).getData(quest).completed > 0;
	}

}
