package dev.xkmc.gensokyolegacy.content.rpg.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.VisitModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Restricts content to a character visiting one particular home. {@link HomeBoundCondition} only says <em>whether</em>
 * she is a guest, but a guest says different things at different houses, so visit chat has to name the host. Visiting
 * is implied, so nothing also needs {@code visiting()}; {@link
 * dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey#CUSTOM} matches any built home.
 */
public record VisitStructureCondition(
		ResourceLocation structure
) implements QuestCondition<VisitStructureCondition> {

	public static final MapCodec<VisitStructureCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ResourceLocation.CODEC.fieldOf("structure").forGetter(VisitStructureCondition::structure)
	).apply(i, VisitStructureCondition::new));

	@Override
	public MapCodec<VisitStructureCondition> codec() {
		return CODEC;
	}

	@Override
	public boolean test(ServerPlayer pl, YoukaiEntity ch) {
		var host = ch.getModule(VisitModule.class).map(VisitModule::host).orElse(null);
		return host != null && host.structure().equals(structure);
	}
}