package dev.xkmc.gensokyolegacy.content.item.ingredient;

import dev.xkmc.gensokyolegacy.content.worldgen.structure.FlatCheckStructure;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.LevelBounds;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2core.events.SchedulerHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One fairy-dust cast: a transient server ticker that spins a mote around the flower while the ring-by-ring
 * structure search runs off-thread.
 *
 * @see "doc/design/structure/fairy_dust.md"
 */
public final class FairyDustSearch {

	/**
	 * Rings of a structure set's own spacing searched before the cast gives up. On the 32-chunk grid this is about
	 * 4 km, and a home is normally in ring 1-3.
	 */
	private static final int MAX_RING = 8;

	/**
	 * One mote per tick, 30 degrees apart, so a revolution takes 12 ticks.
	 */
	private static final double SPIN_STEP = Math.PI / 6;
	private static final double SPIN_RADIUS = 0.6;

	/**
	 * The cast in flight per player. Main-thread only: a player cannot start a second search while one is running.
	 */
	private static final Map<UUID, FairyDustSearch> ACTIVE = new HashMap<>();

	/**
	 * One dedicated thread: a ring is submitted only after the previous one finished, so casts queue instead of
	 * competing with worldgen on {@code Util.backgroundExecutor()}.
	 */
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "gensokyo-fairy-dust");
		thread.setDaemon(true);
		return thread;
	});

	private final ServerLevel level;
	private final BlockPos origin;
	private final Vec3 from;
	private final long seed;
	private final RegistryAccess registries;
	private final ChunkGenerator generator;
	private final RandomState randomState;
	private final StructureTemplateManager templates;
	private final LevelHeightAccessor height;
	private final List<FlatCheckStructure> structures;
	private final ChunkPos center;
	private final ItemStack refund;
	private final boolean consume;
	private final UUID playerId;

	private int ring = -1;
	private int ticks;
	private CompletableFuture<BlockPos> pending;
	private volatile boolean cancelled;

	private FairyDustSearch(ServerLevel level, BlockPos origin, List<FlatCheckStructure> structures,
							ItemStack refund, boolean consume, UUID playerId) {
		this.level = level;
		this.origin = origin;
		this.from = Vec3.atBottomCenterOf(origin).add(0, 0.5, 0);
		this.seed = level.getSeed();
		this.registries = level.registryAccess();
		this.generator = level.getChunkSource().getGenerator();
		this.randomState = level.getChunkSource().randomState();
		this.templates = level.getStructureManager();
		this.height = LevelBounds.of(level);
		this.structures = structures;
		this.center = new ChunkPos(origin);
		this.refund = refund;
		this.consume = consume;
		this.playerId = playerId;
	}

	/**
	 * Resolves the tag on the server thread and starts the ticker. {@code false} when nothing in the tag is a
	 * flat-check structure, in which case the caller refunds immediately.
	 */
	static boolean start(ServerLevel level, BlockPos origin, TagKey<Structure> tag,
						 ItemStack refund, boolean consume, UUID playerId) {
		var tagSet = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(tag);
		if (tagSet.isEmpty()) return false;
		List<FlatCheckStructure> structures = new ArrayList<>();
		for (Holder<Structure> holder : tagSet.get()) {
			if (holder.value() instanceof FlatCheckStructure flat && flat.spacing() > 0) {
				structures.add(flat);
			}
		}
		if (structures.isEmpty()) return false;
		FairyDustSearch search = new FairyDustSearch(level, origin, structures, refund, consume, playerId);
		ACTIVE.put(playerId, search);
		SchedulerHandler.schedulePersistent(search::tick);
		return true;
	}

	/**
	 * True while this player's previous cast is still searching. A search left behind by a stopped server is pruned.
	 */
	static boolean isBusy(Player player) {
		FairyDustSearch search = ACTIVE.get(player.getUUID());
		if (search == null) return false;
		if (search.level.getServer().isRunning()) return true;
		ACTIVE.remove(player.getUUID());
		return false;
	}

	private boolean tick() {
		try {
			if (!level.getServer().isRunning()) {
				cancelled = true;
				ACTIVE.remove(playerId);
				return true;
			}
			spin();
			ticks++;
			if (pending != null) {
				if (!pending.isDone()) return false;
				BlockPos found = pending.join();
				pending = null;
				if (found != null) {
					FairyDustItem.castRay(level, from, Vec3.atBottomCenterOf(found));
					ACTIVE.remove(playerId);
					return true;
				}
				if (ring >= MAX_RING) return fail();
			}
			ring++;
			int current = ring;
			pending = CompletableFuture.supplyAsync(() -> searchRing(current), EXECUTOR);
			return false;
		} catch (Exception e) {
			GensokyoLegacy.LOGGER.error("Fairy dust search failed", e);
			return fail();
		}
	}

	/**
	 * One ring around the clicked chunk, for every structure in the tag, keeping the nearest start found. Region
	 * coordinates are per structure because each set has its own spacing.
	 */
	private BlockPos searchRing(int ring) {
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (FlatCheckStructure flat : structures) {
			int spacing = flat.spacing();
			int centerX = Math.floorDiv(center.x, spacing);
			int centerZ = Math.floorDiv(center.z, spacing);
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dz = -ring; dz <= ring; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
					if (cancelled) return best;
					var found = flat.locateRegion(seed, centerX + dx, centerZ + dz,
							registries, generator, randomState, templates, height);
					if (found.isEmpty()) continue;
					double dist = horizontalDistSqr(origin, found.get().pos());
					if (dist < bestDist) {
						bestDist = dist;
						best = found.get().pos();
					}
				}
			}
		}
		return best;
	}

	private void spin() {
		double angle = ticks * SPIN_STEP;
		double x = from.x + Math.cos(angle) * SPIN_RADIUS;
		double z = from.z + Math.sin(angle) * SPIN_RADIUS;
		level.sendParticles(FairyDustItem.DUST, x, from.y, z, 1, 0, 0, 0, 0);
	}

	private boolean fail() {
		// stop a ring still running after an exception; the normal path has nothing in flight
		cancelled = true;
		FairyDustItem.scatter(level, from, level.random);
		if (consume) {
			ItemEntity item = new ItemEntity(level, from.x, from.y + 0.25, from.z, refund);
			item.setDefaultPickUpDelay();
			level.addFreshEntity(item);
		}
		ACTIVE.remove(playerId);
		return true;
	}

	private static double horizontalDistSqr(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz;
	}

}
