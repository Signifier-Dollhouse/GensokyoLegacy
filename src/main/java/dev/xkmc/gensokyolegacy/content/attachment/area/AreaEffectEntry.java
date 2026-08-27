package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

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

	// not SerialField -> not serialized, holds count of tracked chunks per player for this effect
	private final Map<ServerPlayer, Integer> trackingCounts = new HashMap<>();

	public AreaEffectEntry() {
	}

	public AreaEffectEntry(UUID id, BlockPos ownerPos, ChunkPosRange range, EffectData data, long createdGameTime) {
		this.id = id;
		this.ownerPos = ownerPos;
		this.range = range;
		this.data = data;
		this.createdGameTime = createdGameTime;
	}

	public boolean isOwnerValid(ServerLevel level) {
		if (!level.isLoaded(ownerPos)) return false;
		return data.isOwnerStillValid(level, ownerPos, level.getBlockState(ownerPos));
	}

	public Set<ServerPlayer> getTrackingPlayers() {
		return trackingCounts.keySet();
	}

	public Map<ServerPlayer, Integer> getTrackingCounts() {
		return trackingCounts;
	}

	/** @return true if first tracking chunk for this player */
	public boolean incrementTracking(ServerPlayer player) {
		int c = trackingCounts.getOrDefault(player, 0) + 1;
		trackingCounts.put(player, c);
		return c == 1;
	}

	/** @return true if last chunk untracked (should send REMOVE) */
	public boolean decrementTracking(ServerPlayer player) {
		Integer c = trackingCounts.get(player);
		if (c == null) return false;
		if (c <= 1) {
			trackingCounts.remove(player);
			return true;
		}
		trackingCounts.put(player, c - 1);
		return false;
	}

	public void sync() {
		for (ServerPlayer p : trackingCounts.keySet()) {
			AreaEffectSyncPacket.sendUpdate(p, this);
		}
	}
}
