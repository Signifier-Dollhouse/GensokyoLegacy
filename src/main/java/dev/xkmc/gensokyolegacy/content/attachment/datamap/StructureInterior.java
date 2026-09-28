package dev.xkmc.gensokyolegacy.content.attachment.datamap;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;

/**
 * Per-room decomposition of a structure interior for staged pathfinding.
 * All coords are template-local; see {@code StructureHomeData} for the
 * world-mapped counterpart. Empty when the structure has no interior data.
 */
public record StructureInterior(
		ArrayList<InteriorRoom> rooms,
		ArrayList<InteriorNode> nodes
) {

	public static StructureInterior empty() {
		return new StructureInterior(new ArrayList<>(), new ArrayList<>());
	}

	public boolean isEmpty() {
		return rooms.isEmpty();
	}

	/**
	 * Index of the first room containing the position, or -1.
	 */
	public int roomIndexOf(BlockPos pos) {
		for (int i = 0; i < rooms.size(); i++) {
			if (rooms.get(i).bound().isInside(pos)) return i;
		}
		return -1;
	}

}
