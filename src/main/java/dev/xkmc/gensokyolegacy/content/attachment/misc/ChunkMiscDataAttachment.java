package dev.xkmc.gensokyolegacy.content.attachment.misc;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2core.capability.attachment.GeneralCapabilityTemplate;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Miscellaneous per-chunk state, currently the ghost summon's start and finish timestamps.
 *
 * @see "doc/design/gravestone.md"
 */
@SerialClass
public class ChunkMiscDataAttachment extends GeneralCapabilityTemplate<LevelChunk, ChunkMiscDataAttachment> {

	/**
	 * The five seconds a started summon holds the chunk, which is also the ritual's length.
	 */
	public static final int SUMMON_LOCK = 100;

	/**
	 * The one minute a finished summon locks the chunk.
	 */
	public static final int SUMMON_COOLDOWN = 1200;

	@SerialField
	private long summonStartTime = -SUMMON_LOCK;

	@SerialField
	private long summonFinishTime = -SUMMON_COOLDOWN;

	// unannotated, so not serialized; the chunk is only kept to dirty itself on write
	private LevelChunk owner;

	public static ChunkMiscDataAttachment of(LevelChunk chunk) {
		ChunkMiscDataAttachment data = GLMeta.CHUNK_MISC.type().getOrCreate(chunk);
		data.owner = chunk;
		return data;
	}

	public long summonStartTime() {
		return summonStartTime;
	}

	public long summonFinishTime() {
		return summonFinishTime;
	}

	/**
	 * A chunk is free only after both the running ritual's start lock and the last finished
	 * summon's cooldown have expired.
	 */
	public boolean canSummon(long gameTime) {
		return gameTime - summonStartTime >= SUMMON_LOCK
				&& gameTime - summonFinishTime >= SUMMON_COOLDOWN;
	}

	public void startSummon(long gameTime) {
		summonStartTime = gameTime;
		dirty();
	}

	public void finishSummon(long gameTime) {
		summonFinishTime = gameTime;
		dirty();
	}

	private void dirty() {
		if (owner != null) owner.setUnsaved(true);
	}

}
