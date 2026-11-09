package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaChunkHolder;
import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectManager;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyGiftReader;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyTicker;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.FoodResultCache;
import dev.xkmc.gensokyolegacy.content.attachment.party.network.PartyGiftToClient;
import dev.xkmc.gensokyolegacy.content.cooldown.CooldownClock;
import dev.xkmc.gensokyolegacy.content.rpg.core.ServerCharacterDialogManager;
import dev.xkmc.gensokyolegacy.content.rpg.network.QuestLootToClient;
import dev.xkmc.gensokyolegacy.content.rpg.reward.LootReader;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class GLMetaEventHandlers {

	@SubscribeEvent
	public static void reload(OnDatapackSyncEvent event) {
		var sp = event.getPlayer();
		if (sp == null) {
			ServerCharacterDialogManager.clearCache();
			FoodResultCache.invalidate();
		} else {
			var server = sp.getServer();
			if (server != null) {
				GensokyoLegacy.HANDLER.toClientPlayer(new PartyGiftToClient(PartyGiftReader.all(server)), sp);
				GensokyoLegacy.HANDLER.toClientPlayer(new QuestLootToClient(LootReader.questRewards(server)), sp);
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerTickPre(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		Player player = event.player;
		// the client ticks other players too, and only the local one has cooldowns that matter
		if (player.level().isClientSide() && !player.isLocalPlayer()) return;
		CooldownClock.advance(player);
	}

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		if (!(event.level instanceof ServerLevel sl)) return;
		AreaEffectManager.tickValidation(sl);
		AreaEffectManager.tickPendingFlush(sl);
		PartyTicker.tick(sl);
	}

	/**
	 * A party lives on its chunk, so a level going away takes the whole list with it.
	 */
	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel sl) PartyTicker.clear(sl);
	}

	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load event) {
		if (!(event.getChunk() instanceof LevelChunk chunk)) return;
		if (!(chunk.getLevel() instanceof ServerLevel sl)) return;
		// A party lives on its chunk and outlives the server, but the ticker entry does not: only PartySend fills
		// ACTIVE, so a restored party has state with nothing to advance it. Re-register on chunk load, post-NBT read.
		var party = PartyAttachment.of(sl, chunk.getPos());
		if (party != null && party.active()) PartyTicker.register(sl, chunk.getPos());
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
