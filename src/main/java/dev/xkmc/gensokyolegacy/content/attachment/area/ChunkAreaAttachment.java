package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.l2core.capability.attachment.GeneralCapabilityTemplate;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SerialClass
public class ChunkAreaAttachment extends GeneralCapabilityTemplate<LevelChunk, ChunkAreaAttachment> {

	@SerialField
	private final Set<UUID> effectIds = new LinkedHashSet<>();

	// not SerialField -> not serialized (no transient keyword needed)
	private long cachedTick = Long.MIN_VALUE;
	private List<AreaEffectEntry> cachedResolved = List.of();
	private boolean cacheValid = false;

	public Set<UUID> getEffectIds() {
		return effectIds;
	}

	public long getCachedTick() {
		return cachedTick;
	}

	public List<AreaEffectEntry> getCachedResolved() {
		return cachedResolved;
	}

	public boolean isCacheValid() {
		return cacheValid;
	}

	public void setCached(List<AreaEffectEntry> resolved, long tick) {
		this.cachedResolved = resolved;
		this.cachedTick = tick;
		this.cacheValid = true;
	}

	public void invalidateCache() {
		cacheValid = false;
	}
}
