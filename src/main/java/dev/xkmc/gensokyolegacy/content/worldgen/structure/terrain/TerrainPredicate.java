package dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

import javax.annotation.Nullable;

/**
 * Terrain fitness rule of a flat-check structure: shifts the speculative layout onto the terrain and, for rules
 * that need it, fills the gap under the placed structure.
 *
 * @see "doc/design/structure/terrain_fit.md"
 */
public interface TerrainPredicate {

	Codec<TerrainPredicate> CODEC = ResourceLocation.CODEC.dispatch("type",
			TerrainPredicate::type, TerrainPredicate::codecFor);

	ResourceLocation type();

	Codec<? extends TerrainPredicate> codec();

	FitResult fit(TerrainFit fit);

	/**
	 * Whether placement should try all four start rotations instead of only the one drawn from the seed.
	 */
	default boolean triesAllRotations() {
		return false;
	}

	/**
	 * Runs per chunk in {@code Structure#afterPlace}, after the jigsaw pieces of that chunk have been placed.
	 */
	default void afterPlace(WorldGenLevel level, BoundingBox chunk, PiecesContainer pieces) {
	}

	/**
	 * Beard adjustment of one jigsaw piece, or {@code null} to keep the vanilla jigsaw piece. The piece becomes a
	 * {@code GLBeardedPiece} so the choice survives a structure-start reload.
	 */
	@Nullable
	default TerrainAdjustment beardAdjustment(PoolElementStructurePiece piece) {
		return null;
	}

	/**
	 * Whether the Beardifier must still visit this structure when its {@code terrain_adaptation} is NONE. The
	 * structure then advertises BEARD_THIN to pass the Beardifier's structure-level filter, while every piece
	 * still reports its own {@link #beardAdjustment}.
	 */
	default boolean needsBeardifier() {
		return false;
	}

	// Resolved lazily: a public field of an implementor is not readable while this interface initializes.
	private static Codec<? extends TerrainPredicate> codecFor(ResourceLocation id) {
		if (id.equals(UniformHeightCheck.ID)) return UniformHeightCheck.CODEC;
		if (id.equals(BastionHeightCheck.ID)) return BastionHeightCheck.CODEC;
		throw new IllegalArgumentException("Unknown terrain predicate: " + id);
	}

}
