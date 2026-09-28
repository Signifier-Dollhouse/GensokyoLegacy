package dev.xkmc.gensokyolegacy.content.attachment.datamap;

import net.minecraft.core.BlockPos;

/**
 * A staged-pathfinding waypoint inside a structure, in template-local coords.
 * Rooms are referenced by index into {@link StructureInterior#rooms()}.
 * Room boxes are disjoint; a shared doorway sits at their boundary.
 * <ul>
 * <li>{@code roomB == -1}: house entry/exit, position in {@code roomA}.</li>
 * <li>{@code posA} equals {@code posB}: shared doorway at the boundary of
 * both rooms; the position sits in {@code roomA}.</li>
 * <li>{@code posA} differs from {@code posB}: dual linkage, {@code posA} in
 * {@code roomA} leads to {@code posB} in {@code roomB} (e.g. stair base/head
 * staging cells clear of the stair run itself).</li>
 * </ul>
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
