package dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

import java.util.List;

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

	MapCodec<? extends TerrainPredicate> codec();

	FitResult fit(TerrainFit fit);

	/**
	 * Diagnostic lines for {@link #fit}: call it first, then this reads the same tree for the numbers behind the
	 * verdict.
	 */
	default List<String> describe(TerrainFit fit) {
		return List.of();
	}

	/**
	 * Runs per chunk in {@code Structure#afterPlace}, after the jigsaw pieces of that chunk have been placed.
	 */
	default void afterPlace(WorldGenLevel level, BoundingBox chunk, PiecesContainer pieces) {
	}

	// Resolved lazily: a public field of an implementor is not readable while this interface initializes.
	private static MapCodec<? extends TerrainPredicate> codecFor(ResourceLocation id) {
		if (id.equals(UniformHeightCheck.ID)) return UniformHeightCheck.CODEC;
		if (id.equals(BastionHeightCheck.ID)) return BastionHeightCheck.CODEC;
		throw new IllegalArgumentException("Unknown terrain predicate: " + id);
	}

}
