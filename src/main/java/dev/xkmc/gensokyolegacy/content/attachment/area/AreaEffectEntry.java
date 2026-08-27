package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
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

	// not SerialField -> not serialized, held by entry per spec
	private final Set<ServerPlayer> trackingPlayers = new HashSet<>();

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
		return trackingPlayers;
	}

	public void sync() {
		for (ServerPlayer p : trackingPlayers) {
			AreaEffectSyncPacket.sendUpdate(p, this);
		}
	}
}
