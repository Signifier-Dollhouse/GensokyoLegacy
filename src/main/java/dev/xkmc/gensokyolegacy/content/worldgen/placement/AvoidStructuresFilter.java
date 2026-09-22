package dev.xkmc.gensokyolegacy.content.worldgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Keeps wide vegetation away from buildings: rejects positions within {@code margin} blocks (XZ)
 * of the bounding box of any listed structure. Buildings generate in an earlier step, and a
 * template canopy would otherwise fill every air block of a house it overlaps.
 * <p>
 * During the features step only the decorated chunk and its direct neighbours carry complete
 * structure references, so the margin is capped at one chunk and the filter must run while the
 * positions still lie inside the decorated chunk (before any random offset).
 */
public class AvoidStructuresFilter extends PlacementFilter {

	public static final MapCodec<AvoidStructuresFilter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegistryCodecs.homogeneousList(Registries.STRUCTURE).fieldOf("structures").forGetter(e -> e.structures),
			Codec.intRange(0, 16).fieldOf("margin").forGetter(e -> e.margin)
	).apply(i, AvoidStructuresFilter::new));

	private final HolderSet<Structure> structures;
	private final int margin;

	public AvoidStructuresFilter(HolderSet<Structure> structures, int margin) {
		this.structures = structures;
		this.margin = margin;
	}

	@Override
	protected boolean shouldPlace(PlacementContext ctx, RandomSource random, BlockPos pos) {
		WorldGenLevel level = ctx.getLevel();
		// every chunk the margin square touches, capped to the 3x3 around the decorated chunk
		int minX = SectionPos.blockToSectionCoord(pos.getX() - margin), maxX = SectionPos.blockToSectionCoord(pos.getX() + margin);
		int minZ = SectionPos.blockToSectionCoord(pos.getZ() - margin), maxZ = SectionPos.blockToSectionCoord(pos.getZ() + margin);
		if (level instanceof WorldGenRegion region) {
			ChunkPos center = region.getCenter();
			minX = Math.max(minX, center.x - 1);
			maxX = Math.min(maxX, center.x + 1);
			minZ = Math.max(minZ, center.z - 1);
			maxZ = Math.min(maxZ, center.z + 1);
		}
		for (int cx = minX; cx <= maxX; cx++) {
			for (int cz = minZ; cz <= maxZ; cz++) {
				if (hasStructureNear(level, cx, cz, pos)) return false;
			}
		}
		return true;
	}

	/**
	 * A chunk references every start whose bounding box intersects it, so the starts referenced
	 * by the chunks around the position are all that can reach it.
	 */
	private boolean hasStructureNear(WorldGenLevel level, int cx, int cz, BlockPos pos) {
		var references = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
		for (var entry : references.entrySet()) {
			if (!matches(entry.getKey())) continue;
			for (long packed : entry.getValue()) {
				int sx = ChunkPos.getX(packed), sz = ChunkPos.getZ(packed);
				// a start outside the generation region can not be read (reading would crash the
				// generation); its box would have to span 7 chunks to matter here
				if (!level.hasChunk(sx, sz)) continue;
				StructureStart start = level.getChunk(sx, sz, ChunkStatus.STRUCTURE_STARTS).getStartForStructure(entry.getKey());
				if (start != null && start.isValid() && isNear(start.getBoundingBox(), pos)) return true;
			}
		}
		return false;
	}

	private boolean isNear(BoundingBox box, BlockPos pos) {
		return pos.getX() >= box.minX() - margin && pos.getX() <= box.maxX() + margin
				&& pos.getZ() >= box.minZ() - margin && pos.getZ() <= box.maxZ() + margin;
	}

	private boolean matches(Structure structure) {
		for (var holder : structures) {
			if (holder.value() == structure) return true;
		}
		return false;
	}

	@Override
	public PlacementModifierType<?> type() {
		return GLWorldGen.AVOID_STRUCTURES.get();
	}

}
