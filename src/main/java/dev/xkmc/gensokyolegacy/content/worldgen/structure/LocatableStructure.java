package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * A structure the dev locate tools can search without reading chunks, because it owns a region pick: a primary or a
 * secondary. The tiny tier takes no region pick, so it has no region to report and is not locatable.
 *
 * @see "doc/design/structure/placement.md"
 */
public interface LocatableStructure {

	/**
	 * Chunk spacing of the set that places this structure, or {@code 0} when no multi-spread set places it.
	 */
	int spacing();

	/**
	 * The single start this structure places in the given region, if any, from the region cache. Reads no chunks.
	 */
	Optional<Located> locateRegion(long seed, int regionX, int regionZ, RegistryAccess registries,
								   ChunkGenerator generator, RandomState randomState,
								   StructureTemplateManager templates, LevelHeightAccessor heightAccessor);

	/**
	 * A start returned by {@link #locateRegion}: its chunk, the position {@code /locate} would report, and its region.
	 */
	record Located(ChunkPos chunk, BlockPos pos, int regionX, int regionZ) {
	}

}
