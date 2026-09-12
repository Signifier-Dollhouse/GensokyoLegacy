package dev.xkmc.gensokyolegacy.content.rpg.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestData;
import dev.xkmc.gensokyolegacy.content.rpg.trigger.RaidTrigger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record RaidVictoryRequirement(
		String text, int count
) implements QuestRequirement<RaidVictoryRequirement, RaidTrigger> {

	public static final MapCodec<RaidVictoryRequirement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("text").forGetter(RaidVictoryRequirement::text),
			Codec.INT.fieldOf("count").forGetter(RaidVictoryRequirement::count)
	).apply(i, RaidVictoryRequirement::new));

	@Override
	public MapCodec<RaidVictoryRequirement> codec() {
		return CODEC;
	}

	@Override
	public Class<RaidTrigger> getTrigger() {
		return RaidTrigger.class;
	}

	@Override
	public int match(RaidTrigger trigger) {
		return 1;
	}

	@Override
	public int getMaxProgress() {
		return count;
	}

	@Override
	public List<Component> getDesc(Player player, QuestData data, String key) {
		int progress = data.progress.getOrDefault(key, 0);
		return List.of(Component.literal("- ").append(Component.translatable(text)).append(": ")
				.append(Component.literal("" + progress).withStyle(progress == count ? ChatFormatting.GREEN : ChatFormatting.RED))
				.append("/").append(Component.literal("" + count).withStyle(ChatFormatting.AQUA)));
	}

}