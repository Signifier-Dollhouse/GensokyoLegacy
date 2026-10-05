package dev.xkmc.gensokyolegacy.content.rpg.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Restricts a chat, quest or trade to characters in (or out of) a visit.
 * <p>
 * A visitor is the same {@code EntityType} as the resident, so nothing about the
 * entry itself distinguishes the two - only this condition does. Home-bound
 * content needs it because a guest is addressed differently and talks about
 * different things; visit-only content needs {@code invert}.
 */
public record HomeBoundCondition(
		boolean invert
) implements QuestCondition<HomeBoundCondition> {

	public static final MapCodec<HomeBoundCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("invert", false).forGetter(HomeBoundCondition::invert)
	).apply(i, HomeBoundCondition::new));

	/**
	 * Home-bound characters only.
	 */
	public HomeBoundCondition() {
		this(false);
	}

	@Override
	public MapCodec<HomeBoundCondition> codec() {
		return CODEC;
	}

	@Override
	public boolean test(ServerPlayer pl, YoukaiEntity ch) {
		return invert != ch.isVisiting();
	}
}