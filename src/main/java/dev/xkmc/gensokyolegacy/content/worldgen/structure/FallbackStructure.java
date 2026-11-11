package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Structure member of a {@link FlatCheckStructure} set that fills the candidates the region's picked flat rejected.
 * It takes no part in the region pick (no set index/count) and is associated with a candidate by a position-seeded
 * pick among the set's fallback members. Distances to flats and to earlier kept fallback candidates use the flat's
 * safety radius; a reserved region has no flat to yield to, so every candidate is attempted there.
 *
 * @see "doc/design/structure/fallback.md"
 */
public class FallbackStructure extends Structure {

	public static final MapCodec<FallbackStructure> CODEC = RecordCodecBuilder.<FallbackStructure>mapCodec((i) -> i.group(
			settingsCodec(i),
			StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter((e) -> e.startPool),
			Codec.intRange(0, 7).fieldOf("size").forGetter((e) -> e.maxDepth),
			Codec.BOOL.optionalFieldOf("use_expansion_hack", false).forGetter((e) -> e.useExpansionHack),
			Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter((e) -> e.maxDistanceFromCenter),
			Codec.intRange(1, 128).fieldOf("height_tolerance").forGetter((e) -> e.heightTolerance),
			StructureSet.CODEC.fieldOf("set").forGetter((e) -> e.set)
	).apply(i, FallbackStructure::new)).validate(FallbackStructure::verifyRange);

	public final Holder<StructureTemplatePool> startPool;
	private final Holder<StructureSet> set;
	private final int maxDepth;
	private final boolean useExpansionHack;
	private final int maxDistanceFromCenter;
	private final int heightTolerance;

	private static DataResult<FallbackStructure> verifyRange(FallbackStructure s) {
		byte b0;
		switch (s.terrainAdaptation()) {
			case NONE:
				b0 = 0;
				break;
			case BURY:
			case BEARD_THIN:
			case BEARD_BOX:
			case ENCAPSULATE:
				b0 = 12;
				break;
			default:
				throw new IncompatibleClassChangeError();
		}

		if (s.maxDistanceFromCenter + b0 > 128)
			return DataResult.error(() -> "Structure size including terrain adaptation must not exceed 128");
		return DataResult.success(s);
	}

