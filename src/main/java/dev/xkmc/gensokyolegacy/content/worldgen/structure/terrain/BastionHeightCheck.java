package dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.PieceTree;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

import javax.annotation.Nullable;

/**
 * Bastion rule: the anchor piece (the gate) sits on the terrain and may not deviate more than the tolerance;
 * under every other rigid piece the terrain must stay below {@code ground + tolerance}: the piece may float
 * any distance above it, but may not be buried deeper than the tolerance. {@link #afterPlace} fills the gap.
 */
public record BastionHeightCheck(ResourceLocation anchor, int tolerance, BlockState fill) implements TerrainPredicate {

	public static final ResourceLocation ID = GensokyoLegacy.loc("bastion_height");

	public static final Codec<BastionHeightCheck> CODEC = RecordCodecBuilder.create(i -> i.group(
			ResourceLocation.CODEC.fieldOf("anchor").forGetter(BastionHeightCheck::anchor),
			Codec.intRange(1, 128).optionalFieldOf("height_tolerance", 4).forGetter(BastionHeightCheck::tolerance),
			BlockState.CODEC.optionalFieldOf("fill", Blocks.DIRT.defaultBlockState()).forGetter(BastionHeightCheck::fill)
	).apply(i, BastionHeightCheck::new));

	@Override
	public ResourceLocation type() {
		return ID;
	}

	@Override
	public Codec<BastionHeightCheck> codec() {
		return CODEC;
	}

	/**
	 * The one-sided rule depends on which way the footprint faces: only the gate is anchored, and terrain rising
	 * under any other wing fails the fit. The four rotations sample almost disjoint ground, so all four are tried.
	 */
	@Override
	public boolean triesAllRotations() {
		return true;
	}

	@Override
	public boolean needsBeardifier() {
		return true;
	}

	/**
	 * Only the gate blends into the terrain; every other piece opts out, so the Beardifier leaves the gap under
	 * the temple air-only for {@link #afterPlace} to fill.
	 */
	@Nullable
	@Override
	public TerrainAdjustment beardAdjustment(PoolElementStructurePiece piece) {
		return anchor.equals(PieceTree.templateId(piece.getElement()))
				? TerrainAdjustment.BEARD_THIN : TerrainAdjustment.NONE;
	}

	@Override
	public FitResult fit(TerrainFit fit) {
		PieceTree tree = fit.tree();
		int sea = fit.seaLevel();
		int anchor = tree.findPiece(this.anchor);
		if (anchor < 0) {
			return FitResult.fail();
		}
		if (!tree.isRigid(anchor) || !tree.isRigid(0) || tree.rigidRootOf(anchor) != tree.rigidRootOf(0)) {
			return FitResult.fail();
		}
		int[] gate = fit.ring(tree.piece(anchor).getBoundingBox());
		int min = TerrainFit.min(gate), max = TerrainFit.max(gate);
		for (int h : gate) {
			if (h <= sea) {
				return FitResult.fail();
			}
		}
		// spawn is the anchor ring midpoint, so |h - spawn| <= t is exactly max - min <= 2 * t.
		int spawn = (min + max) / 2;
		int delta = tree.shiftRigidGroupToGround(anchor, spawn);
		for (int h : gate) {
			if (Math.abs(h - spawn) > tolerance) {
				return FitResult.fail();
			}
		}
		for (int i = 0; i < tree.size(); i++) {
			if (!tree.isRigid(i) || i == anchor) continue;
			BoundingBox b = tree.piece(i).getBoundingBox();
			int h = fit.height((b.minX() + b.maxX()) / 2, (b.minZ() + b.maxZ()) / 2);
			if (h <= sea) {
				return FitResult.fail();
			}
			int g = tree.groundOf(i);
			// Floating is free (the fill closes the gap); only burial fails, with the same sampling slack.
			if (h >= g + tolerance) {
				return FitResult.fail();
			}
		}
		return FitResult.pass(delta);
	}

	/**
	 * Vanilla-mansion fill: from the anchor group's bottom layer down to the first non-air block, air only.
	 * The scan is bounded only by the build height, because a piece may float any distance.
	 */
	@Override
	public void afterPlace(WorldGenLevel level, BoundingBox chunk, PiecesContainer pieces) {
		int groundY = Integer.MIN_VALUE;
		for (StructurePiece p : pieces.pieces()) {
			if (p instanceof PoolElementStructurePiece pool
					&& anchor.equals(PieceTree.templateId(pool.getElement()))) {
				groundY = PieceTree.groundOf(pool);
				break;
			}
		}
		if (groundY == Integer.MIN_VALUE) {
			return;
		}
		int top = groundY - 1;
		int minBuild = level.getMinBuildHeight();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = chunk.minX(); x <= chunk.maxX(); x++) {
			for (int z = chunk.minZ(); z <= chunk.maxZ(); z++) {
				cursor.set(x, top, z);
				if (level.isEmptyBlock(cursor) || !pieces.isInsidePiece(cursor)) {
					continue;
				}
				for (int y = top - 1; y > minBuild; y--) {
					cursor.setY(y);
					if (!level.isEmptyBlock(cursor)) {
						break;
					}
					level.setBlock(cursor, fill, 2);
				}
			}
		}
	}

}
