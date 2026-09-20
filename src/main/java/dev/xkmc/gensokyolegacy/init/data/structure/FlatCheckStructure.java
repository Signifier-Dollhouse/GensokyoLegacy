package dev.xkmc.gensokyolegacy.init.data.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.List;
import java.util.Optional;

/**
 * Port of YoukaiHomecoming 1.20.1 FlatStructure, paired with
 * {@link MultiSpreadPlacement}.
 * Samples heights on a 3x3 grid (center + 4 ends + 4 corners) spaced by
 * flatCheckRange around the chunk middle, and rejects the placement when
 * max - min exceeds heightTolerance, when any sample is at/below sea level,
 * or when any sample biome is outside the structure biome tag.
 * This guarantees vertical variation on the 4 ends and center stays below
 * heightTolerance (configured to 7, i.e. less than 8 blocks).
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
			if (this.checkCandidate(ctx, cands.get(j)).isPresent()) {
				return Optional.empty();
			}
		}
		return this.checkCandidate(ctx, me).flatMap((y) -> this.place(ctx, me, y));
	}

	private Optional<GenerationStub> place(GenerationContext ctx, ChunkPos chunkpos, int y) {
		BlockPos blockpos = new BlockPos(chunkpos.getMinBlockX(), y, chunkpos.getMinBlockZ());
		return JigsawPlacement.addPieces(ctx, this.startPool, Optional.empty(), this.maxDepth, blockpos,
				this.useExpansionHack, Optional.empty(), this.maxDistanceFromCenter,
				PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING);
	}

	/**
	 * Full eligibility of a candidate chunk: flat check plus margin safety.
	 * Returns the average ground height when eligible.
	 */
	private Optional<Integer> checkCandidate(GenerationContext ctx, ChunkPos chunkpos) {
		Optional<Integer> y = this.checkFlat(ctx, chunkpos);
		if (y.isEmpty()) {
			return Optional.empty();
		}
		if (!this.checkMarginSafe(ctx, chunkpos, y.get())) {
			return Optional.empty();
		}
		return y;
	}

	/**
	 * Flat check at a candidate chunk. Returns the average ground height when
	 * the 3x3 sample grid passes height, sea level and biome checks.
	 */
	private Optional<Integer> checkFlat(GenerationContext ctx, ChunkPos chunkpos) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (int ix = -1; ix <= 1; ix++) {
			for (int iz = -1; iz <= 1; iz++) {
				int x = chunkpos.getMiddleBlockX() + ix * flatCheckRange;
				int z = chunkpos.getMiddleBlockZ() + iz * flatCheckRange;
				int y = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
				if (y < min) min = y;
				if (y > max) max = y;
				if (y <= ctx.chunkGenerator().getSeaLevel()) {
					return Optional.empty();
				}
				var biome = ctx.chunkGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), ctx.randomState().sampler());
				if (!biomes().contains(biome)) {
					return Optional.empty();
				}
			}
		}
		if (min + flatTolerance < max) return Optional.empty();
		return Optional.of((max + min) / 2);
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

}
