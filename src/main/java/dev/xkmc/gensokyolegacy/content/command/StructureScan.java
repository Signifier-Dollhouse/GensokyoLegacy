package dev.xkmc.gensokyolegacy.content.command;

import dev.xkmc.gensokyolegacy.content.worldgen.structure.LocatableStructure;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.LevelBounds;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Background {@code /gensokyo structure scan}: walks regions on its own thread and reports through the server
 * thread, so a large radius does not stall the tick loop. One scan at a time.
 *
 * @see "doc/design/structure/placement.md"
 */
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public final class StructureScan {

	/**
	 * One scan at a time, so two large radii cannot pile up on the same machine.
	 */
	private static final AtomicReference<StructureScan> RUNNING = new AtomicReference<>();

	/**
	 * A scan thread of its own, so it neither starves worldgen nor the fairy-dust ring search.
	 */
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "gensokyo-structure-scan");
		thread.setDaemon(true);
		return thread;
	});

	/**
	 * Progress messages are throttled to this interval, since a radius-100 scan takes minutes.
	 */
	private static final long PROGRESS_NANOS = 2_000_000_000L;

	private final MinecraftServer server;
	private final CommandSourceStack source;
	private final ServerPlayer player;
	private final ResourceLocation id;
	private final LocatableStructure loc;
	private final int radius;
	private final boolean first;
	private final long seed;
	private final RegistryAccess registries;
	private final ChunkGenerator generator;
	private final RandomState randomState;
	private final StructureTemplateManager templates;
	private final LevelHeightAccessor height;
	private final BlockPos origin;
	private final int centerX;
	private final int centerZ;

	private volatile boolean cancelled;
	private long lastReport;

	private StructureScan(CommandSourceStack source, LocatableStructure loc, ResourceLocation id,
			int radius, boolean first) {
		ServerLevel level = source.getLevel();
		this.origin = BlockPos.containing(source.getPosition());
		ChunkPos center = new ChunkPos(origin);
		this.server = level.getServer();
		this.source = source;
		this.player = source.getPlayer();
		this.id = id;
		this.loc = loc;
		this.radius = radius;
		this.first = first;
		this.seed = level.getSeed();
		this.registries = level.registryAccess();
		this.generator = level.getChunkSource().getGenerator();
		this.randomState = level.getChunkSource().randomState();
		this.templates = level.getStructureManager();
		this.height = LevelBounds.of(level);
		this.centerX = Math.floorDiv(center.x, loc.spacing());
		this.centerZ = Math.floorDiv(center.z, loc.spacing());
	}

	/**
	 * Starts a scan off-thread. {@code false} when one is already running.
	 */
	static boolean start(CommandSourceStack source, LocatableStructure loc, ResourceLocation id,
			int radius, boolean first) {
		StructureScan scan = new StructureScan(source, loc, id, radius, first);
		if (!RUNNING.compareAndSet(null, scan)) return false;
		EXECUTOR.submit(scan::run);
		return true;
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		StructureScan scan = RUNNING.getAndSet(null);
		if (scan != null) scan.cancelled = true;
	}

	private void run() {
		try {
			long begin = System.nanoTime();
			lastReport = begin;
			List<LocatableStructure.Located> hits = first ? searchFirst(begin) : searchAll(begin);
			if (!cancelled) report(hits, (System.nanoTime() - begin) / 1_000_000);
		} catch (Exception e) {
			GensokyoLegacy.LOGGER.error("Structure scan failed", e);
			report(List.of("scan failed: " + e));
		} finally {
			RUNNING.compareAndSet(this, null);
		}
	}

	/**
	 * Rings outward from the source, stopping at the first ring that has a start.
	 */
	private List<LocatableStructure.Located> searchFirst(long begin) {
		for (int ring = 0; ring <= radius; ring++) {
			List<LocatableStructure.Located> hits = scanRing(ring);
			if (cancelled) return List.of();
			if (!hits.isEmpty()) {
				hits.sort(nearestFirst());
				return List.of(hits.get(0));
			}
			progress(ring + 1, radius + 1, 0, begin);
		}
		return List.of();
	}

	/**
	 * The whole {@code (2r+1)^2} square, printed nearest-first.
	 */
	private List<LocatableStructure.Located> searchAll(long begin) {
		List<LocatableStructure.Located> hits = new ArrayList<>();
		int rows = 2 * radius + 1;
		for (int dz = -radius; dz <= radius; dz++) {
			for (int dx = -radius; dx <= radius; dx++) {
				if (cancelled) return hits;
				locate(dx, dz).ifPresent(hits::add);
			}
			progress(dz + radius + 1, rows, hits.size(), begin);
		}
		hits.sort(nearestFirst());
		return hits;
	}

	private List<LocatableStructure.Located> scanRing(int ring) {
		List<LocatableStructure.Located> hits = new ArrayList<>();
		for (int dx = -ring; dx <= ring; dx++) {
			for (int dz = -ring; dz <= ring; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
				if (cancelled) return hits;
				locate(dx, dz).ifPresent(hits::add);
			}
		}
		return hits;
	}

	private Optional<LocatableStructure.Located> locate(int dx, int dz) {
		return loc.locateRegion(seed, centerX + dx, centerZ + dz, registries, generator,
				randomState, templates, height);
	}

	private void progress(int done, int total, int hits, long begin) {
		long now = System.nanoTime();
		if (now - lastReport < PROGRESS_NANOS) return;
		lastReport = now;
		report(List.of("%s: %d/%d %s, %d start(s), %d s".formatted(id, done, total,
				first ? "rings" : "rows", hits, (now - begin) / 1_000_000_000L)));
	}

	private void report(List<LocatableStructure.Located> hits, long elapsed) {
		List<String> lines = new ArrayList<>();
		lines.add("%s: %d start(s) within %d region rings (%d ms)"
				.formatted(id, hits.size(), radius, elapsed));
		for (LocatableStructure.Located hit : hits) {
			lines.add("region [%d, %d] chunk [%d, %d] at [%d, ~, %d] (%.0f blocks away)"
					.formatted(hit.regionX(), hit.regionZ(), hit.chunk().x, hit.chunk().z,
							hit.pos().getX(), hit.pos().getZ(),
							Math.sqrt(horizontalDistSqr(origin, hit.pos()))));
		}
		report(lines);
	}

	private void report(List<String> lines) {
		for (String line : lines) GensokyoLegacy.LOGGER.info("[structure] {}", line);
		server.execute(() -> {
			if (player != null && player.hasDisconnected()) return;
			for (String line : lines) source.sendSuccess(() -> Component.literal(line), false);
		});
	}

	private Comparator<LocatableStructure.Located> nearestFirst() {
		return Comparator.comparingDouble(hit -> horizontalDistSqr(origin, hit.pos()));
	}

	private static double horizontalDistSqr(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz;
	}

}
