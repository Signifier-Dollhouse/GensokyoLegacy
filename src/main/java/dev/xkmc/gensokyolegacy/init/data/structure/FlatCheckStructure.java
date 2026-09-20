package dev.xkmc.gensokyolegacy.init.data.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Port of YoukaiHomecoming 1.20.1 FlatStructure, paired with
 * {@link MultiSpreadPlacement}.
 * Multi-piece placement check. A speculative jigsaw layout is built per
 * candidate chunk, then validated in order:
 * <ol>
 * <li>Biome check on 8 total-bound points (4 union-box corners + 4 edge
 * centers).</li>
 * <li>Root footprint check: 9 terrain samples (corners + edge centers +
 * center of the start piece box). Rejects on sea level or when
 * {@code max - min > 2 * heightTolerance}; the average sets the spawn
 * height and the root rigid group is shifted to it.</li>
 * <li>Rigid check: every rigid piece's center terrain height must be within
 * {@code heightTolerance} of its placed ground level
 * ({@code boundingBox.minY + groundLevelDelta}).</li>
 * <li>Joint check: for every connected non-rigid subgraph, the placed
 * ground levels of all adjacent rigid groups must be within
 * {@code heightTolerance} of each other.</li>
 * </ol>
 * Rigid pieces linked by rigid-only paths form one group sharing the entry
 * (first) rigid piece as reference; non-rigid joints reset the budget so
 * total drift may accumulate along flexible chains. Sea level is checked
 * together with every height sample. {@code flatCheckRange} is retained in
 * the codec for datapack compatibility and no longer sizes a grid.
 *
 * <p>Multi-attempt: the placement yields {@code attempts} uniform candidates
 * per region (covering the full region, including the old separation margin
 * band). Every candidate recomputes the same ordered candidate list and only
 * the least-index candidate passing the full check below builds, so exactly
 * one structure start is placed per region. The checks are pure functions of
 * world seed and position, making the winner independent of chunk generation
 * order. {@code spacing}, {@code spreadType}, {@code salt} and
 * {@code attempts} must match the structure set placement (synced by datagen).
 *
 * <p>Margin band: a candidate within {@code safetyRadius} chunks of its
 * region border is only allowed when every chunk within {@code safetyRadius}
 * of it that falls outside its own region fails the structure biome check
 * (sampled at the candidate ground level). Then no neighboring region could
 * host an overlapping structure there.
 */
public class FlatCheckStructure extends Structure {

	public static final MapCodec<FlatCheckStructure> CODEC = RecordCodecBuilder.<FlatCheckStructure>mapCodec((i) -> i.group(
			settingsCodec(i),
			StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter((e) -> e.startPool),
			Codec.intRange(0, 7).fieldOf("size").forGetter((e) -> e.maxDepth),
			Codec.BOOL.fieldOf("use_expansion_hack").forGetter((e) -> e.useExpansionHack),
			Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter((e) -> e.maxDistanceFromCenter),
			Codec.intRange(1, 128).fieldOf("flat_check_range").forGetter((e) -> e.flatCheckRange),
			Codec.intRange(1, 128).fieldOf("height_tolerance").forGetter((e) -> e.flatTolerance),
			Codec.intRange(1, 32).fieldOf("attempts").forGetter((e) -> e.attempts),
			Codec.intRange(1, 4096).fieldOf("spacing").forGetter((e) -> e.spacing),
			RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter((e) -> e.spreadType),
			ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter((e) -> e.salt),
			Codec.intRange(0, 16).optionalFieldOf("safety_radius", 8).forGetter((e) -> e.safetyRadius)
	).apply(i, FlatCheckStructure::new)).validate(FlatCheckStructure::verifyRange);

