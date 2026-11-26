package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain.FitResult;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain.TerrainFit;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain.TerrainPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Speculative jigsaw eligibility of one template: cheap prefilter, layout build, biome gate and the terrain fit.
 * {@link FlatCheckStructure}, {@link SecondaryStructure} and {@link FallbackStructure} differ only in parameters and
 * in how a candidate is selected, so all run this.
 *
 * @see "doc/design/structure/placement.md"
 * @see "doc/design/structure/terrain_fit.md"
 */
public record JigsawCheck(
		Holder<StructureTemplatePool> startPool,
		int maxDepth,
		boolean useExpansionHack,
		int maxDistanceFromCenter,
		TerrainPredicate terrain,
		HolderSet<Biome> biomes,
		int safetyRadius
) {

	public record Prefilter(boolean pass, int y) {

	}

	public record Validated(boolean pass, Structure.GenerationStub stub) {

		static Validated fail() {
			return new Validated(false, null);
		}

		static Validated pass(Structure.GenerationStub stub) {
			return new Validated(true, stub);
		}

	}

	/**
	 * Cheap middle-column sea/biome gate, run before the speculative layout.
	 */
	public Prefilter prefilter(Structure.GenerationContext ctx, ChunkPos chunkpos) {
		int y = TerrainFit.height(ctx, chunkpos.getMiddleBlockX(), chunkpos.getMiddleBlockZ());
		if (y <= ctx.chunkGenerator().getSeaLevel()) {
			return new Prefilter(false, y);
		}
		var biome = ctx.chunkGenerator().getBiomeSource().getNoiseBiome(
				QuartPos.fromBlock(chunkpos.getMiddleBlockX()), QuartPos.fromBlock(y),
				QuartPos.fromBlock(chunkpos.getMiddleBlockZ()), ctx.randomState().sampler());
		if (!biomes.contains(biome)) {
			return new Prefilter(false, y);
		}
		return new Prefilter(true, y);
	}

	/**
	 * Builds the jigsaw layout speculatively (provisional Y only fixes XZ), runs the biome gate, then lets
	 * {@link #terrain} place the layout on the terrain. On success returns a stub serving the shifted pieces; the
	 * predicate's foundation runs later in {@link TerrainPredicate#afterPlace}.
	 */
	public Validated validateLayout(Structure.GenerationContext ctx, ChunkPos chunkpos, int provY) {
		if (!this.terrain.triesAllRotations()) {
			return validateLayout(ctx, chunkpos, provY, -1);
		}
		Validated firstFail = null;
		for (Rotation rotation : Rotation.getShuffled(seededRandom(ctx.seed(), chunkpos))) {
			Validated v = validateLayout(ctx, chunkpos, provY, rotation.ordinal());
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
	 * As {@link #validateLayout(Structure.GenerationContext, ChunkPos, int)}, but {@code rotation >= 0} forces the
	 * start rotation instead of the one drawn from {@code ctx.random()}.
	 */
	private Validated validateLayout(Structure.GenerationContext ctx, ChunkPos chunkpos, int provY, int rotation) {
		if (rotation >= 0) {
			ctx = ctxForRotation(ctx, rotation);
		}
		int sea = ctx.chunkGenerator().getSeaLevel();
		BlockPos start = new BlockPos(chunkpos.getMinBlockX(), provY, chunkpos.getMinBlockZ());
		Optional<Structure.GenerationStub> layout = JigsawPlacement.addPieces(ctx, startPool, Optional.empty(), maxDepth, start,
				useExpansionHack, Optional.empty(), maxDistanceFromCenter);
		if (layout.isEmpty()) {
			return Validated.fail();
		}
		List<PoolElementStructurePiece> pieces = new ArrayList<>();
		for (StructurePiece p : layout.get().getPiecesBuilder().build().pieces()) {
			if (p instanceof PoolElementStructurePiece pool) {
				TerrainAdjustment adjustment = this.terrain.beardAdjustment(pool);
				pieces.add(adjustment == null ? pool
						: GLBeardedPiece.of(ctx.structureTemplateManager(), pool, adjustment));
			}
		}
		if (pieces.isEmpty()) {
			return Validated.fail();
		}
		PieceTree tree = PieceTree.build(pieces);
		for (BoundingBox box : this.terrain.biomeBoxes(tree)) {
			if (!inBiome(ctx, box)) {
				return Validated.fail();
			}
		}
		TerrainFit fit = new TerrainFit(ctx, tree, sea);
		FitResult result = this.terrain.fit(fit);
		if (!result.pass()) {
			return Validated.fail();
		}
		BlockPos pos = layout.get().position().offset(0, result.delta(), 0);
		return Validated.pass(new Structure.GenerationStub(pos, b -> pieces.forEach(b::addPiece)));
	}

	/**
	 * The 8-point biome gate of one box: 4 corners and 4 edge centres, sampled at the terrain height.
	 */
	private boolean inBiome(Structure.GenerationContext ctx, BoundingBox box) {
		int x0 = box.minX(), z0 = box.minZ(), x1 = box.maxX(), z1 = box.maxZ();
		int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
		int[][] pts = {{x0, z0}, {x0, z1}, {x1, z0}, {x1, z1},
				{cx, z0}, {cx, z1}, {x0, cz}, {x1, cz}};
		for (int[] q : pts) {
			int h = TerrainFit.height(ctx, q[0], q[1]);
			var biome = ctx.chunkGenerator().getBiomeSource().getNoiseBiome(
					QuartPos.fromBlock(q[0]), QuartPos.fromBlock(h), QuartPos.fromBlock(q[1]), ctx.randomState().sampler());
			if (!biomes.contains(biome)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Full eligibility of a candidate chunk, cheapest gates first: prefilter, margin safety, then the speculative
	 * layout.
	 */
	public Optional<Structure.GenerationStub> checkCandidate(Structure.GenerationContext ctx, ChunkPos chunkpos,
															 MultiSpreadPlacement placement) {
		Prefilter pre = prefilter(ctx, chunkpos);
		if (!pre.pass()) {
			return Optional.empty();
		}
		if (!marginSafe(ctx, chunkpos, placement)) {
			return Optional.empty();
		}
		Validated v = validateLayout(ctx, chunkpos, pre.y());
		return v.pass() ? Optional.of(v.stub()) : Optional.empty();
	}

	/**
	 * A candidate within {@code safetyRadius} chunks of its region border is rejected when a neighbouring region hosts
	 * a candidate inside the same radius that passes the prefilter and lies at most as close to its own region center
	 * as we do to ours. The closer of two colliding candidates survives, exact ties reject both, candidates deep inside
	 * their region are always safe, and no jigsaw layouts are built here.
	 */
	public boolean marginSafe(Structure.GenerationContext ctx, ChunkPos chunkpos, MultiSpreadPlacement placement) {
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
					if (prefilter(ctxFor(ctx, c, biomes), c).pass()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * The Beardifier drops structures whose adaptation is NONE before it reads any piece, so a sparse-beard rule
	 * advertises BEARD_THIN here while each piece still reports its own adjustment. The configured JSON value stays
	 * NONE: only the gate keeps a beard.
	 */
	public static TerrainAdjustment adaptation(TerrainPredicate terrain, TerrainAdjustment configured) {
		return configured == TerrainAdjustment.NONE && terrain.needsBeardifier()
				? TerrainAdjustment.BEARD_THIN : configured;
	}

	/**
	 * Context for another chunk, seeded as vanilla seeds the one it hands to {@code findGenerationPoint}.
	 */
	public static Structure.GenerationContext ctxFor(Structure.GenerationContext ctx, ChunkPos pos,
													 HolderSet<Biome> biomes) {
		return context(ctx.registryAccess(), ctx.chunkGenerator(), ctx.randomState(),
				ctx.structureTemplateManager(), ctx.seed(), pos, ctx.heightAccessor(), biomes);
	}

	public static Structure.GenerationContext context(RegistryAccess registries, ChunkGenerator generator,
													  RandomState randomState, StructureTemplateManager templates,
													  long seed, ChunkPos pos, LevelHeightAccessor heightAccessor,
													  HolderSet<Biome> biomes) {
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureSeed(seed, pos.x, pos.z);
		return new Structure.GenerationContext(registries, generator, generator.getBiomeSource(), randomState,
				templates, random, seed, pos, heightAccessor, biomes::contains);
	}

	/**
	 * The candidate's own seeded random, used for the rotation shuffle so the layout stream stays untouched.
	 */
	private static WorldgenRandom seededRandom(long seed, ChunkPos pos) {
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureSeed(seed, pos.x, pos.z);
		return random;
	}

	private static Structure.GenerationContext ctxForRotation(Structure.GenerationContext ctx, int rotation) {
		return new Structure.GenerationContext(ctx.registryAccess(), ctx.chunkGenerator(), ctx.biomeSource(),
				ctx.randomState(), ctx.structureTemplateManager(), new RotationRandom(rotation, ctx.seed(), ctx.chunkPos()),
				ctx.seed(), ctx.chunkPos(), ctx.heightAccessor(), ctx.validBiome());
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

}
