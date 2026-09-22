package dev.xkmc.gensokyolegacy.content.rpg.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.server.level.ServerPlayer;

public record TimerCondition(
		String key,
		boolean invert
) implements QuestCondition<TimerCondition> {

	public static final MapCodec<TimerCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("key").forGetter(TimerCondition::key),
			Codec.BOOL.optionalFieldOf("invert", false).forGetter(TimerCondition::invert)
	).apply(i, TimerCondition::new));

	public TimerCondition(String key) {
		this(key, false);
	}

	@Override
	public MapCodec<TimerCondition> codec() {
		return CODEC;
	}

	@Override
	public boolean test(ServerPlayer pl, YoukaiEntity ch) {
		var data = GLMeta.CHAR.type().getOrCreate(pl).get(pl, ch).data();
		boolean available = data.isAvailable(key, pl.level().getGameTime());
		return invert != available;
	}

}
