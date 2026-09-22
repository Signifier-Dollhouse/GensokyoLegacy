package dev.xkmc.gensokyolegacy.content.rpg.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientList;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import dev.xkmc.gensokyolegacy.util.InventoryMapper;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public record HasItemCondition(
		List<IngredientEntry> ingredients
) implements QuestCondition<HasItemCondition>, IngredientList {

	public static final MapCodec<HasItemCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IngredientEntry.CODEC.listOf().fieldOf("ingredients").forGetter(HasItemCondition::ingredients)
	).apply(i, HasItemCondition::new));

	@Override
	public MapCodec<HasItemCondition> codec() {
		return CODEC;
	}

	@Override
	public boolean test(ServerPlayer pl, YoukaiEntity ch) {
		return InventoryMapper.testCached(pl, this);
	}

}
