package dev.xkmc.gensokyolegacy.content.rpg.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Composite condition that passes as soon as one of its nested conditions
 * passes. Condition lists themselves are ANDed (see {@code GatedEntry#match}),
 * so this is the only way to write "either A or B" - e.g. Marisa's talisman
 * errand is offered once the player is known to Reimu <i>or</i> to Alice.
 *
 * <p>An empty list never passes, matching the "no requirement" reading of an
 * empty AND list rather than "always satisfied".
 */
public record AnyCondition(
		List<QuestCondition<?>> conditions
) implements QuestCondition<AnyCondition> {

	public static final MapCodec<AnyCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			CodecRegistry.CONDITION.codec().listOf().fieldOf("conditions").forGetter(AnyCondition::conditions)
	).apply(i, AnyCondition::new));

	@Override
	public MapCodec<AnyCondition> codec() {
		return CODEC;
	}

	@Override
	public boolean test(ServerPlayer pl, YoukaiEntity ch) {
		for (var c : conditions()) {
			if (c.test(pl, ch))
				return true;
		}
		return false;
	}

}
