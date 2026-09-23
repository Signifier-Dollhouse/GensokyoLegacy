package dev.xkmc.gensokyolegacy.content.rpg.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestData;
import dev.xkmc.gensokyolegacy.content.rpg.trigger.KillTrigger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record KillEnemyRequirement(
		String text, int count
) implements QuestRequirement<KillEnemyRequirement, KillTrigger> {

	public static final MapCodec<KillEnemyRequirement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("text").forGetter(KillEnemyRequirement::text),
			Codec.INT.fieldOf("count").forGetter(KillEnemyRequirement::count)
	).apply(i, KillEnemyRequirement::new));

	@Override
	public MapCodec<KillEnemyRequirement> codec() {
		return CODEC;
	}

	@Override
	public Class<KillTrigger> getTrigger() {
		return KillTrigger.class;
	}

	@Override
	public int match(KillTrigger trigger) {
		return trigger.target() instanceof Enemy ? 1 : 0;
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
