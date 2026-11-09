package dev.xkmc.gensokyolegacy.content.rpg.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

public record OtherReputationCondition(
		EntityType<?> character,
		int reputation
) implements QuestCondition<OtherReputationCondition> {

	public static final Codec<OtherReputationCondition> CODEC = RecordCodecBuilder.create(i -> i.group(
			BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("character").forGetter(OtherReputationCondition::character),
			Codec.INT.fieldOf("reputation").forGetter(OtherReputationCondition::reputation)
	).apply(i, OtherReputationCondition::new));

	@Override
	public Codec<OtherReputationCondition> codec() {
		return CODEC;
	}

	@Override
	public boolean test(ServerPlayer pl, YoukaiEntity ch) {
		return GLMeta.CHAR.type().getOrCreate(pl).getUnbounded(pl, character()).data().reputation >= reputation;
	}

}
