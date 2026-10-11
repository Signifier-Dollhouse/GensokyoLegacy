package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaChunkHolder;
import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectManager;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyTicker;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.FoodResultCache;
import dev.xkmc.gensokyolegacy.content.attachment.party.item.PartyWandComponent;
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
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class GLMetaEventHandlers {

	@SubscribeEvent
	public static void reload(OnDatapackSyncEvent event) {
		if (event.getPlayer() == null) {
			ServerCharacterDialogManager.clearCache();
			// Nothing here is required. The party_food override is deliberately resolved
			// by the caller in PartyFoodValue and never enters the cache, so a reload
			// cannot leave a stale appeal behind. Kept as belt-and-braces: the cache is
			// keyed on BlockState alone, and if a derivation ever grows a datapack input
			// this is what stops it going stale.
			FoodResultCache.invalidate();
		}
	}

	/** Vanilla-registry entries the mod adds that Registrate does not own. */
	@SubscribeEvent
	public static void register(RegisterEvent event) {
		PartyWandComponent.register(event);
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel sl)) return;
		AreaEffectManager.tickValidation(sl);
		AreaEffectManager.tickPendingFlush(sl);
		PartyTicker.tick(sl);
	}

	/** A party lives on its chunk, so a level going away takes the whole list with it. */
	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel sl) PartyTicker.clear(sl);
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
