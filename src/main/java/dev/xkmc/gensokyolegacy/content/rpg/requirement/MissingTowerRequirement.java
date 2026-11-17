package dev.xkmc.gensokyolegacy.content.rpg.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.attachment.index.IndexStorage;
import dev.xkmc.gensokyolegacy.content.attachment.index.MissingTowerManager;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestData;
import dev.xkmc.gensokyolegacy.content.rpg.trigger.EmptyTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The tower is rolled near the giver on start; completion requires carrying it back.
 *
 * @see "doc/design/quest/missing_tower.md"
 */
public record MissingTowerRequirement(
		ResourceLocation quest,
		String text
) implements QuestRequirement<MissingTowerRequirement, EmptyTrigger> {

	public static final MapCodec<MissingTowerRequirement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ResourceLocation.CODEC.fieldOf("quest").forGetter(MissingTowerRequirement::quest),
			Codec.STRING.fieldOf("text").forGetter(MissingTowerRequirement::text)
	).apply(i, MissingTowerRequirement::new));

	@Override
	public MapCodec<MissingTowerRequirement> codec() {
		return CODEC;
	}

	@Override
	public Class<EmptyTrigger> getTrigger() {
		return EmptyTrigger.class;
	}

	@Override
	public void start(QuestData data, ServerPlayer sp, @Nullable YoukaiEntity ch, String key) {
		ServerLevel level = ch != null && ch.level() instanceof ServerLevel sl ? sl : sp.serverLevel();
		BlockPos origin = ch != null ? ch.blockPosition() : sp.blockPosition();
		IndexStorage.get(level).createTower(sp.getUUID(), quest, level, origin);
	}

	@Override
	public boolean canComplete(Player player, QuestData data, String key) {
		// the client only has the synced progress, the server also checks the record
		if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level))
			return true;
		var state = IndexStorage.get(level).getTower(sp.getUUID());
		return state != null && state.carried() && quest.equals(state.quest());
	}

	@Override
	public int getMaxProgress() {
		return 1;
	}

	@Override
	public void doComplete(ServerPlayer sp, QuestData data, String key) {
		MissingTowerManager.complete(sp);
	}

	@Override
	public List<Component> getDesc(Player player, QuestData data, String key) {
		int progress = data.progress.getOrDefault(key, 0);
		return List.of(Component.literal("- ").append(Component.translatable(text)).append(": ")
				.append(Component.literal("" + progress).withStyle(progress > 0 ? ChatFormatting.GREEN : ChatFormatting.RED))
				.append("/").append(Component.literal("1").withStyle(ChatFormatting.AQUA)));
	}

}
