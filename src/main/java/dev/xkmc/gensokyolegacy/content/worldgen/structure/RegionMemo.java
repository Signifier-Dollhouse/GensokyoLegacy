package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Process-local cache of the pure per-region verdict of one structure: the candidate index where it starts in that
 * region, or {@code -1} for a region it leaves empty. Misses are cached too, since a miss is the common case and costs
 * the same worldgen walk. Used by every tier, so a lower tier reads a higher tier's verdict once instead of rescanning.
 *
 * @see "doc/design/structure/placement.md"
 */
public final class RegionMemo {

	/**
	 * Bounds the map across a long session; a full clear is cheap next to a worldgen walk, and entries are only
	 * re-earned by searching.
	 */
	private static final int CAP = 100_000;

	private static final Map<Key, Integer> VERDICTS = new ConcurrentHashMap<>();

	private RegionMemo() {
	}

	/**
	 * The cached index for {@code (structure, generator, seed, region)}, computing it on a miss. Two threads may
	 * compute the same region at once; the later write wins and both callers get the same value.
	 */
	static int verdict(Structure structure, ChunkGenerator generator, long seed, int regionX, int regionZ,
					   Supplier<Integer> compute) {
		Key key = new Key(structure, generator, seed, regionX, regionZ);
		Integer hit = VERDICTS.get(key);
		if (hit != null) {
			return hit;
		}
		int value = compute.get();
		if (VERDICTS.size() >= CAP) {
			VERDICTS.clear();
		}
		VERDICTS.put(key, value);
		return value;
	}

	/**
	 * Drops every verdict, so a new world or a reloaded datapack cannot read a verdict of another generator or a
	 * structure instance that no longer exists.
	 */
	public static void clearAll() {
		VERDICTS.clear();
	}

	/**
	 * The generator is part of the key by identity: the same structure can be placed in two dimensions.
	 */
	private record Key(Structure structure, ChunkGenerator generator, long seed, int regionX, int regionZ) {

	}

}
