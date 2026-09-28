package dev.xkmc.gensokyolegacy.content.attachment.datamap;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

	/**
	 * Room route from one room to another as ordered node edges, via BFS
	 * over shared and dual linkages (entries excluded). Empty when already
	 * there or unreachable.
	 */
	public List<InteriorNode> findRoute(int from, int to) {
		if (from == to) return List.of();
		if (from < 0 || to < 0 || from >= rooms.size() || to >= rooms.size())
			return List.of();
		int n = rooms.size();
		boolean[] seen = new boolean[n];
		int[] prevRoom = new int[n];
		InteriorNode[] prevEdge = new InteriorNode[n];
		Arrays.fill(prevRoom, -1);
		seen[from] = true;
		var queue = new ArrayDeque<Integer>();
		queue.add(from);
		while (!queue.isEmpty()) {
			int cur = queue.removeFirst();
			for (var edge : nodes) {
				if (edge.isEntry()) continue;
				int next = -1;
				if (edge.roomA() == cur) next = edge.roomB();
				else if (edge.roomB() == cur) next = edge.roomA();
				if (next < 0 || next >= n || seen[next]) continue;
				seen[next] = true;
				prevRoom[next] = cur;
				prevEdge[next] = edge;
				if (next == to) {
					var ans = new ArrayList<InteriorNode>();
					for (int r = to; r != from; r = prevRoom[r]) ans.add(prevEdge[r]);
					Collections.reverse(ans);
					return ans;
				}
				queue.add(next);
			}
		}
		return List.of();
	}

}