	public FallbackStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxDepth,
							 boolean useExpansionHack, int maxDistanceFromCenter, int heightTolerance,
							 Holder<StructureSet> set) {
		super(settings);
		this.startPool = startPool;
		this.set = set;
		this.maxDepth = maxDepth;
		this.useExpansionHack = useExpansionHack;
		this.maxDistanceFromCenter = maxDistanceFromCenter;
		this.heightTolerance = heightTolerance;
	}

	private JigsawCheck check() {
		return new JigsawCheck(startPool, maxDepth, useExpansionHack, maxDistanceFromCenter, heightTolerance, biomes());
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
		ChunkPos me = ctx.chunkPos();
		Optional<Resolved> resolved = this.resolve();
		if (resolved.isEmpty()) {
			return Optional.empty();
		}
		Resolved r = resolved.get();
		List<ChunkPos> cands = r.placement().getPotentialChunks(ctx.seed(), me.x, me.z);
		int idx = cands.indexOf(me);
		if (idx < 0) {
			return Optional.empty();
		}
		int spacing = r.placement().spacing();
		int rx = Math.floorDiv(me.x, spacing);
		int rz = Math.floorDiv(me.z, spacing);
		int pick = FlatCheckStructure.pickForRegion(ctx.seed(), rx, rz, r.placement().salt(), r.flatCount());
		FlatCheckStructure flat = r.flatFor(pick);
		int radius = r.flatRadius();
		// Border margin: keeps every cross-region fallback pair farther apart than the radius.
		if (2 * MultiSpreadPlacement.borderDistance(me, spacing) < radius) {
			return Optional.empty();
		}
		if (spacingBlocked(cands, idx, radius, spacing)) {
			return Optional.empty();
		}
		// Each attempt location belongs to one fallback member only.
		if (pickForPosition(ctx.seed(), me.x, me.z, r.placement().salt(), r.fallbacks().size()) != r.index()) {
			return Optional.empty();
		}
		JigsawCheck check = check();
		JigsawCheck.Prefilter pre = check.prefilter(ctx, me);
		if (!pre.pass()) {
			return Optional.empty();
		}
		if (nearFlat(ctx, me, cands, r, flat)) {
			return Optional.empty();
		}
		// The picked flat owns this candidate when it passes; a reserved region has none to yield to.
		if (flat != null && flat.passesCandidate(flat.ctxFor(ctx, me), me, r.placement())) {
			return Optional.empty();
		}
		JigsawCheck.Validated v = check.validateLayout(ctx, me, pre.y());
		return v.pass() ? Optional.of(v.stub()) : Optional.empty();
	}

	/**
	 * The set's placement, flat members, fallback members and this member's position among the fallbacks.
	 */
	private Optional<Resolved> resolve() {
		Holder<StructureSet> holder = this.set;
		if (!holder.isBound()) {
			return Optional.empty();
		}
		if (!(holder.value().placement() instanceof MultiSpreadPlacement placement)) {
			return Optional.empty();
		}
		List<FlatCheckStructure> flats = new ArrayList<>();
		List<FallbackStructure> fallbacks = new ArrayList<>();
		for (var entry : holder.value().structures()) {
			var structure = entry.structure().value();
			if (structure instanceof FlatCheckStructure flat) {
				flats.add(flat);
			} else if (structure instanceof FallbackStructure fallback) {
				fallbacks.add(fallback);
			}
		}
		int index = -1;
		for (int i = 0; i < fallbacks.size(); i++) {
			if (fallbacks.get(i) == this) {
				index = i;
				break;
			}
		}
		if (flats.isEmpty() || index < 0) {
			return Optional.empty();
		}
		return Optional.of(new Resolved(placement, flats, fallbacks, index,
				flats.getFirst().setCount(), flats.getFirst().safetyRadius()));
	}

	private record Resolved(
			MultiSpreadPlacement placement,
			List<FlatCheckStructure> flats,
			List<FallbackStructure> fallbacks,
			int index,
			int flatCount,
			int flatRadius
	) {

		@Nullable
		FlatCheckStructure flatFor(int pick) {
			for (FlatCheckStructure flat : flats) {
				if (flat.setIndex() == pick) {
					return flat;
				}
			}
			return null;
		}

	}

	/**
	 * Index of the fallback associated with a candidate chunk; pure in seed and position. The xor keeps the draw off
	 * the layout RNG that is seeded for the same candidate chunk.
	 */
	public static int pickForPosition(long seed, int chunkX, int chunkZ, int salt, int count) {
		if (count <= 1) {
			return 0;
		}
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureWithSalt(seed, chunkX, chunkZ, salt ^ 0x2545F491);
		return random.nextInt(count);
	}

	/**
	 * Greedy positional spacing: earlier candidates that survive the border margin are kept in order, and a
	 * candidate within the radius of a kept point is skipped. No world state is read for earlier points.
	 */
	private static boolean spacingBlocked(List<ChunkPos> cands, int idx, int radius, int spacing) {
		if (radius <= 0) {
			return false;
		}
		long dist = (long) radius * radius;
		List<ChunkPos> kept = new ArrayList<>();
		for (int j = 0; j < idx; j++) {
			ChunkPos p = cands.get(j);
			if (2 * MultiSpreadPlacement.borderDistance(p, spacing) < radius) {
				continue;
			}
			boolean free = true;
			for (ChunkPos k : kept) {
				if (MultiSpreadPlacement.distanceSquared(p, k) <= dist) {
					free = false;
					break;
				}
			}
			if (free) {
				kept.add(p);
			}
		}
		ChunkPos me = cands.get(idx);
		for (ChunkPos k : kept) {
			if (MultiSpreadPlacement.distanceSquared(me, k) <= dist) {
				return true;
			}
		}
		return false;
	}

	/**
	 * True when a flat start could sit within the radius of {@code me}. Same-region candidates use the picked flat;
	 * neighbours use their own region pick. The flat always wins, so there is no centre-distance tie-break.
	 */
	private static boolean nearFlat(GenerationContext ctx, ChunkPos me, List<ChunkPos> own, Resolved r,
									@Nullable FlatCheckStructure flat) {
		int radius = r.flatRadius();
		if (radius <= 0) {
			return false;
		}
		long dist = (long) radius * radius;
		int spacing = r.placement().spacing();
		if (flat != null) {
			for (ChunkPos c : own) {
				if (c.equals(me)) {
					continue;
				}
				if (MultiSpreadPlacement.distanceSquared(c, me) <= dist && flat.prefilterAt(ctx, c)) {
					return true;
				}
			}
		}
		if (MultiSpreadPlacement.borderDistance(me, spacing) >= radius) {
			return false;
		}
		int rx = Math.floorDiv(me.x, spacing);
		int rz = Math.floorDiv(me.z, spacing);
		for (int drx = -1; drx <= 1; drx++) {
			for (int drz = -1; drz <= 1; drz++) {
				if (drx == 0 && drz == 0) {
					continue;
				}
				int nx = rx + drx, nz = rz + drz;
				FlatCheckStructure nflat = r.flatFor(FlatCheckStructure.pickForRegion(
						ctx.seed(), nx, nz, r.placement().salt(), r.flatCount()));
				if (nflat == null) {
					continue;
				}
				for (ChunkPos c : r.placement().getPotentialChunks(ctx.seed(), nx * spacing, nz * spacing)) {
					if (MultiSpreadPlacement.distanceSquared(c, me) <= dist && nflat.prefilterAt(ctx, c)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public StructureType<?> type() {
		return GLWorldGen.FALLBACK.get();
	}

}
