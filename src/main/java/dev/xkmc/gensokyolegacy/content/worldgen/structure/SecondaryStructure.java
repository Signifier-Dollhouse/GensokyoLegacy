package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Middle tier of a {@link FlatCheckStructure} set: a smaller flat-check structure that starts at most once per region
 * and only where the region's picked primary placed nothing. It reads the primary's region verdict and never feeds back
 * into it, so the tier graph stays acyclic; the radius and margin rule are the primary's.
 *
 * @see "doc/design/structure/secondary.md"
 */
public class SecondaryStructure extends Structure {

	public static final MapCodec<SecondaryStructure> CODEC = RecordCodecBuilder.<SecondaryStructure>mapCodec((i) -> i.group(
			settingsCodec(i),
			StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter((e) -> e.startPool),
			Codec.intRange(0, 7).fieldOf("size").forGetter((e) -> e.maxDepth),
			Codec.BOOL.fieldOf("use_expansion_hack").forGetter((e) -> e.useExpansionHack),
			Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter((e) -> e.maxDistanceFromCenter),
			Codec.intRange(1, 128).fieldOf("height_tolerance").forGetter((e) -> e.heightTolerance),
			StructureSet.CODEC.fieldOf("set").forGetter((e) -> e.set),
			Codec.intRange(0, 128).optionalFieldOf("set_index", 0).forGetter((e) -> e.setIndex),
			Codec.intRange(1, 128).optionalFieldOf("set_count", 1).forGetter((e) -> e.setCount)
	).apply(i, SecondaryStructure::new)).validate(SecondaryStructure::verifyRange);

	/**
	 * Keeps the secondary pick independent of the primary pick drawn from the same salted random family.
	 */
	public static final int PICK_MIX = 0x3C6EF372;

	public final Holder<StructureTemplatePool> startPool;
	private final Holder<StructureSet> set;
	private final int maxDepth;
	private final boolean useExpansionHack;
	private final int maxDistanceFromCenter;
	private final int heightTolerance;
	private final int setIndex;
	private final int setCount;
	private final RegionMemo memo = new RegionMemo();

	private static DataResult<SecondaryStructure> verifyRange(SecondaryStructure s) {
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

		int i = b0;
		if (s.maxDistanceFromCenter + i > 128)
			return DataResult.error(() -> "Structure size including terrain adaptation must not exceed 128");
		if (s.setIndex < 0 || s.setIndex >= s.setCount)
			return DataResult.error(() -> "set_index " + s.setIndex + " out of range for set_count " + s.setCount);
		return DataResult.success(s);
	}

