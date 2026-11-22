package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.attachment.index.MissingTowerManager;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Keeps ground towers alive with their chunk and re-syncs quest progress on login.
 *
 * @see "doc/design/quest/missing_tower.md"
 */
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class MissingTowerEventHandlers {

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel level)) return;
		MissingTowerManager.postTick(level);
		if (level != level.getServer().overworld()) return;
		if (level.getGameTime() % 20 != 0) return;
		MissingTowerManager.tick(level);
	}

	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel level)
			MissingTowerManager.onChunkLoad(level, event.getChunk().getPos());
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			MissingTowerManager.syncProgress(player);
	}

}
