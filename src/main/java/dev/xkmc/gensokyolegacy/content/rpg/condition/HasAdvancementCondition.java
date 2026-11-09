package dev.xkmc.gensokyolegacy.content.rpg.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record HasAdvancementCondition(
		ResourceLocation advancement,
		boolean invert
) implements QuestCondition<HasAdvancementCondition> {

	public static final Codec<HasAdvancementCondition> CODEC = RecordCodecBuilder.create(i -> i.group(
			ResourceLocation.CODEC.fieldOf("advancement").forGetter(HasAdvancementCondition::advancement),
			Codec.BOOL.optionalFieldOf("invert", false).forGetter(HasAdvancementCondition::invert)
	).apply(i, HasAdvancementCondition::new));

	public HasAdvancementCondition(ResourceLocation advancement) {
		this(advancement, false);
	}

	@Override
	public Codec<HasAdvancementCondition> codec() {
		return CODEC;
	}

	@Override
	public boolean test(ServerPlayer pl, YoukaiEntity ch) {
		var server = pl.level().getServer();
		if (server == null) return false;
		var holder = server.getAdvancements().getAdvancement(advancement);
		if (holder == null) return invert;
		boolean done = pl.getAdvancements().getOrStartProgress(holder).isDone();
		return invert != done;
	}

}
