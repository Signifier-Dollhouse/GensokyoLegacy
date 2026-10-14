package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side view of one chunk's effect index; {@code of(level, pos)} never loads, null if unloaded.
 *
 * @see "doc/design/area_effect.md"
 */
public record AreaChunkHolder(ServerLevel level, ChunkPos pos, LevelChunk chunk, ChunkAreaAttachment attachment) {

	@Nullable
	public static AreaChunkHolder of(ServerLevel level, ChunkPos pos) {
		LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, false);
		if (chunk == null) return null;
		return new AreaChunkHolder(level, pos, chunk, GLMeta.CHUNK_EFFECT.type().getOrCreate(chunk));
	}

	public static AreaChunkHolder of(ServerLevel level, LevelChunk chunk) {
		return new AreaChunkHolder(level, chunk.getPos(), chunk, GLMeta.CHUNK_EFFECT.type().getOrCreate(chunk));
	}

	// effectIds is @SerialField, so the chunk must be dirtied or the change is not saved
	public void addId(UUID id) {
		if (attachment.getEffectIds().add(id)) {
			attachment.invalidateCache();
			chunk.setUnsaved(true);
		}
	}

	public void removeId(UUID id) {
		if (attachment.getEffectIds().remove(id)) {
			attachment.invalidateCache();
			chunk.setUnsaved(true);
		}
	}

	/**
	 * Effects covering this chunk, resolved once per game tick. Ids whose entry is already gone are
	 * pruned here, because removal never scans chunk indexes.
	 */
	public List<AreaEffectEntry> getAffecting() {
		long gameTime = level.getGameTime();
		if (attachment.isCacheValid() && gameTime == attachment.getCachedTick()) {
			return attachment.getCachedResolved();
		}
		LevelAreaAttachment levelAtt = GLMeta.LEVEL_EFFECT.type().getOrCreate(level);
		List<AreaEffectEntry> resolved = new ArrayList<>(attachment.getEffectIds().size());
		List<UUID> toRemove = new ArrayList<>();
		for (UUID id : attachment.getEffectIds()) {
			AreaEffectEntry entry = levelAtt.get(id);
			if (entry != null) resolved.add(entry);
			else toRemove.add(id);
		}
		if (!toRemove.isEmpty()) {
			attachment.getEffectIds().removeAll(toRemove);
			chunk.setUnsaved(true);
		}
		List<AreaEffectEntry> copy = List.copyOf(resolved);
		attachment.setCached(copy, gameTime);
		return copy;
	}
}
