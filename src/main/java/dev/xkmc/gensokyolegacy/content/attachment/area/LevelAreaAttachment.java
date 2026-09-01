package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.l2core.capability.attachment.GeneralCapabilityTemplate;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SerialClass
public class LevelAreaAttachment extends GeneralCapabilityTemplate<Level, LevelAreaAttachment> {

	@SerialField
	private final Map<UUID, AreaEffectEntry> byId = new LinkedHashMap<>();

	@SerialField
	private final Map<String, List<UUID>> pending = new LinkedHashMap<>();

	// not SerialField -> not serialized
	private long lastPendingFlushTick = Long.MIN_VALUE;

	public Map<UUID, AreaEffectEntry> getById() {
		return byId;
	}

	public Map<String, List<UUID>> getPending() {
		return pending;
	}

	@Nullable
	public AreaEffectEntry get(UUID id) {
		return byId.get(id);
	}

	public Collection<AreaEffectEntry> getAll() {
		return byId.values();
	}

	public void tickValidation(ServerLevel level) {
		long tick = level.getGameTime();
		int tickBucket = (int) (tick % 100);
		List<UUID> toRemove = new ArrayList<>();
		for (AreaEffectEntry entry : byId.values()) {
			int bucket = Math.floorMod(entry.id.hashCode(), 100);
			if (bucket != tickBucket) continue;
			ChunkPos ownerCP = new ChunkPos(entry.ownerPos);
			if (level.getChunkSource().getChunkNow(ownerCP.x, ownerCP.z) == null) continue;
			if (!entry.isOwnerValid(level)) {
				toRemove.add(entry.id);
			}
		}
		for (UUID id : toRemove) {
			AreaEffectEntry entry = byId.remove(id);
			if (entry == null) continue;
			for (UUID playerId : Set.copyOf(entry.getTrackingPlayers())) {
				ServerPlayer p = level.getServer().getPlayerList().getPlayer(playerId);
				if (p != null) AreaEffectManager.notifyRemoveToPlayer(level, p, id);
			}
			entry.getTrackingCounts().clear();
			// pending entries with dead UUID will be skipped on flush via byId.containsKey
		}
		// every 5s clean up offline players from tracking lists
		if (tick % 100 == 0) {
			for (AreaEffectEntry e : byId.values()) e.cleanupPlayers(level);
		}
	}

	public void tickPendingFlush(ServerLevel level) {
		if (level.getGameTime() - lastPendingFlushTick < 100) return;
		lastPendingFlushTick = level.getGameTime();
		if (pending.size() <= 10) return;
		var snapshot = new ArrayList<>(pending.keySet());
		for (String key : snapshot) {
			long posLong = Long.parseUnsignedLong(key, 16);
			ChunkPos cpos = new ChunkPos(posLong);
			if (level.getChunkSource().getChunkNow(cpos.x, cpos.z) != null) {
				// in fact in a loaded chunk but still pending (e.g., add raced load) — drain directly
				var pendingIds = pending.remove(key);
				if (pendingIds == null || pendingIds.isEmpty()) continue;
				AreaChunkHolder holder = AreaChunkHolder.of(level, cpos);
				if (holder == null) continue;
				for (UUID uid : pendingIds) {
					if (!byId.containsKey(uid)) continue;
					holder.addId(uid);
				}
			} else {
				// not loaded, schedule offthread load (forcing generation expected)
				level.getChunkSource().getChunk(cpos.x, cpos.z, ChunkStatus.FULL, false);
			}
		}
	}
}
