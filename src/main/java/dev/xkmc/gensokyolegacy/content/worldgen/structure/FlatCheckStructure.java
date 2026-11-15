package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain.FitResult;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain.TerrainFit;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain.TerrainPredicate;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Port of YoukaiHomecoming 1.20.1 FlatStructure, paired with {@link MultiSpreadPlacement}. Every check is a pure
 * function of seed and position, so exactly one start per region is placed whatever the chunk generation order. Spacing,
 * spread type, salt and attempts are read from {@link #set}, the structure set that places this structure, rather than
 * copied here, so the two sides cannot disagree. setCount/setIndex must never change once released.
 *
 * @see "doc/design/structure/placement.md"
 * @see "doc/design/structure/terrain_fit.md"
 */
public class FlatCheckStructure extends Structure {

	public static final MapCodec<FlatCheckStructure> CODEC = RecordCodecBuilder.<FlatCheckStructure>mapCodec((i) -> i.group(
			settingsCodec(i),
			StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter((e) -> e.startPool),
			Codec.intRange(0, 7).fieldOf("size").forGetter((e) -> e.maxDepth),
			Codec.BOOL.fieldOf("use_expansion_hack").forGetter((e) -> e.useExpansionHack),
			Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter((e) -> e.maxDistanceFromCenter),
			TerrainPredicate.CODEC.fieldOf("terrain").forGetter((e) -> e.terrain),
			StructureSet.CODEC.fieldOf("set").forGetter((e) -> e.set),
			Codec.intRange(0, 16).optionalFieldOf("safety_radius", 8).forGetter((e) -> e.safetyRadius),
			Codec.intRange(0, 128).optionalFieldOf("set_index", 0).forGetter((e) -> e.setIndex),
			Codec.intRange(1, 128).optionalFieldOf("set_count", 1).forGetter((e) -> e.setCount)
	).apply(i, FlatCheckStructure::new)).validate(FlatCheckStructure::verifyRange);

	public final Holder<StructureTemplatePool> startPool;
	/**
	 * The structure set that places this structure; its placement carries spacing, spread type, salt and attempts. The
	 * codec can only name it, so the holder arrives unbound and {@link #placement} swaps in the registry's own the
	 * first time worldgen reads it.
	 */
	private final Holder<StructureSet> set;
	private final int maxDepth;
	private final boolean useExpansionHack;
	private final int maxDistanceFromCenter;
	private final TerrainPredicate terrain;
	private final int safetyRadius;
	private final int setIndex;
	private final int setCount;

	private static DataResult<FlatCheckStructure> verifyRange(FlatCheckStructure s) {
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

	public FlatCheckStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxDepth, boolean useExpansionHack, int maxDistanceFromCenter, TerrainPredicate terrain,
							  Holder<StructureSet> set, int safetyRadius, int setIndex, int setCount) {
		super(settings);
		this.startPool = startPool;
		this.set = set;
		this.maxDepth = maxDepth;
		this.useExpansionHack = useExpansionHack;
		this.maxDistanceFromCenter = maxDistanceFromCenter;
		this.terrain = terrain;
		this.safetyRadius = safetyRadius;
		this.setIndex = setIndex;
		this.setCount = setCount;
	}

	/**
	 * The placement of {@link #set}, empty when the named set does not exist or is placed by something other than
	 * {@link MultiSpreadPlacement} (whose candidate stream this structure is built on). This is the only read of the
	 * holder, and the only place it gets bound: the codec can only name the set, because structures decode before any
	 * structure set does.
	 */
	private Optional<MultiSpreadPlacement> placement() {
		Holder<StructureSet> holder = this.set;
		if (!holder.isBound()) {
			return Optional.empty();
		}
		if (!(holder.value().placement() instanceof MultiSpreadPlacement multi)) {
			return Optional.empty();
		}
		return Optional.of(multi);
	}

	/**
	 * Index of the set member picked for the region {@code (rx, rz)}. Drawn from its own salted random, never the
	 * candidate stream, so a pick can not shift candidate positions; pure in seed and region, so every member of the
	 * set computes the same pick.
	 */
	public static int pickForRegion(long seed, int rx, int rz, int salt, int count) {
		if (count <= 1) {
			return 0;
		}
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureWithSalt(seed, rx, rz, salt ^ 0x9E3779B9);
		return random.nextInt(count);
	}

	private boolean checkRegionPicked(long seed, MultiSpreadPlacement placement, ChunkPos chunkpos) {
		if (this.setCount <= 1) {
			return true;
		}
		int rx = Math.floorDiv(chunkpos.x, placement.spacing());
		int rz = Math.floorDiv(chunkpos.z, placement.spacing());
		return pickForRegion(seed, rx, rz, placement.salt(), this.setCount) == this.setIndex;
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
		ChunkPos me = ctx.chunkPos();
		Optional<MultiSpreadPlacement> placement = this.placement();
		if (placement.isEmpty()) {
			return Optional.empty();
		}
		MultiSpreadPlacement place = placement.get();
		List<ChunkPos> cands = place.getPotentialChunks(ctx.seed(), me.x, me.z);
		int idx = cands.indexOf(me);
		if (idx < 0) {
			return Optional.empty();
		}
		if (!this.checkRegionPicked(ctx.seed(), place, me)) {
			return Optional.empty();
		}
		for (int j = 0; j < idx; j++) {
			ChunkPos sib = cands.get(j);
			GenerationContext sctx = new GenerationContext(ctx.registryAccess(), ctx.chunkGenerator(), ctx.biomeSource(),
					ctx.randomState(), ctx.structureTemplateManager(), ctx.seed(), sib, ctx.heightAccessor(), this.biomes()::contains);
			if (this.checkCandidate(sctx, sib, place).isPresent()) {
				return Optional.empty();
			}
		}
		return this.checkCandidate(ctx, me, place);
	}

	/**
	 * Full eligibility of a candidate chunk, cheapest gates first: middle-column prefilter, margin
	 * safety at the provisional height, then the speculative jigsaw layout with tree checks.
	 */
	private Optional<GenerationStub> checkCandidate(GenerationContext ctx, ChunkPos chunkpos, MultiSpreadPlacement placement) {
		Prefilter pre = prefilter(ctx, chunkpos);
		if (!pre.pass()) {
			return Optional.empty();
		}
		if (!this.checkMarginSafe(ctx, chunkpos, placement)) {
			return Optional.empty();
		}
		Validated v = this.validateLayout(ctx, chunkpos, pre.y());
		if (!v.pass()) {
			return Optional.empty();
		}
		return Optional.of(v.stub());
	}

	private record Validated(boolean pass, GenerationStub stub, String reason) {

		static Validated fail(String reason) {
			return new Validated(false, null, reason);
		}

		static Validated pass(GenerationStub stub) {
			return new Validated(true, stub, "pass");
		}

	}

	/**
	 * Builds the jigsaw layout speculatively (provisional Y only fixes XZ), runs the biome gate, then lets
	 * {@link #terrain} place the layout on the terrain. On success returns a stub serving the shifted pieces;
	 * the predicate's foundation runs later in {@link #afterPlace}. A non-null {@code trace} collects the
	 * predicate's numbers when the fit fails.
	 */
	private Validated validateLayout(GenerationContext ctx, ChunkPos chunkpos, int provY) {
		return validateLayout(ctx, chunkpos, provY, null);
	}

	private Validated validateLayout(GenerationContext ctx, ChunkPos chunkpos, int provY, List<String> trace) {
		if (!this.terrain.triesAllRotations()) {
			return validateLayout(ctx, chunkpos, provY, trace, -1);
		}
		Validated firstFail = null;
		for (Rotation rotation : Rotation.getShuffled(seededRandom(ctx.seed(), chunkpos))) {
			Validated v = validateLayout(ctx, chunkpos, provY, firstFail == null ? trace : null, rotation.ordinal());
			if (v.pass()) {
				return v;
			}
			if (firstFail == null) {
				firstFail = v;
			}
		}
		return firstFail;
	}

	/**
	 * As {@link #validateLayout(GenerationContext, ChunkPos, int, List)}, but {@code rotation >= 0} forces the
	 * start rotation instead of the one drawn from {@code ctx.random()}.
	 */
	private Validated validateLayout(GenerationContext ctx, ChunkPos chunkpos, int provY, List<String> trace, int rotation) {
		if (rotation >= 0) {
			ctx = ctxForRotation(ctx, rotation);
		}
		int sea = ctx.chunkGenerator().getSeaLevel();
		BlockPos start = new BlockPos(chunkpos.getMinBlockX(), provY, chunkpos.getMinBlockZ());
		Optional<GenerationStub> layout = JigsawPlacement.addPieces(ctx, this.startPool, Optional.empty(), this.maxDepth, start,
				this.useExpansionHack, Optional.empty(), this.maxDistanceFromCenter,
				PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING);
		if (layout.isEmpty()) {
			return Validated.fail("no-layout");
		}
		List<PoolElementStructurePiece> pieces = new ArrayList<>();
		for (StructurePiece p : layout.get().getPiecesBuilder().build().pieces()) {
			if (p instanceof PoolElementStructurePiece pool) {
				TerrainAdjustment adjustment = this.terrain.beardAdjustment(pool);
				pieces.add(adjustment == null ? pool
						: GLBeardedPiece.of(ctx.structureTemplateManager(), pool, LiquidSettings.IGNORE_WATERLOGGING, adjustment));
			}
		}
		if (pieces.isEmpty()) {
			return Validated.fail("no-layout");
		}
		PieceTree tree = PieceTree.build(pieces);
		// Total-bound biome check: 4 union-box corners + 4 edge centers.
		BoundingBox union = tree.unionBox();
		int ux0 = union.minX(), uz0 = union.minZ(), ux1 = union.maxX(), uz1 = union.maxZ();
		int ucx = (ux0 + ux1) / 2, ucz = (uz0 + uz1) / 2;
		int[][] biomePts = {{ux0, uz0}, {ux0, uz1}, {ux1, uz0}, {ux1, uz1},
				{ucx, uz0}, {ucx, uz1}, {ux0, ucz}, {ux1, ucz}};
		for (int[] q : biomePts) {
			int h = freeHeight(ctx, q[0], q[1]);
			var biome = ctx.chunkGenerator().getBiomeSource().getNoiseBiome(
					QuartPos.fromBlock(q[0]), QuartPos.fromBlock(h), QuartPos.fromBlock(q[1]), ctx.randomState().sampler());
			if (!biomes().contains(biome)) {
				return Validated.fail("biome");
			}
		}
		TerrainFit fit = new TerrainFit(ctx, tree, sea);
		FitResult result = this.terrain.fit(fit);
		if (!result.pass()) {
			if (trace != null) {
				trace.add("fit: " + result.reason());
				trace.addAll(this.terrain.describe(fit));
			}
			return Validated.fail(result.reason());
		}
		BlockPos pos = layout.get().position().offset(0, result.delta(), 0);
		GenerationStub stub = new GenerationStub(pos, b -> pieces.forEach(b::addPiece));
		return Validated.pass(stub);
	}

	/**
	 * Runs the terrain predicate's foundation pass once per chunk, after that chunk's jigsaw pieces are placed.
	 */
	@Override
	public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
						   RandomSource random, BoundingBox box, ChunkPos chunkPos, PiecesContainer pieces) {
		this.terrain.afterPlace(level, box, pieces);
	}

	/**
	 * The Beardifier drops structures whose adaptation is NONE before it reads any piece, so a sparse-beard rule
	 * advertises BEARD_THIN here while each piece still reports its own adjustment. The configured JSON value stays
	 * NONE: only the gate keeps a beard.
	 */
	@Override
	public TerrainAdjustment terrainAdaptation() {
		TerrainAdjustment configured = super.terrainAdaptation();
		return configured == TerrainAdjustment.NONE && this.terrain.needsBeardifier()
				? TerrainAdjustment.BEARD_THIN : configured;
	}

	private static int freeHeight(GenerationContext ctx, int x, int z) {
		return ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
	}

	/**
	 * The candidate's own seeded random, used for the rotation shuffle so the layout stream stays untouched.
	 */
	private static WorldgenRandom seededRandom(long seed, ChunkPos pos) {
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureSeed(seed, pos.x, pos.z);
		return random;
	}

	private static GenerationContext ctxForRotation(GenerationContext ctx, int rotation) {
		return new GenerationContext(ctx.registryAccess(), ctx.chunkGenerator(), ctx.biomeSource(), ctx.randomState(),
				ctx.structureTemplateManager(), new RotationRandom(rotation, ctx.seed(), ctx.chunkPos()), ctx.seed(),
				ctx.chunkPos(), ctx.heightAccessor(), ctx.validBiome());
	}

	/**
	 * Returns {@code rotation} from the first {@code nextInt(4)} and delegates everything else, so the rest of the
	 * layout sees the same random stream the drawn rotation would have consumed.
	 */
	private static final class RotationRandom extends WorldgenRandom {

		private final int rotation;
		private boolean forced;

		RotationRandom(int rotation, long seed, ChunkPos pos) {
			super(new LegacyRandomSource(0L));
			this.rotation = rotation;
			this.setLargeFeatureSeed(seed, pos.x, pos.z);
		}

		@Override
		public int nextInt(int bound) {
			if (!this.forced && bound == Rotation.values().length) {
				this.forced = true;
				super.nextInt(bound);
				return this.rotation;
			}
			return super.nextInt(bound);
		}

	}

	/**
	 * Cheap middle-column sea/biome gate. Rejects unsuitable candidates
	 * before the speculative jigsaw layout is built.
	 */
	private record Prefilter(boolean pass, int y, String reason) {
	}

	private Prefilter prefilter(GenerationContext ctx, ChunkPos chunkpos) {
		int y = freeHeight(ctx, chunkpos.getMiddleBlockX(), chunkpos.getMiddleBlockZ());
		if (y <= ctx.chunkGenerator().getSeaLevel()) {
			return new Prefilter(false, y, "sea");
		}
		var biome = ctx.chunkGenerator().getBiomeSource().getNoiseBiome(
				QuartPos.fromBlock(chunkpos.getMiddleBlockX()), QuartPos.fromBlock(y),
				QuartPos.fromBlock(chunkpos.getMiddleBlockZ()), ctx.randomState().sampler());
		if (!biomes().contains(biome)) {
			return new Prefilter(false, y, "biome");
		}
		return new Prefilter(true, y, "pass");
	}

	/**
	 * A candidate within {@code safetyRadius} chunks of its region border is rejected when a neighbouring region hosts
	 * a candidate inside the same radius that passes the prefilter and lies at most as close to its own region center
	 * as we do to ours. The closer of two colliding candidates survives, exact ties reject both, candidates deep inside
	 * their region are always safe, and no jigsaw layouts are built here.
	 */
	private boolean checkMarginSafe(GenerationContext ctx, ChunkPos chunkpos, MultiSpreadPlacement placement) {
		if (this.safetyRadius <= 0) {
			return true;
		}
		int spacing = placement.spacing();
		int rx = Math.floorDiv(chunkpos.x, spacing);
		int rz = Math.floorDiv(chunkpos.z, spacing);
		int minX = rx * spacing;
		int minZ = rz * spacing;
		int maxX = minX + spacing - 1;
		int maxZ = minZ + spacing - 1;
		int borderDist = Math.min(Math.min(chunkpos.x - minX, maxX - chunkpos.x),
				Math.min(chunkpos.z - minZ, maxZ - chunkpos.z));
		if (borderDist >= this.safetyRadius) {
			return true;
		}
		double cxc = minX + (spacing - 1) / 2.0;
		double czc = minZ + (spacing - 1) / 2.0;
		double myDx = chunkpos.x - cxc, myDz = chunkpos.z - czc;
		double myDist = myDx * myDx + myDz * myDz;
		long dist = (long) this.safetyRadius * this.safetyRadius;
		for (int drx = -1; drx <= 1; drx++) {
			for (int drz = -1; drz <= 1; drz++) {
				if (drx == 0 && drz == 0) {
					continue;
				}
				int nx = rx + drx, nz = rz + drz;
				double nxc = nx * spacing + (spacing - 1) / 2.0;
				double nzc = nz * spacing + (spacing - 1) / 2.0;
				for (ChunkPos c : placement.getPotentialChunks(ctx.seed(), nx * spacing, nz * spacing)) {
					int dxq = c.x - chunkpos.x, dzq = c.z - chunkpos.z;
					if ((long) dxq * dxq + (long) dzq * dzq > dist) {
						continue;
					}
					double dxc = c.x - nxc, dzc = c.z - nzc;
					double cDist = dxc * dxc + dzc * dzc;
					if (cDist > myDist) {
						continue;
					}
					GenerationContext sctx = this.ctxFor(ctx.registryAccess(), ctx.chunkGenerator(),
							ctx.randomState(), ctx.structureTemplateManager(), ctx.seed(), c, ctx.heightAccessor());
					if (this.prefilter(sctx, c).pass()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public StructureType<?> type() {
		return GLWorldGen.FLAT.get();
	}

	private GenerationContext ctxFor(RegistryAccess registries, ChunkGenerator generator, RandomState randomState,
									 StructureTemplateManager templates, long seed, ChunkPos pos, LevelHeightAccessor heightAccessor) {
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureSeed(seed, pos.x, pos.z);
		return new GenerationContext(registries, generator, generator.getBiomeSource(), randomState,
				templates, random, seed, pos, heightAccessor, this.biomes()::contains);
	}

	/**
	 * Per-region funnel counts, cumulative: each bucket counts attempts
	 * passing that gate and every earlier one.
	 */
	public record RegionStats(int tried, int prefilter, int margin, int biome, int root, int detailed, int pass) {

		public RegionStats add(RegionStats o) {
			return new RegionStats(tried + o.tried, prefilter + o.prefilter, margin + o.margin,
					biome + o.biome, root + o.root, detailed + o.detailed, pass + o.pass);
		}

		public static RegionStats zero() {
			return new RegionStats(0, 0, 0, 0, 0, 0, 0);
		}

	}

	public record RegionReport(List<String> lines, RegionStats stats) {
	}

	/**
	 * Dev diagnostic for the region containing {@code chunkpos}; see {@link #diagnoseRegion}.
	 */
	public RegionReport diagnoseAt(long seed, ChunkPos chunkpos, RegistryAccess registries, ChunkGenerator generator,
								   RandomState randomState, StructureTemplateManager templates,
								   LevelHeightAccessor heightAccessor, boolean detailed) {
		Optional<MultiSpreadPlacement> resolved = this.placement();
		if (resolved.isEmpty()) {
			return new RegionReport(List.of("no multi-spread structure set places this structure"), RegionStats.zero());
		}
		int spacing = resolved.get().spacing();
		return diagnoseRegion(seed, Math.floorDiv(chunkpos.x, spacing), Math.floorDiv(chunkpos.z, spacing),
				registries, generator, randomState, templates, heightAccessor, detailed);
	}

	/**
	 * Dev diagnostic: lists every attempted position in one region with its verdict (suppressed / place-fail /
	 * margin-fail / pass(checks)) plus the cumulative funnel line, placing nothing. Mirrors {@link
	 * #findGenerationPoint}, and stops at the first full pass, so later attempts are untried. With {@code detailed},
	 * the first candidate to reach the terrain fit also prints the predicate's own numbers.
	 */
	public RegionReport diagnoseRegion(long seed, int regionX, int regionZ, RegistryAccess registries,
									   ChunkGenerator generator, RandomState randomState, StructureTemplateManager templates,
									   LevelHeightAccessor heightAccessor, boolean detailed) {
		List<String> lines = new ArrayList<>();
		Optional<MultiSpreadPlacement> resolved = this.placement();
		if (resolved.isEmpty()) {
			lines.add("no multi-spread structure set places this structure");
			return new RegionReport(lines, RegionStats.zero());
		}
		MultiSpreadPlacement placement = resolved.get();
		int spacing = placement.spacing();
		List<ChunkPos> cands = placement.getPotentialChunks(seed, regionX * spacing, regionZ * spacing);
		StringBuilder head = new StringBuilder("region [%d, %d] spacing %d salt %d candidates:".formatted(
				regionX, regionZ, spacing, placement.salt()));
		for (int n = 0; n < cands.size(); n++) {
			ChunkPos c = cands.get(n);
			head.append(" #%d (%d, %d)".formatted(n, c.x, c.z));
		}
		lines.add(head.toString());
		int prePassed = 0;
		for (ChunkPos c : cands) {
			GenerationContext ctx = this.ctxFor(registries, generator, randomState, templates, seed, c, heightAccessor);
			if (this.prefilter(ctx, c).pass()) prePassed++;
		}
		lines.add("  prefilter: %d/%d passed".formatted(prePassed, cands.size()));
		int pick = pickForRegion(seed, regionX, regionZ, placement.salt(), this.setCount);
		lines.add("  region pick: #%d (mine #%d of %d)".formatted(pick, this.setIndex, this.setCount));
		if (pick != this.setIndex) {
			for (int i = 0; i < cands.size(); i++) {
				ChunkPos me = cands.get(i);
				lines.add("  #%d (%d, %d): suppressed-by-pick-#%d".formatted(i, me.x, me.z, pick));
			}
			lines.add("  funnel tried=0 prefilter=0 margin=0 biome=0 root=0 detailed=0 pass=0");
			return new RegionReport(lines, RegionStats.zero());
		}
		int tried = 0, pre = 0, mar = 0, bio = 0, roo = 0, det = 0, pas = 0;
		int firstPass = -1;
		boolean detailUsed = false;
		for (int i = 0; i < cands.size(); i++) {
			ChunkPos me = cands.get(i);
			String verdict;
			int stage;
			List<String> trace = null;
			if (firstPass >= 0) {
				verdict = "suppressed-by-#" + firstPass;
				stage = -1;
			} else {
				tried++;
				GenerationContext ctx = this.ctxFor(registries, generator, randomState, templates, seed, me, heightAccessor);
				var prefilter = this.prefilter(ctx, me);
				if (!prefilter.pass()) {
					verdict = "place-" + prefilter.reason();
					stage = 0;
				} else {
					pre++;
					if (!this.checkMarginSafe(ctx, me, placement)) {
						verdict = "margin-fail";
						stage = 1;
					} else {
						mar++;
						if (detailed && !detailUsed) trace = new ArrayList<>();
						var v = this.validateLayout(ctx, me, prefilter.y(), trace);
						if (trace != null && !trace.isEmpty()) detailUsed = true;
						if (!v.pass()) {
							verdict = "place-" + v.reason();
							stage = stageOf(v.reason());
						} else {
							verdict = "pass(checks)";
							stage = 5;
							pas++;
							firstPass = i;
						}
					}
				}
			}
			if (stage >= 3) bio++;
			if (stage >= 4) roo++;
			if (stage >= 5) det++;
			lines.add("  #%d (%d, %d): %s".formatted(i, me.x, me.z, verdict));
			if (trace != null) {
				for (String line : trace) lines.add("      " + line);
			}
		}
		lines.add("  funnel tried=%d prefilter=%d margin=%d biome=%d root=%d detailed=%d pass=%d".formatted(
				tried, pre, mar, bio, roo, det, pas));
		return new RegionReport(lines, new RegionStats(tried, pre, mar, bio, roo, det, pas));
	}

	/**
	 * Furthest gate passed for a layout failure reason: 2 = margin (layout or detailed biome
	 * failed), 3 = detailed biome (root sea/variation), 4 = root (rigid sea/variation), 5 = joint.
	 */
	private static int stageOf(String reason) {
		if (reason.startsWith("joint[")) return 5;
		if (reason.startsWith("rigid-") || reason.startsWith("anchor[")) return 4;
		if (reason.equals("sea") || reason.equals("anchor-sea") || reason.startsWith("height[")) return 3;
		return 2;
	}

}
