package dev.xkmc.gensokyolegacy.content.rpg.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestData;
import dev.xkmc.gensokyolegacy.content.rpg.trigger.KillTrigger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public record KillMobRequirement(
		String text, ResourceLocation target, int count
) implements QuestRequirement<KillMobRequirement, KillTrigger> {

	public static final Codec<KillMobRequirement> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("text").forGetter(KillMobRequirement::text),
			ResourceLocation.CODEC.fieldOf("target").forGetter(KillMobRequirement::target),
			Codec.INT.fieldOf("count").forGetter(KillMobRequirement::count)
	).apply(i, KillMobRequirement::new));

	public KillMobRequirement(String text, TagKey<EntityType<?>> type, int count) {
		this(text, type.location(), count);
	}

	@Override
	public Codec<KillMobRequirement> codec() {
		return CODEC;
	}

	@Override
	public Class<KillTrigger> getTrigger() {
		return KillTrigger.class;
	}

	@Override
	public int match(KillTrigger trigger) {
		return trigger.target().getType().is(TagKey.create(ForgeRegistries.Keys.ENTITY_TYPES, target)) ? 1 : 0;
	}

	@Override
	public List<Component> getDesc(Player player, QuestData data, String key) {
		int progress = data.progress.getOrDefault(key, 0);
		return List.of(Component.literal("- ").append(Component.translatable(text)).append(": ")
				.append(Component.literal("" + progress).withStyle(progress == count ? ChatFormatting.GREEN : ChatFormatting.RED))
				.append("/").append(Component.literal("" + count).withStyle(ChatFormatting.AQUA)));
	}

	@Override
	public int getMaxProgress() {
		return count;
	}

}
