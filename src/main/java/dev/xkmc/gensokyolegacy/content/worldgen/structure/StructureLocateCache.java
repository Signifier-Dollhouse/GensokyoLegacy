package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Region results of {@link FlatCheckStructure#locateRegion}, so a repeated fairy-dust cast or structure scan skips
 * the worldgen walk. Empty regions are cached too: a miss is the common and expensive case. Cleared on server stop.
 *
 * @see "doc/design/structure/placement.md"
 */
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public final class StructureLocateCache {

	/**
	 * Bounds the map across a long session; a full clear is cheap next to a worldgen walk, and entries are only
	 * re-earned by searching.
	 */
	private static final int MAX_ENTRIES = 100_000;

	private static final Map<Key, Optional<FlatCheckStructure.Located>> CACHE = new ConcurrentHashMap<>();

	private StructureLocateCache() {
	}

	/**
	 * Cached result for one region, computing it on a miss. Two threads may compute the same region at once; the
	 * later write wins and both callers get the same value.
	 */
	static Optional<FlatCheckStructure.Located> get(FlatCheckStructure structure, ChunkGenerator generator,
			long seed, int regionX, int regionZ, Supplier<Optional<FlatCheckStructure.Located>> compute) {
		Key key = new Key(structure, generator, seed, regionX, regionZ);
		Optional<FlatCheckStructure.Located> hit = CACHE.get(key);
		if (hit != null) return hit;
		Optional<FlatCheckStructure.Located> value = compute.get();
		if (CACHE.size() >= MAX_ENTRIES) CACHE.clear();
		CACHE.put(key, value);
		return value;
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		CACHE.clear();
	}

	/**
	 * The generator is part of the key by identity: the same structure can be placed in two dimensions.
	 */
	private record Key(FlatCheckStructure structure, ChunkGenerator generator, long seed, int regionX, int regionZ) {
	}

}
