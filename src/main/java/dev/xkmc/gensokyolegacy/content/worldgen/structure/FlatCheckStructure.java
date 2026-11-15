package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain.TerrainPredicate;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
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

import java.util.List;
import java.util.Optional;

/**
 * Port of YoukaiHomecoming 1.20.1 FlatStructure, paired with {@link MultiSpreadPlacement}. Every check is a pure
 * function of seed and position, so exactly one start per region is placed whatever the chunk generation order. Spacing,
 * spread type, salt and attempts are read from {@link #set}, the structure set that places this structure, rather than
 * copied here, so the two sides cannot disagree. setCount/setIndex must never change once released.
 *
 * @see "doc/design/structure/placement.md"
 * @see "doc/design/structure/secondary.md"
 * @see "doc/design/structure/terrain_fit.md"
 */
public class FlatCheckStructure extends Structure implements LocatableStructure {

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

	public FlatCheckStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxDepth,
							  boolean useExpansionHack, int maxDistanceFromCenter, TerrainPredicate terrain,
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
	 * Index of the first candidate of region {@code (rx, rz)} where the picked primary passes, or {@code -1}. Pure in
	 * seed, generator and region, so every candidate of the region and the lower tiers share one scan; a member that is
	 * not picked, or a reserved pick, returns {@code -1} without a scan.
	 *
	 * @see "doc/design/structure/secondary.md"
	 */
	int firstPassInRegion(GenerationContext ctx, MultiSpreadPlacement placement, int rx, int rz) {
		return RegionMemo.verdict(this, ctx.chunkGenerator(), ctx.seed(), rx, rz,
				() -> computeFirstPass(ctx, placement, rx, rz));
	}

	private int computeFirstPass(GenerationContext ctx, MultiSpreadPlacement placement, int rx, int rz) {
		if (pickForRegion(ctx.seed(), rx, rz, placement.salt(), this.setCount) != this.setIndex) {
			return -1;
		}
		int spacing = placement.spacing();
		List<ChunkPos> cands = placement.getPotentialChunks(ctx.seed(), rx * spacing, rz * spacing);
		JigsawCheck check = check();
		for (int i = 0; i < cands.size(); i++) {
			ChunkPos c = cands.get(i);
			if (check.checkCandidate(JigsawCheck.ctxFor(ctx, c, biomes()), c, placement).isPresent()) {
				return i;
			}
		}
		return -1;
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
	 * Runs the terrain predicate's foundation pass once per chunk, after that chunk's pieces are placed.
	 */
	@Override
	public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
						   RandomSource random, BoundingBox box, ChunkPos chunkPos, PiecesContainer pieces) {
		this.terrain.afterPlace(level, box, pieces);
	}

	@Override
	public TerrainAdjustment terrainAdaptation() {
		return JigsawCheck.adaptation(this.terrain, super.terrainAdaptation());
	}

	/**
	 * Chunk spacing of the set that places this structure, or {@code 0} when no multi-spread set places it.
	 */
	@Override
	public int spacing() {
		return this.placement().map(MultiSpreadPlacement::spacing).orElse(0);
	}

	/**
	 * The single start this structure places in the given region, if any. Mirrors {@link #findGenerationPoint}:
	 * the least-index candidate that passes {@link JigsawCheck#checkCandidate} wins, so this agrees with worldgen and
	 * reads no chunks. Cached per region by {@link RegionMemo}.
	 */
	@Override
	public Optional<Located> locateRegion(long seed, int regionX, int regionZ, RegistryAccess registries,
										  ChunkGenerator generator, RandomState randomState,
										  StructureTemplateManager templates, LevelHeightAccessor heightAccessor) {
		Optional<MultiSpreadPlacement> resolved = this.placement();
		if (resolved.isEmpty()) {
			return Optional.empty();
		}
		MultiSpreadPlacement placement = resolved.get();
		int spacing = placement.spacing();
		List<ChunkPos> cands = placement.getPotentialChunks(seed, regionX * spacing, regionZ * spacing);
		if (cands.isEmpty()) {
			return Optional.empty();
		}
		GenerationContext ctx = JigsawCheck.context(registries, generator, randomState, templates, seed,
				cands.getFirst(), heightAccessor, biomes());
		int idx = this.firstPassInRegion(ctx, placement, regionX, regionZ);
		if (idx < 0) {
			return Optional.empty();
		}
		ChunkPos chunk = cands.get(idx);
		return Optional.of(new Located(chunk, placement.getLocatePos(chunk), regionX, regionZ));
	}

}
