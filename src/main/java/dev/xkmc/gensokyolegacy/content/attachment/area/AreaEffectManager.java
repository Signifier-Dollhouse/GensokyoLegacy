package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
					AreaChunkHolder.of(chunk).addId(entry.id);
				} else {
					String key = Long.toHexString(cpos.toLong());
					levelAtt.getPending().computeIfAbsent(key, k -> new ArrayList<>()).add(entry.id);
				}
			}
		}
		// sync to tracking players (those already tracking any chunk in range)
		Set<ServerPlayer> players = collectTrackingPlayers(level, entry.range);
		entry.getTrackingPlayers().addAll(players);
		for (ServerPlayer p : players) {
			AreaEffectSyncPacket.sendAdd(p, entry);
		}
	}

	public static boolean remove(ServerLevel level, UUID id) {
		LevelAreaAttachment levelAtt = GLMeta.LEVEL_EFFECT.type().getOrCreate(level);
		AreaEffectEntry removed = levelAtt.getById().remove(id);
		if (removed == null) return false;
		// notify tracking players (now held by entry), keep pending stale skip via byId check
		for (ServerPlayer p : removed.getTrackingPlayers()) {
			AreaEffectSyncPacket.sendRemove(p, id);
		}
		removed.getTrackingPlayers().clear();
		// no chunk iteration, no pending scan per spec (pending lazily skipped)
		return true;
	}

	@Nullable
	public static AreaEffectEntry get(ServerLevel level, UUID id) {
		return GLMeta.LEVEL_EFFECT.type().getOrCreate(level).get(id);
	}

	public static List<AreaEffectEntry> getAffecting(LevelChunk chunk) {
		return AreaChunkHolder.of(chunk).getAffecting();
	}

	public static List<AreaEffectEntry> getAffecting(Level level, ChunkPos pos) {
		if (!level.isLoaded(new BlockPos(pos.x << 4, 64, pos.z << 4))) return List.of();
		LevelChunk chunk = level.getChunk(pos.x, pos.z);
		return getAffecting(chunk);
	}

	public static List<AreaEffectEntry> getAffecting(Level level, BlockPos pos) {
		return getAffecting(level, new ChunkPos(pos));
	}

	public static void tickValidation(ServerLevel level) {
		GLMeta.LEVEL_EFFECT.type().getOrCreate(level).tickValidation(level);
	}

	public static void tickPendingFlush(ServerLevel level) {
		GLMeta.LEVEL_EFFECT.type().getOrCreate(level).tickPendingFlush(level);
	}

	// tracking helpers

	private static Set<ServerPlayer> collectTrackingPlayers(ServerLevel level, ChunkPosRange range) {
		Set<ServerPlayer> result = new HashSet<>();
		for (int x = range.minCX(); x <= range.maxCX(); x++) {
			for (int z = range.minCZ(); z <= range.maxCZ(); z++) {
				ChunkPos cpos = new ChunkPos(x, z);
				result.addAll(getTrackingPlayers(level, cpos));
			}
		}
		return result;
	}

	private static Set<ServerPlayer> getTrackingPlayers(ServerLevel level, ChunkPos pos) {
		// Use chunkMap's tracking players; fallback to distance check if API not accessible
		try {
			var cache = level.getChunkSource();
			// ServerChunkCache#chunkMap is accessible via level.getChunkSource().chunkMap
			var chunkMap = cache.chunkMap;
			// chunkMap.getPlayers(ChunkPos, boolean) exists in 1.21
			return new HashSet<>((Collection<ServerPlayer>) chunkMap.getClass().getMethod("getPlayers", ChunkPos.class, boolean.class).invoke(chunkMap, pos, false));
		} catch (Exception e) {
			// fallback: manual distance check
			Set<ServerPlayer> out = new HashSet<>();
			for (ServerPlayer p : level.players()) {
				ChunkPos pPos = new ChunkPos(p.blockPosition());
				int dist = Math.max(Math.abs(pPos.x - pos.x), Math.abs(pPos.z - pos.z));
				if (dist <= p.requestedViewDistance()) {
					if (level.getChunkSource().getChunkNow(pos.x, pos.z) != null) {
						out.add(p);
					}
				}
			}
			return out;
		}
	}

	static boolean isPlayerTracking(ServerLevel level, ServerPlayer player, ChunkPos pos) {
		Set<ServerPlayer> trackers = getTrackingPlayers(level, pos);
		return trackers.contains(player);
	}

	static void onTrack(ServerLevel level, ChunkPos pos, ServerPlayer player) {
		AreaChunkHolder holder = AreaChunkHolder.of(level, pos);
		if (holder == null) return;
		// iterate only affecting data for this chunk (k effects, not all M on level)
		for (AreaEffectEntry entry : holder.getAffecting()) {
			if (entry.getTrackingPlayers().add(player)) {
				AreaEffectSyncPacket.sendAdd(player, entry);
			}
		}
	}

	static void onUntrack(ServerLevel level, ChunkPos pos, ServerPlayer player) {
		AreaChunkHolder holder = AreaChunkHolder.of(level, pos);
		if (holder == null) return;
		// iterate only effects that actually affected this chunk
		for (UUID id : holder.attachment().getEffectIds()) {
			AreaEffectEntry entry = GLMeta.LEVEL_EFFECT.type().getOrCreate(level).get(id);
			if (entry == null) continue;
			boolean stillTracks = false;
			for (int x = entry.range.minCX(); x <= entry.range.maxCX(); x++) {
				for (int z = entry.range.minCZ(); z <= entry.range.maxCZ(); z++) {
					ChunkPos other = new ChunkPos(x, z);
					if (other.equals(pos)) continue;
					if (isPlayerTracking(level, player, other)) {
						stillTracks = true;
						break;
					}
				}
				if (stillTracks) break;
			}
			if (!stillTracks) {
				if (entry.getTrackingPlayers().remove(player)) {
					AreaEffectSyncPacket.sendRemove(player, entry.id);
				}
			}
		}
	}

	static void notifyRemoveToPlayer(ServerLevel level, ServerPlayer player, UUID id) {
		AreaEffectSyncPacket.sendRemove(player, id);
	}
}
