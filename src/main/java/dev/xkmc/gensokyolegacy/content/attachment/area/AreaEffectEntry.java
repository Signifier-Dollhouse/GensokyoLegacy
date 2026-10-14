package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One area effect: its owner block, chunk range and payload.
 *
 * @see "doc/design/area_effect.md"
 */
@SerialClass
public class AreaEffectEntry {

	@SerialField
	public UUID id;
	@SerialField
	public BlockPos ownerPos;
	@SerialField
	public ChunkPosRange range;
	@SerialField
	public EffectData data;
	@SerialField
	public long createdGameTime;

	// not serialized: how many of this effect's chunks each player currently tracks
	private final Map<UUID, Integer> trackingCounts = new HashMap<>();

	public AreaEffectEntry() {
	}

	public AreaEffectEntry(UUID id, BlockPos ownerPos, ChunkPosRange range, EffectData data, long createdGameTime) {
		this.id = id;
		this.ownerPos = ownerPos;
		this.range = range;
		this.data = data;
		this.createdGameTime = createdGameTime;
	}

	// isLoaded rather than getBlockState alone: validating an owner must not pull in its chunk
	public boolean isOwnerValid(ServerLevel level) {
		if (!level.isLoaded(ownerPos)) return false;
		return data.isOwnerStillValid(level, ownerPos, level.getBlockState(ownerPos));
	}

	public Set<UUID> getTrackingPlayers() {
		return trackingCounts.keySet();
	}

	public Map<UUID, Integer> getTrackingCounts() {
		return trackingCounts;
	}

	/**
	 * @return true on the player's first tracked chunk, so an ADD should be sent
	 */
	public boolean incrementTracking(ServerPlayer player) {
		UUID uuid = player.getUUID();
		int c = trackingCounts.getOrDefault(uuid, 0) + 1;
		trackingCounts.put(uuid, c);
		return c == 1;
	}

	/**
	 * @return true on the player's last tracked chunk, so a REMOVE should be sent
	 */
	public boolean decrementTracking(ServerPlayer player) {
		UUID uuid = player.getUUID();
		Integer c = trackingCounts.get(uuid);
		if (c == null) return false;
		if (c <= 1) {
			trackingCounts.remove(uuid);
			return true;
		}
		trackingCounts.put(uuid, c - 1);
		return false;
	}

	public void sync(ServerLevel level) {
		for (UUID uuid : Set.copyOf(trackingCounts.keySet())) {
			ServerPlayer p = level.getServer().getPlayerList().getPlayer(uuid);
			if (p != null) AreaEffectSyncPacket.sendUpdate(p, this);
		}
	}

	public void cleanupPlayers(ServerLevel level) {
		trackingCounts.keySet().removeIf(uuid -> level.getServer().getPlayerList().getPlayer(uuid) == null);
	}
}
