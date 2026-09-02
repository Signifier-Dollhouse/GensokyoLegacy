package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaChunkHolder;
import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectManager;
import dev.xkmc.gensokyolegacy.content.rpg.core.ServerCharacterDialogManager;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class GLMetaEventHandlers {

	@SubscribeEvent
	public static void reload(OnDatapackSyncEvent event) {
		if (event.getPlayer() == null) {
			ServerCharacterDialogManager.clearCache();
		}
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel sl)) return;
		AreaEffectManager.tickValidation(sl);
		AreaEffectManager.tickPendingFlush(sl);
	}

	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load event) {
		if (!(event.getChunk() instanceof LevelChunk chunk)) return;
		if (!(chunk.getLevel() instanceof ServerLevel sl)) return;
		String key = Long.toHexString(chunk.getPos().toLong());
		var att = GLMeta.LEVEL_EFFECT.type().getOrCreate(sl);
		var pendingIds = att.getPending().remove(key);
		if (pendingIds == null || pendingIds.isEmpty()) return;
		var holder = AreaChunkHolder.of(sl, chunk);
		for (var id : pendingIds) {
			if (!att.getById().containsKey(id)) continue;
			holder.addId(id);
		}
	}

	@SubscribeEvent
	public static void onChunkWatch(ChunkWatchEvent.Watch event) {
		ServerLevel sl = event.getLevel();
		ServerPlayer player = event.getPlayer();
		AreaEffectManager.onTrack(sl, event.getPos(), player);
	}

	@SubscribeEvent
	public static void onChunkUnwatch(ChunkWatchEvent.UnWatch event) {
		ServerLevel sl = event.getLevel();
		ServerPlayer player = event.getPlayer();
		AreaEffectManager.onUntrack(sl, event.getPos(), player);
	}

}
