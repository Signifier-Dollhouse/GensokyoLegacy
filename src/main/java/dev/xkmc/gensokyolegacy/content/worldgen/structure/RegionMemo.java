package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-local cache of pure per-region verdicts of one structure instance: the candidate index where the tier starts,
 * or {@code -1}. Eviction only costs a recompute, so it is cleared on server stop and on datapack reload.
 *
 * @see "doc/design/structure/secondary.md"
 */
public final class RegionMemo {

	private static final int CAP = 4096;
	private static final Set<RegionMemo> LIVE =
			Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	private final Map<Key, Integer> verdicts = new ConcurrentHashMap<>();

	public RegionMemo() {
		LIVE.add(this);
	}

	/**
	 * The cached verdict, or {@code null} when the region was never scanned. {@code -1} is a real verdict.
	 */
	Integer get(ChunkGenerator generator, long seed, int rx, int rz) {
		return verdicts.get(new Key(generator, seed, rx, rz));
	}

	void put(ChunkGenerator generator, long seed, int rx, int rz, int verdict) {
		if (verdicts.size() >= CAP) {
			verdicts.clear();
		}
		verdicts.put(new Key(generator, seed, rx, rz), verdict);
	}

	/**
	 * Drops every live memo, so a new world or a reloaded datapack cannot read a verdict of another generator or a
	 * structure instance that no longer exists.
	 */
	public static void clearAll() {
		synchronized (LIVE) {
			for (RegionMemo memo : LIVE) {
				memo.verdicts.clear();
			}
		}
	}

	private record Key(ChunkGenerator generator, long seed, int rx, int rz) {

	}

}