	public SecondaryStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxDepth,
							  boolean useExpansionHack, int maxDistanceFromCenter, int heightTolerance,
							  Holder<StructureSet> set, int setIndex, int setCount) {
		super(settings);
		this.startPool = startPool;
		this.set = set;
		this.maxDepth = maxDepth;
		this.useExpansionHack = useExpansionHack;
		this.maxDistanceFromCenter = maxDistanceFromCenter;
		this.heightTolerance = heightTolerance;
		this.setIndex = setIndex;
		this.setCount = setCount;
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
		Optional<Resolved> resolved = this.resolve();
		if (resolved.isEmpty()) {
			return Optional.empty();
		}
		Resolved r = resolved.get();
		ChunkPos me = ctx.chunkPos();
		List<ChunkPos> cands = r.placement().getPotentialChunks(ctx.seed(), me.x, me.z);
		int idx = cands.indexOf(me);
		if (idx < 0) {
			return Optional.empty();
		}
		int spacing = r.placement().spacing();
		int rx = Math.floorDiv(me.x, spacing);
		int rz = Math.floorDiv(me.z, spacing);
		if (this.startInRegion(ctx, r, rx, rz) != idx) {
			return Optional.empty();
		}
		return this.checkCandidate(ctx, me, r);
	}

	/**
	 * Index of the secondary's start in region {@code (rx, rz)}, or {@code -1}: the primary must have started nowhere in
	 * the region, this member must be the picked secondary, and it starts at its first passing candidate. Pure in seed,
	 * generator and region, so every candidate of the region and the tiny tier share one scan.
	 */
	int startInRegion(GenerationContext ctx, int rx, int rz) {
		Optional<Resolved> resolved = this.resolve();
		return resolved.map(r -> startInRegion(ctx, r, rx, rz)).orElse(-1);
	}

	private int startInRegion(GenerationContext ctx, Resolved r, int rx, int rz) {
		Integer cached = memo.get(ctx.chunkGenerator(), ctx.seed(), rx, rz);
		if (cached != null) {
			return cached;
		}
		int ans = -1;
		if (FlatCheckStructure.pickForRegion(ctx.seed(), rx, rz, r.placement().salt() ^ PICK_MIX,
				r.secondaryCount()) == this.setIndex) {
			FlatCheckStructure primary = r.primaryFor(FlatCheckStructure.pickForRegion(
					ctx.seed(), rx, rz, r.placement().salt(), r.primaryCount()));
			if (primary == null || primary.firstPassInRegion(ctx, r.placement(), rx, rz) == -1) {
				int spacing = r.placement().spacing();
				List<ChunkPos> cands = r.placement().getPotentialChunks(ctx.seed(), rx * spacing, rz * spacing);
				for (int i = 0; i < cands.size(); i++) {
					ChunkPos c = cands.get(i);
					if (checkCandidate(JigsawCheck.ctxFor(ctx, c, biomes()), c, r).isPresent()) {
						ans = i;
						break;
					}
				}
			}
		}
		memo.put(ctx.chunkGenerator(), ctx.seed(), rx, rz, ans);
		return ans;
	}

	/**
	 * Full eligibility of a candidate chunk: the shared prefilter, margin and layout, plus the border rule against a
	 * neighbouring region's primary start. The primary cannot read the secondary (the tier graph stays acyclic), so the
	 * secondary applies the centre tie-break alone.
	 */
	private Optional<GenerationStub> checkCandidate(GenerationContext ctx, ChunkPos chunkpos, Resolved r) {
		JigsawCheck check = check(r.radius());
		JigsawCheck.Prefilter pre = check.prefilter(ctx, chunkpos);
		if (!pre.pass()) {
			return Optional.empty();
		}
		if (!check.marginSafe(ctx, chunkpos, r.placement())) {
			return Optional.empty();
		}
		if (!neighbourPrimaryFree(ctx, chunkpos, r)) {
			return Optional.empty();
		}
		JigsawCheck.Validated v = check.validateLayout(ctx, chunkpos, pre.y());
		return v.pass() ? Optional.of(v.stub()) : Optional.empty();
	}

	/**
	 * Rejects a candidate within the radius of a neighbouring region's primary start unless the candidate is strictly
	 * closer to its own region centre, the same tie-break the same-tier margin rule uses. The primary is never read
	 * the other way round.
	 */
	private boolean neighbourPrimaryFree(GenerationContext ctx, ChunkPos chunkpos, Resolved r) {
		int radius = r.radius();
		if (radius <= 0) {
			return true;
		}
		int spacing = r.placement().spacing();
		if (MultiSpreadPlacement.borderDistance(chunkpos, spacing) >= radius) {
			return true;
		}
		long dist = (long) radius * radius;
		int rx = Math.floorDiv(chunkpos.x, spacing);
		int rz = Math.floorDiv(chunkpos.z, spacing);
		double myDist = centreDistanceSquared(chunkpos, rx, rz, spacing);
		for (int drx = -1; drx <= 1; drx++) {
			for (int drz = -1; drz <= 1; drz++) {
				if (drx == 0 && drz == 0) {
					continue;
				}
				int nx = rx + drx, nz = rz + drz;
				FlatCheckStructure primary = r.primaryFor(FlatCheckStructure.pickForRegion(
						ctx.seed(), nx, nz, r.placement().salt(), r.primaryCount()));
				if (primary == null) {
					continue;
				}
				int p = primary.firstPassInRegion(ctx, r.placement(), nx, nz);
				if (p < 0) {
					continue;
				}
				ChunkPos start = r.placement().getPotentialChunks(ctx.seed(), nx * spacing, nz * spacing).get(p);
				if (MultiSpreadPlacement.distanceSquared(chunkpos, start) > dist) {
					continue;
				}
				if (centreDistanceSquared(start, nx, nz, spacing) <= myDist) {
					return false;
				}
			}
		}
		return true;
	}

	private static double centreDistanceSquared(ChunkPos pos, int rx, int rz, int spacing) {
		double cx = rx * spacing + (spacing - 1) / 2.0;
		double cz = rz * spacing + (spacing - 1) / 2.0;
		double dx = pos.x - cx, dz = pos.z - cz;
		return dx * dx + dz * dz;
	}

	/**
	 * The placement, primary members, this member's index among the secondaries and the shared radius.
	 */
	private Optional<Resolved> resolve() {
		Holder<StructureSet> holder = this.set;
		if (!holder.isBound()) {
			return Optional.empty();
		}
		if (!(holder.value().placement() instanceof MultiSpreadPlacement placement)) {
			return Optional.empty();
		}
		List<FlatCheckStructure> primaries = new ArrayList<>();
		List<SecondaryStructure> secondaries = new ArrayList<>();
		for (var entry : holder.value().structures()) {
			var structure = entry.structure().value();
			if (structure instanceof FlatCheckStructure flat) {
				primaries.add(flat);
			} else if (structure instanceof SecondaryStructure secondary) {
				secondaries.add(secondary);
			}
		}
		int index = secondaries.indexOf(this);
		if (primaries.isEmpty() || index < 0) {
			return Optional.empty();
		}
		int radius = 0;
		for (var flat : primaries) {
			radius = Math.max(radius, flat.safetyRadius());
		}
		return Optional.of(new Resolved(placement, primaries, index,
				secondaries.getFirst().setCount(), primaries.getFirst().setCount(), radius));
	}

	private record Resolved(
			MultiSpreadPlacement placement,
			List<FlatCheckStructure> primaries,
			int index,
			int secondaryCount,
			int primaryCount,
			int radius
	) {

		@Nullable
		FlatCheckStructure primaryFor(int pick) {
			for (FlatCheckStructure flat : primaries) {
				if (flat.setIndex() == pick) {
					return flat;
				}
			}
			return null;
		}

	}

	private JigsawCheck check(int radius) {
		return new JigsawCheck(startPool, maxDepth, useExpansionHack, maxDistanceFromCenter, heightTolerance,
				biomes(), radius);
	}

	int setIndex() {
		return setIndex;
	}

	int setCount() {
		return setCount;
	}

	@Override
	public StructureType<?> type() {
		return GLWorldGen.SECONDARY.get();
	}

	/**
	 * Dev diagnostic: the primary verdict, the secondary picks and a verdict per candidate, placing nothing.
	 */
	public List<String> diagnoseRegion(long seed, int regionX, int regionZ, RegistryAccess registries,
									   ChunkGenerator generator, RandomState randomState, StructureTemplateManager templates,
									   LevelHeightAccessor heightAccessor) {
		List<String> lines = new ArrayList<>();
		Optional<Resolved> resolved = this.resolve();
		if (resolved.isEmpty()) {
			lines.add("no multi-spread structure set with a primary places this structure");
			return lines;
		}
		Resolved r = resolved.get();
		MultiSpreadPlacement placement = r.placement();
		int spacing = placement.spacing();
		List<ChunkPos> cands = placement.getPotentialChunks(seed, regionX * spacing, regionZ * spacing);
		GenerationContext ctx = JigsawCheck.context(registries, generator, randomState, templates, seed,
				cands.getFirst(), heightAccessor, biomes());
		int pick = FlatCheckStructure.pickForRegion(seed, regionX, regionZ, placement.salt(), r.primaryCount());
		FlatCheckStructure primary = r.primaryFor(pick);
		int p = primary == null ? -1 : primary.firstPassInRegion(ctx, placement, regionX, regionZ);
		int myPick = FlatCheckStructure.pickForRegion(seed, regionX, regionZ, placement.salt() ^ PICK_MIX,
				r.secondaryCount());
		int s = this.startInRegion(ctx, r, regionX, regionZ);
		lines.add("region [%d, %d] spacing %d candidates %d".formatted(regionX, regionZ, spacing, cands.size()));
		lines.add("  primary pick #%d (%s) p=%d".formatted(pick, primary == null ? "reserved" : "member", p));
		lines.add("  secondary pick #%d (mine #%d of %d) s=%d".formatted(myPick, r.index(), r.secondaryCount(), s));
		for (int i = 0; i < cands.size(); i++) {
			ChunkPos c = cands.get(i);
			String verdict;
			if (p == i) {
				verdict = "primary-started";
			} else if (s == i) {
				verdict = "secondary-start";
			} else if (p >= 0) {
				verdict = "suppressed-by-primary-#" + p;
			} else if (myPick != r.index()) {
				verdict = "not-picked";
			} else if (s >= 0 && i < s) {
				verdict = "suppressed-by-secondary-#" + s;
			} else {
				verdict = "place-fail";
			}
			lines.add("  #%d (%d, %d): %s".formatted(i, c.x, c.z, verdict));
		}
		return lines;
	}

}