	public final Holder<StructureTemplatePool> startPool;
	private final int maxDepth;
	private final boolean useExpansionHack;
	private final int maxDistanceFromCenter;
	private final int flatCheckRange;
	private final int flatTolerance;
	private final int attempts;
	private final int spacing;
	private final RandomSpreadType spreadType;
	private final int salt;
	private final int safetyRadius;

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
		return s.maxDistanceFromCenter + i > 128 ? DataResult.error(() -> "Structure size including terrain adaptation must not exceed 128") : DataResult.success(s);
	}

	public FlatCheckStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxDepth, boolean useExpansionHack, int maxDistanceFromCenter, int flatCheckRange, int flatTolerance,
							  int attempts, int spacing, RandomSpreadType spreadType, int salt, int safetyRadius) {
		super(settings);
		this.startPool = startPool;
		this.maxDepth = maxDepth;
		this.useExpansionHack = useExpansionHack;
		this.maxDistanceFromCenter = maxDistanceFromCenter;
		this.flatCheckRange = flatCheckRange;
		this.flatTolerance = flatTolerance;
		this.attempts = attempts;
		this.spacing = spacing;
		this.spreadType = spreadType;
		this.salt = salt;
		this.safetyRadius = safetyRadius;
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
		ChunkPos me = ctx.chunkPos();
		List<ChunkPos> cands = MultiSpreadPlacement.candidates(ctx.seed(), me.x, me.z, this.spacing, this.spreadType, this.salt, this.attempts);
		int idx = cands.indexOf(me);
		if (idx < 0) {
			return Optional.empty();
		}
		for (int j = 0; j < idx; j++) {
			ChunkPos sib = cands.get(j);
			GenerationContext sctx = new GenerationContext(ctx.registryAccess(), ctx.chunkGenerator(), ctx.biomeSource(),
					ctx.randomState(), ctx.structureTemplateManager(), ctx.seed(), sib, ctx.heightAccessor(), this.biomes()::contains);
			if (this.checkCandidate(sctx, sib).isPresent()) {
				return Optional.empty();
			}
		}
		return this.checkCandidate(ctx, me);
	}

	/**
	 * Full eligibility of a candidate chunk: speculative jigsaw layout,
	 * biome + flat + deformation checks, plus margin safety.
	 */
	private Optional<GenerationStub> checkCandidate(GenerationContext ctx, ChunkPos chunkpos) {
		Validated v = this.validate(ctx, chunkpos);
		if (!v.pass()) {
			return Optional.empty();
		}
		if (!this.checkMarginSafe(ctx, chunkpos, v.spawnY())) {
			return Optional.empty();
		}
		return Optional.of(v.stub());
	}

	private record Validated(boolean pass, int spawnY, GenerationStub stub, String reason) {

		static Validated fail(String reason) {
			return new Validated(false, 0, null, reason);
		}

	}

	/**
	 * Speculatively builds the jigsaw layout (provisional Y only fixes XZ;
	 * the root rigid group is shifted to the footprint average afterwards)
	 * and runs the biome, root-footprint, rigid and joint checks. On success
	 * returns a stub serving the already-shifted pieces.
	 */
	private Validated validate(GenerationContext ctx, ChunkPos chunkpos) {
		int sea = ctx.chunkGenerator().getSeaLevel();
		int provY = freeHeight(ctx, chunkpos.getMiddleBlockX(), chunkpos.getMiddleBlockZ());
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
				pieces.add(pool);
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
		// Root footprint check: corners + edge centers + center of start box.
		BoundingBox rootBox = pieces.get(0).getBoundingBox();
		int rx0 = rootBox.minX(), rz0 = rootBox.minZ(), rx1 = rootBox.maxX(), rz1 = rootBox.maxZ();
		int rcx = (rx0 + rx1) / 2, rcz = (rz0 + rz1) / 2;
		int[][] rootPts = {{rx0, rz0}, {rx0, rz1}, {rx1, rz0}, {rx1, rz1},
				{rcx, rz0}, {rcx, rz1}, {rx0, rcz}, {rx1, rcz}, {rcx, rcz}};
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (int[] q : rootPts) {
			int h = freeHeight(ctx, q[0], q[1]);
			if (h <= sea) {
				return Validated.fail("sea");
			}
			if (h < min) min = h;
			if (h > max) max = h;
		}
		if (max - min > 2 * this.flatTolerance) {
			return Validated.fail("height[%d-%d]".formatted(min, max));
		}
		int spawn = (max + min) / 2;
		// Shift the start rigid group (assumes a rigid start piece) so its
		// ground sits on the footprint average.
		int delta = tree.shiftStartGroupToGround(spawn);
		// Rigid check: center terrain within tolerance of placed ground.
		for (int i = 0; i < tree.size(); i++) {
			if (!tree.isRigid(i)) continue;
			BoundingBox b = tree.piece(i).getBoundingBox();
			int h = freeHeight(ctx, (b.minX() + b.maxX()) / 2, (b.minZ() + b.maxZ()) / 2);
			if (h <= sea) {
				return Validated.fail("sea");
			}
			int g = tree.groundOf(i);
			if (Math.abs(h - g) > this.flatTolerance) {
				return Validated.fail("rigid-%d[%d-vs-%d]".formatted(i, h, g));
			}
		}
		// Joint check: per connected non-rigid subgraph, adjacent rigid
		// group grounds must lie within tolerance of each other.
		for (Set<Integer> grounds : tree.flexJointGrounds()) {
			if (grounds.size() < 2) continue;
			int lo = grounds.stream().mapToInt(Integer::intValue).min().orElse(0);
			int hi = grounds.stream().mapToInt(Integer::intValue).max().orElse(0);
			if (hi - lo > this.flatTolerance) {
				return Validated.fail("joint[%d-%d]".formatted(lo, hi));
			}
		}
		BlockPos pos = layout.get().position().offset(0, delta, 0);
		GenerationStub stub = new GenerationStub(pos, b -> pieces.forEach(b::addPiece));
		return new Validated(true, spawn, stub, "pass");
	}

	private static int freeHeight(GenerationContext ctx, int x, int z) {
		return ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
	}

	/**
	 * Margin safety: when the candidate is within safetyRadius chunks of its
	 * region border, every chunk within safetyRadius of it that falls outside
	 * its own region must fail the structure biome check, otherwise a
	 * neighboring region could host a colliding structure there. Candidates
	 * deep inside their region are always safe.
	 */
	private boolean checkMarginSafe(GenerationContext ctx, ChunkPos chunkpos, int y) {
		if (this.safetyRadius <= 0) {
			return true;
		}
		int rx = Math.floorDiv(chunkpos.x, this.spacing);
		int rz = Math.floorDiv(chunkpos.z, this.spacing);
		int minX = rx * this.spacing;
		int minZ = rz * this.spacing;
		int maxX = minX + this.spacing - 1;
		int maxZ = minZ + this.spacing - 1;
		int borderDist = Math.min(Math.min(chunkpos.x - minX, maxX - chunkpos.x),
				Math.min(chunkpos.z - minZ, maxZ - chunkpos.z));
		if (borderDist >= this.safetyRadius) {
			return true;
		}
		for (int dx = -this.safetyRadius; dx <= this.safetyRadius; dx++) {
			for (int dz = -this.safetyRadius; dz <= this.safetyRadius; dz++) {
				int cx = chunkpos.x + dx;
				int cz = chunkpos.z + dz;
				if (cx >= minX && cx <= maxX && cz >= minZ && cz <= maxZ) {
					continue;
				}
				int bx = new ChunkPos(cx, cz).getMiddleBlockX();
				int bz = new ChunkPos(cx, cz).getMiddleBlockZ();
				var biome = ctx.chunkGenerator().getBiomeSource().getNoiseBiome(
						QuartPos.fromBlock(bx), QuartPos.fromBlock(y), QuartPos.fromBlock(bz),
						ctx.randomState().sampler());
				if (biomes().contains(biome)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public StructureType<?> type() {
		return GLWorldGen.FLAT.get();
	}

	int spacing() {
		return this.spacing;
	}

	private GenerationContext ctxFor(RegistryAccess registries, ChunkGenerator generator, RandomState randomState,
									 StructureTemplateManager templates, long seed, ChunkPos pos, LevelHeightAccessor heightAccessor) {
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureSeed(seed, pos.x, pos.z);
		return new GenerationContext(registries, generator, generator.getBiomeSource(), randomState,
				templates, random, seed, pos, heightAccessor, this.biomes()::contains);
	}

	/**
	 * Dev diagnostic: lists every attempted position in one region and the
	 * verdict per attempt (suppressed / place-fail / margin-fail /
	 * pass(checks)), without placing anything. Mirrors
	 * {@link #findGenerationPoint} so the trace shows exactly what worldgen
	 * would attempt.
	 */
	List<String> diagnoseRegion(long seed, int regionX, int regionZ, RegistryAccess registries,
								ChunkGenerator generator, RandomState randomState, StructureTemplateManager templates,
								LevelHeightAccessor heightAccessor) {
		List<String> lines = new ArrayList<>();
		List<ChunkPos> cands = MultiSpreadPlacement.candidates(seed, regionX * this.spacing, regionZ * this.spacing,
				this.spacing, this.spreadType, this.salt, this.attempts);
		StringBuilder head = new StringBuilder("region [%d, %d] attempts:".formatted(regionX, regionZ));
		for (int n = 0; n < cands.size(); n++) {
			ChunkPos c = cands.get(n);
			head.append(" #%d (%d, %d)".formatted(n, c.x, c.z));
		}
		lines.add(head.toString());
		for (int i = 0; i < cands.size(); i++) {
			ChunkPos me = cands.get(i);
			String verdict = null;
			for (int j = 0; j < i; j++) {
			ChunkPos sib = cands.get(j);
			GenerationContext sctx = this.ctxFor(registries, generator, randomState, templates, seed, sib, heightAccessor);
			if (this.checkCandidate(sctx, sib).isPresent()) {
				verdict = "suppressed-by-#" + j;
				break;
			}
		}
		if (verdict == null) {
			GenerationContext ctx = this.ctxFor(registries, generator, randomState, templates, seed, me, heightAccessor);
			var v = this.validate(ctx, me);
			if (!v.pass()) verdict = "place-" + v.reason();
			else if (!this.checkMarginSafe(ctx, me, v.spawnY())) verdict = "margin-fail";
			else verdict = "pass(checks)";
		}
			lines.add("  #%d (%d, %d): %s".formatted(i, me.x, me.z, verdict));
		}
		return lines;
	}

}
