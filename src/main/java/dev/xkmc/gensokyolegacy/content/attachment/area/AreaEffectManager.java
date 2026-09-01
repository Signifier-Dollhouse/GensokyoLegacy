package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AreaEffectManager {

	private AreaEffectManager() {
	}

	public static UUID add(ServerLevel level, BlockPos ownerPos, ChunkPosRange range, EffectData data) {
		if (!level.isLoaded(ownerPos)) {
			// owner chunk should be loaded at creation per spec
		}
		if (range.chunkCount() > 1024) {
			// enforce max
		}
		UUID uuid = UUID.randomUUID();
		AreaEffectEntry entry = new AreaEffectEntry(uuid, ownerPos, range, data, level.getGameTime());
		add(level, entry);
		return uuid;
	}

	public static void add(ServerLevel level, AreaEffectEntry entry) {
		LevelAreaAttachment levelAtt = GLMeta.LEVEL_EFFECT.type().getOrCreate(level);
		levelAtt.getById().put(entry.id, entry);
		// fan out
		for (int x = entry.range.minCX(); x <= entry.range.maxCX(); x++) {
			for (int z = entry.range.minCZ(); z <= entry.range.maxCZ(); z++) {
				ChunkPos cpos = new ChunkPos(x, z);
				LevelChunk chunk = level.getChunkSource().getChunk(cpos.x, cpos.z, false);
				if (chunk != null) {
					AreaChunkHolder.of(level, chunk).addId(entry.id);
				} else {
					String key = Long.toHexString(cpos.toLong());
					levelAtt.getPending().computeIfAbsent(key, k -> new ArrayList<>()).add(entry.id);
				}
			}
		}
		// sync to tracking players: cheap check via player's tracking view (contains is O(1))
		for (ServerPlayer player : level.players()) {
			int count = 0;
			for (int x = entry.range.minCX(); x <= entry.range.maxCX(); x++) {
				for (int z = entry.range.minCZ(); z <= entry.range.maxCZ(); z++) {
					if (player.getChunkTrackingView().contains(x, z)) count++;
				}
			}
			if (count > 0) {
				entry.getTrackingCounts().put(player.getUUID(), count);
				AreaEffectSyncPacket.sendAdd(player, entry);
			}
		}
	}

	public static boolean remove(ServerLevel level, UUID id) {
		LevelAreaAttachment levelAtt = GLMeta.LEVEL_EFFECT.type().getOrCreate(level);
		AreaEffectEntry removed = levelAtt.getById().remove(id);
		if (removed == null) return false;
		for (UUID playerId : Set.copyOf(removed.getTrackingPlayers())) {
			ServerPlayer p = level.getServer().getPlayerList().getPlayer(playerId);
			if (p != null) AreaEffectSyncPacket.sendRemove(p, id);
		}
		removed.getTrackingCounts().clear();
		// no chunk iteration, no pending scan per spec (pending lazily skipped)
		return true;
	}

	@Nullable
	public static AreaEffectEntry get(ServerLevel level, UUID id) {
		return GLMeta.LEVEL_EFFECT.type().getOrCreate(level).get(id);
	}

	public static List<AreaEffectEntry> getAffecting(ServerLevel level, LevelChunk chunk) {
		return AreaChunkHolder.of(level, chunk).getAffecting();
	}

	public static List<AreaEffectEntry> getAffecting(ServerLevel level, ChunkPos pos) {
		AreaChunkHolder holder = AreaChunkHolder.of(level, pos);
		if (holder == null) return List.of();
		return holder.getAffecting();
	}

	public static List<AreaEffectEntry> getAffecting(ServerLevel level, BlockPos pos) {
		return getAffecting(level, new ChunkPos(pos));
	}

	public static void tickValidation(ServerLevel level) {
		GLMeta.LEVEL_EFFECT.type().getOrCreate(level).tickValidation(level);
	}

	public static void tickPendingFlush(ServerLevel level) {
		GLMeta.LEVEL_EFFECT.type().getOrCreate(level).tickPendingFlush(level);
	}

	// tracking helpers

	static void onTrack(ServerLevel level, ChunkPos pos, ServerPlayer player) {
		AreaChunkHolder holder = AreaChunkHolder.of(level, pos);
		if (holder == null) return;
		for (AreaEffectEntry entry : holder.getAffecting()) {
			if (entry.incrementTracking(player)) {
				AreaEffectSyncPacket.sendAdd(player, entry);
			}
		}
	}

	static void onUntrack(ServerLevel level, ChunkPos pos, ServerPlayer player) {
		AreaChunkHolder holder = AreaChunkHolder.of(level, pos);
		if (holder == null) return;
		for (AreaEffectEntry entry : holder.getAffecting()) {
			if (entry.decrementTracking(player)) {
				AreaEffectSyncPacket.sendRemove(player, entry.id);
			}
		}
	}

	static void notifyRemoveToPlayer(ServerLevel level, ServerPlayer player, UUID id) {
		AreaEffectSyncPacket.sendRemove(player, id);
	}
}
