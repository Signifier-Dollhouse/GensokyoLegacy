package dev.xkmc.gensokyolegacy.content.rpg.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.attachment.index.IndexStorage;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Whether the player's lost tower for {@code quest} lies within {@code radius} of the character:
 * what her dowser can sense. A missing, carried or other-quest tower is never near.
 *
 * @see "doc/design/quest/missing_tower.md"
 */
public record MissingTowerNearCondition(
		ResourceLocation quest,
		int radius,
		boolean invert
) implements QuestCondition<MissingTowerNearCondition> {

	public static final MapCodec<MissingTowerNearCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ResourceLocation.CODEC.fieldOf("quest").forGetter(MissingTowerNearCondition::quest),
			Codec.INT.fieldOf("radius").forGetter(MissingTowerNearCondition::radius),
			Codec.BOOL.optionalFieldOf("invert", false).forGetter(MissingTowerNearCondition::invert)
	).apply(i, MissingTowerNearCondition::new));

	public MissingTowerNearCondition(ResourceLocation quest, int radius) {
		this(quest, radius, false);
	}

	@Override
	public MapCodec<MissingTowerNearCondition> codec() {
		return CODEC;
	}

	@Override
	public boolean test(ServerPlayer pl, YoukaiEntity ch) {
		var state = IndexStorage.get(pl.serverLevel()).getTower(pl.getUUID());
		boolean near = state != null && !state.carried() && quest.equals(state.quest())
				&& state.dim().equals(ch.level().dimension().location())
				&& state.pos().distSqr(ch.blockPosition()) <= (long) radius * radius;
		return invert != near;
	}

}
