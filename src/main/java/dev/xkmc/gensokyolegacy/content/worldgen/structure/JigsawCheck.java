package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
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
 * Speculative jigsaw eligibility of one template: cheap prefilter, layout build and the biome, root-height, rigid and
 * joint checks. {@link FlatCheckStructure}, {@link SecondaryStructure} and {@link FallbackStructure} differ only in
 * parameters and in how a candidate is selected, so all run this.
 *
 * @see "doc/design/structure/placement.md"
 */
public record JigsawCheck(
		Holder<StructureTemplatePool> startPool,
		int maxDepth,
		boolean useExpansionHack,
		int maxDistanceFromCenter,
		int tolerance,
		HolderSet<Biome> biomes,
		int safetyRadius
) {

	public static int freeHeight(Structure.GenerationContext ctx, int x, int z) {
		return ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
				ctx.heightAccessor(), ctx.randomState());
	}

	public record Prefilter(boolean pass, int y, String reason) {

	}

	public record Validated(boolean pass, int spawnY, Structure.GenerationStub stub, String reason) {

		static Validated fail(String reason) {
			return new Validated(false, 0, null, reason);
		}

	}

	/**
	 * Cheap middle-column sea/biome gate, run before the speculative layout.
	 */
	public Prefilter prefilter(Structure.GenerationContext ctx, ChunkPos chunkpos) {
		int y = freeHeight(ctx, chunkpos.getMiddleBlockX(), chunkpos.getMiddleBlockZ());
		if (y <= ctx.chunkGenerator().getSeaLevel()) {
			return new Prefilter(false, y, "sea");
		}
		var biome = ctx.chunkGenerator().getBiomeSource().getNoiseBiome(
				QuartPos.fromBlock(chunkpos.getMiddleBlockX()), QuartPos.fromBlock(y),
				QuartPos.fromBlock(chunkpos.getMiddleBlockZ()), ctx.randomState().sampler());
		if (!biomes.contains(biome)) {
			return new Prefilter(false, y, "biome");
		}
		return new Prefilter(true, y, "pass");
	}

	/**
	 * Builds the jigsaw layout speculatively (provisional Y only fixes XZ; the root rigid group is shifted to the
	 * footprint average afterwards) and runs the biome, root-footprint, rigid and joint checks. On success returns a
	 * stub serving the already-shifted pieces.
	 */
	public Validated validateLayout(Structure.GenerationContext ctx, ChunkPos chunkpos, int provY) {
		int sea = ctx.chunkGenerator().getSeaLevel();
		BlockPos start = new BlockPos(chunkpos.getMinBlockX(), provY, chunkpos.getMinBlockZ());
		Optional<Structure.GenerationStub> layout = JigsawPlacement.addPieces(ctx, startPool, Optional.empty(), maxDepth, start,
				useExpansionHack, Optional.empty(), maxDistanceFromCenter,
				PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING);
		if (layout.isEmpty()) {
			return Validated.fail("no-layout");
		}
		List<PoolElementStructurePiece> pieces = new ArrayList<>();
		for (var p : layout.get().getPiecesBuilder().build().pieces()) {
			if (p instanceof PoolElementStructurePiece pool) {
				pieces.add(pool);
			}
		}
		if (pieces.isEmpty()) {
			return Validated.fail("no-layout");
		}
		PieceTree tree = PieceTree.build(pieces);
		// Total-bound biome check: 4 union-box corners + 4 edge centers.
		var union = tree.unionBox();
		int ux0 = union.minX(), uz0 = union.minZ(), ux1 = union.maxX(), uz1 = union.maxZ();
		int ucx = (ux0 + ux1) / 2, ucz = (uz0 + uz1) / 2;
		int[][] biomePts = {{ux0, uz0}, {ux0, uz1}, {ux1, uz0}, {ux1, uz1},
				{ucx, uz0}, {ucx, uz1}, {ux0, ucz}, {ux1, ucz}};
		for (int[] q : biomePts) {
			int h = freeHeight(ctx, q[0], q[1]);
			var biome = ctx.chunkGenerator().getBiomeSource().getNoiseBiome(
					QuartPos.fromBlock(q[0]), QuartPos.fromBlock(h), QuartPos.fromBlock(q[1]), ctx.randomState().sampler());
			if (!biomes.contains(biome)) {
				return Validated.fail("biome");
			}
		}
		// Root footprint check: corners + edge centers + center of start box.
		var rootBox = pieces.getFirst().getBoundingBox();
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
		if (max - min > 2 * tolerance) {
			return Validated.fail("height[%d-%d]".formatted(min, max));
		}
		int spawn = (max + min) / 2;
		// Shift the start rigid group (assumes a rigid start piece) so its ground sits on the footprint average.
		int delta = tree.shiftStartGroupToGround(spawn);
		// Rigid check: center terrain within tolerance of placed ground.
		for (int i = 0; i < tree.size(); i++) {
			if (!tree.isRigid(i)) continue;
			var b = tree.piece(i).getBoundingBox();
			int h = freeHeight(ctx, (b.minX() + b.maxX()) / 2, (b.minZ() + b.maxZ()) / 2);
			if (h <= sea) {
				return Validated.fail("rigid-sea-%d".formatted(i));
			}
			int g = tree.groundOf(i);
			if (Math.abs(h - g) > tolerance) {
				return Validated.fail("rigid-%d[%d-vs-%d]".formatted(i, h, g));
			}
		}
		// Joint check: per connected non-rigid subgraph, adjacent rigid group grounds must lie within tolerance.
		for (Set<Integer> grounds : tree.flexJointGrounds()) {
			if (grounds.size() < 2) continue;
			int lo = grounds.stream().mapToInt(Integer::intValue).min().orElse(0);
			int hi = grounds.stream().mapToInt(Integer::intValue).max().orElse(0);
			if (hi - lo > tolerance) {
				return Validated.fail("joint[%d-%d]".formatted(lo, hi));
			}
		}
		BlockPos pos = layout.get().position().offset(0, delta, 0);
		Structure.GenerationStub stub = new Structure.GenerationStub(pos, b -> pieces.forEach(b::addPiece));
		return new Validated(true, spawn, stub, "pass");
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

}
