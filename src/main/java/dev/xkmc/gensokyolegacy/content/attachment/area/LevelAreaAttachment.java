package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.l2core.capability.attachment.GeneralCapabilityTemplate;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
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
	private final Map<BlockPos, UUID> byOwner = new LinkedHashMap<>();

	@SerialField
	private final Map<String, List<UUID>> pending = new LinkedHashMap<>();

	// not SerialField -> not serialized
	private long lastPendingFlushTick = Long.MIN_VALUE;

	public Map<UUID, AreaEffectEntry> getById() {
		return byId;
	}

	public Map<BlockPos, UUID> getByOwner() {
		return byOwner;
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

	/**
	 * Add an entry, indexing it both by id and by owner position. One effect per owner block:
	 * an existing effect at the same position is removed first (REMOVE sent to its trackers).
	 */
	public void addEntry(ServerLevel level, AreaEffectEntry entry) {
		UUID prev = byOwner.get(entry.ownerPos);
		if (prev != null && !prev.equals(entry.id)) {
			removeEntry(level, prev);
		}
		byId.put(entry.id, entry);
		byOwner.put(entry.ownerPos, entry.id);
	}

	/** {@code byId}/{@code byOwner} cleanup only; no packet side effects. */
	@Nullable
	private AreaEffectEntry removeEntryData(UUID id) {
		AreaEffectEntry entry = byId.remove(id);
		if (entry == null) return null;
		if (id.equals(byOwner.get(entry.ownerPos))) {
			byOwner.remove(entry.ownerPos);
		}
		return entry;
	}

	/**
	 * Remove an entry by id: cleans the {@code byId}/{@code byOwner} indexes, sends REMOVE to every
	 * tracking player, and clears their counts. Single removal path that keeps maps and clients in sync.
	 */
	@Nullable
	public AreaEffectEntry removeEntry(ServerLevel level, UUID id) {
		AreaEffectEntry entry = removeEntryData(id);
		if (entry == null) return null;
		for (UUID playerId : Set.copyOf(entry.getTrackingPlayers())) {
			ServerPlayer p = level.getServer().getPlayerList().getPlayer(playerId);
			if (p != null) AreaEffectManager.notifyRemoveToPlayer(level, p, id);
		}
		entry.getTrackingCounts().clear();
		return entry;
	}

	@Nullable
	public UUID getOwnerId(BlockPos pos) {
		return byOwner.get(pos);
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
			if (removeEntry(level, id) == null) continue;
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
