package dev.xkmc.gensokyolegacy.content.attachment.home.datamap;

import net.minecraft.core.BlockPos;

/**
 * A staged-pathfinding waypoint in template-local coords; rooms are indices into
 * {@link StructureInterior#rooms()}. Room boxes are disjoint; a shared doorway sits at their
 * boundary. {@code roomB == -1} is the house entry/exit in {@code roomA}; {@code posA == posB} is
 * a shared doorway, also in {@code roomA}; otherwise a dual linkage: {@code posA} in {@code roomA}
 * leads to {@code posB} in {@code roomB} — stair base/head staging cells.
 */
public record InteriorNode(
		BlockPos posA,
		BlockPos posB,
		int roomA,
		int roomB
) {

	public boolean isEntry() {
		return roomB < 0;
	}

	public boolean isDual() {
		return !posA.equals(posB);
	}

	public boolean isShared() {
		return !isEntry() && !isDual();
	}

}
