package dev.xkmc.gensokyolegacy.content.rpg.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Restricts a chat, quest or trade to characters who are (or are not) guests at somebody's party. A party guest is the
 * same {@code EntityType} as the resident, so only this condition tells them apart; home-bound content needs {@code
 * invert}, the same shape as {@link HomeBoundCondition}.
 */
public record PartyCondition(
		boolean invert
) implements QuestCondition<PartyCondition> {

	public static final MapCodec<PartyCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("invert", false).forGetter(PartyCondition::invert)
	).apply(i, PartyCondition::new));

	/**
	 * Party guests only.
	 */
	public PartyCondition() {
		this(false);
	}

	@Override
	public MapCodec<PartyCondition> codec() {
		return CODEC;
	}

	@Override
	public boolean test(ServerPlayer pl, YoukaiEntity ch) {
		return invert != ch.isPartying();
	}

}
