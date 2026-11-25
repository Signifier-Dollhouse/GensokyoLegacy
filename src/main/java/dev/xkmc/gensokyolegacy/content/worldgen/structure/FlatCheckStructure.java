package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain.TerrainPredicate;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
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

	public static final Codec<FlatCheckStructure> CODEC = ExtraCodecs.validate(RecordCodecBuilder.<FlatCheckStructure>create((i) -> i.group(
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
	).apply(i, FlatCheckStructure::new)), FlatCheckStructure::verifyRange);

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
	private final RegionMemo memo = new RegionMemo();

	private static DataResult<FlatCheckStructure> verifyRange(FlatCheckStructure s) {
		byte b0;
		switch (s.terrainAdaptation()) {
			case NONE:
				b0 = 0;
				break;
			case BURY:
			case BEARD_THIN:
			case BEARD_BOX:
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
		int rx = Math.floorDiv(me.x, place.spacing());
		int rz = Math.floorDiv(me.z, place.spacing());
		if (this.firstPassInRegion(ctx, place, rx, rz) != idx) {
			return Optional.empty();
		}
		return this.check().checkCandidate(ctx, me, place);
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

	/**
	 * Index of the first candidate of region {@code (rx, rz)} where the picked primary passes, or {@code -1}. Pure in
	 * seed, generator and region, so every candidate of the region and the lower tiers share one scan; a member that is
	 * not picked, or a reserved pick, returns {@code -1} without a scan.
	 *
	 * @see "doc/design/structure/secondary.md"
	 */
	int firstPassInRegion(GenerationContext ctx, MultiSpreadPlacement placement, int rx, int rz) {
		Integer cached = memo.get(ctx.chunkGenerator(), ctx.seed(), rx, rz);
		if (cached != null) {
			return cached;
		}
		int ans = -1;
		if (pickForRegion(ctx.seed(), rx, rz, placement.salt(), this.setCount) == this.setIndex) {
			int spacing = placement.spacing();
			List<ChunkPos> cands = placement.getPotentialChunks(ctx.seed(), rx * spacing, rz * spacing);
			JigsawCheck check = check();
			for (int i = 0; i < cands.size(); i++) {
				ChunkPos c = cands.get(i);
				if (check.checkCandidate(JigsawCheck.ctxFor(ctx, c, biomes()), c, placement).isPresent()) {
					ans = i;
					break;
				}
			}
		}
		memo.put(ctx.chunkGenerator(), ctx.seed(), rx, rz, ans);
		return ans;
	}

	/**
	 * Shared eligibility of the template this structure points at.
	 */
	private JigsawCheck check() {
		return new JigsawCheck(startPool, maxDepth, useExpansionHack, maxDistanceFromCenter, terrain,
				biomes(), safetyRadius);
	}

	int setIndex() {
		return setIndex;
	}

	int setCount() {
		return setCount;
	}

	int safetyRadius() {
		return safetyRadius;
	}

	@Override
	public StructureType<?> type() {
		return GLWorldGen.FLAT.get();
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
	 * Chunk spacing of the set that places this structure, or {@code 0} when no multi-spread set places it.
	 */
	public int spacing() {
		return this.placement().map(MultiSpreadPlacement::spacing).orElse(0);
	}

	/**
	 * The single start this structure places in the given region, if any. Mirrors {@link #findGenerationPoint}:
	 * the least-index candidate that passes {@link #checkCandidate} wins, so this agrees with worldgen and reads
	 * no chunks.
	 */
	public Optional<Located> locateRegion(long seed, int regionX, int regionZ, RegistryAccess registries,
										  ChunkGenerator generator, RandomState randomState,
										  StructureTemplateManager templates, LevelHeightAccessor heightAccessor) {
		Optional<MultiSpreadPlacement> resolved = this.placement();
		if (resolved.isEmpty()) return Optional.empty();
		MultiSpreadPlacement placement = resolved.get();
		int spacing = placement.spacing();
		int baseX = regionX * spacing;
		int baseZ = regionZ * spacing;
		GenerationContext ctx = JigsawCheck.context(registries, generator, randomState, templates, seed,
				new ChunkPos(baseX, baseZ), heightAccessor, biomes());
		int idx = firstPassInRegion(ctx, placement, regionX, regionZ);
		if (idx < 0) return Optional.empty();
		ChunkPos cand = placement.getPotentialChunks(seed, baseX, baseZ).get(idx);
		return Optional.of(new Located(cand, placement.getLocatePos(cand), regionX, regionZ));
	}

	/**
	 * A start returned by {@link #locateRegion}: its chunk, the position {@code /locate} would report, and its region.
	 */
	public record Located(ChunkPos chunk, BlockPos pos, int regionX, int regionZ) {
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
			GenerationContext ctx = JigsawCheck.context(registries, generator, randomState, templates, seed, c,
					heightAccessor, biomes());
			if (this.check().prefilter(ctx, c).pass()) prePassed++;
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
				GenerationContext ctx = JigsawCheck.context(registries, generator, randomState, templates, seed, me,
						heightAccessor, biomes());
				var prefilter = this.check().prefilter(ctx, me);
				if (!prefilter.pass()) {
					verdict = "place-" + prefilter.reason();
					stage = 0;
				} else {
					pre++;
					if (!this.check().marginSafe(ctx, me, placement)) {
						verdict = "margin-fail";
						stage = 1;
					} else {
						mar++;
						if (detailed && !detailUsed) trace = new ArrayList<>();
						var v = this.check().validateLayout(ctx, me, prefilter.y(), trace);
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
		if (reason.startsWith("rigid-")) return 4;
		if (reason.equals("sea") || reason.startsWith("height[")) return 3;
		return 2;
	}

}
