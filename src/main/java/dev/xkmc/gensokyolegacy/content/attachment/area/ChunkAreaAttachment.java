package dev.xkmc.gensokyolegacy.content.attachment.area;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import dev.xkmc.l2library.capability.entity.GeneralCapabilityTemplate;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Per-chunk effect id set; all resolving logic lives in {@link AreaChunkHolder}.
 *
 * @see "doc/design/area_effect.md"
 */
@SerialClass
public class ChunkAreaAttachment extends GeneralCapabilityTemplate<LevelChunk, ChunkAreaAttachment> {

	public static final Capability<ChunkAreaAttachment> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

	@SerialField
	private final Set<UUID> effectIds = new LinkedHashSet<>();

	// unannotated, so not serialized (l2serial needs no transient keyword); the resolved list is
	// reused only while the game tick matches, and is rebuilt on demand
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
