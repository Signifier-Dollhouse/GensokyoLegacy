package dev.xkmc.gensokyolegacy.content.attachment.home.datamap;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Per-room decomposition of a structure interior for staged pathfinding; coords are
 * template-local. See {@code StructureHomeData} for the world-mapped counterpart; empty when
 * the structure has no interior data.
 *
 * @see "doc/design/structure/interior_rework.md"
 */
public record StructureInterior(
		ArrayList<InteriorRoom> rooms,
		ArrayList<InteriorNode> nodes
) {

	/**
	 * Outdoor transfer cost: a base penalty in indoor legs plus the walk distance at
	 * {@link #TRANSFER_WEIGHT} blocks per leg. The base keeps an indoor doorway cheaper than a short
	 * step outside when both exist.
	 */
	public static final double TRANSFER_BASE = 3.0;
	public static final double TRANSFER_WEIGHT = 16.0;

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
	 * Room route as waypoints, comparing indoor edges against outdoor transfers between entry nodes;
	 * empty when already there or unreachable.
	 */
	public List<BlockPos> route(int from, int to) {
		if (from == to) return List.of();
		int n = rooms.size();
		if (from < 0 || to < 0 || from >= n || to >= n) return List.of();
		double[] dist = new double[n];
		boolean[] done = new boolean[n];
		int[] prevRoom = new int[n];
		Step[] prevStep = new Step[n];
		Arrays.fill(dist, Double.MAX_VALUE);
		Arrays.fill(prevRoom, -1);
		dist[from] = 0;
		for (int iter = 0; iter < n; iter++) {
			int cur = -1;
			for (int i = 0; i < n; i++) {
				if (done[i] || dist[i] == Double.MAX_VALUE) continue;
				if (cur < 0 || dist[i] < dist[cur]) cur = i;
			}
			if (cur < 0) break;
			done[cur] = true;
			if (cur == to) break;
			for (var edge : nodes) {
				if (edge.isEntry()) continue;
				int next = edge.roomA() == cur ? edge.roomB() : edge.roomB() == cur ? edge.roomA() : -1;
				if (next < 0) continue;
				relax(dist, prevRoom, prevStep, cur, next, 1, new Step(edge, null, null));
			}
			for (var exit : nodes) {
				if (!exit.isEntry() || exit.roomA() != cur) continue;
				for (var entry : nodes) {
					if (!entry.isEntry() || entry.roomA() == cur) continue;
					double cost = TRANSFER_BASE +
							Math.sqrt(exit.posA().distSqr(entry.posA())) / TRANSFER_WEIGHT;
					relax(dist, prevRoom, prevStep, cur, entry.roomA(), cost,
							new Step(null, exit, entry));
				}
			}
		}
		if (dist[to] == Double.MAX_VALUE) return List.of();
		var steps = new ArrayList<Step>();
		for (int r = to; r != from; r = prevRoom[r]) steps.add(prevStep[r]);
		Collections.reverse(steps);
		var ans = new ArrayList<BlockPos>();
		int side = from;
		for (var step : steps) {
			if (step.edge() != null) {
				if (!appendNode(ans, step.edge(), side)) return List.of();
				side = step.edge().roomA() == side ? step.edge().roomB() : step.edge().roomA();
			} else {
				if (step.exit() == null || step.entry() == null || step.exit().roomA() != side)
					return List.of();
				ans.add(step.exit().posA());
				ans.add(step.entry().posA());
				side = step.entry().roomA();
			}
		}
		return side == to ? ans : List.of();
	}

	private static void relax(double[] dist, int[] prevRoom, Step[] prevStep,
							  int cur, int next, double cost, Step step) {
		double cand = dist[cur] + cost;
		if (cand >= dist[next]) return;
		dist[next] = cand;
		prevRoom[next] = cur;
		prevStep[next] = step;
	}

	private static boolean appendNode(List<BlockPos> waypoints, InteriorNode edge, int side) {
		if (side == edge.roomA()) {
			waypoints.add(edge.posA());
			if (edge.isDual()) waypoints.add(edge.posB());
			return true;
		}
		if (side == edge.roomB()) {
			if (edge.isDual()) waypoints.add(edge.posB());
			waypoints.add(edge.posA());
			return true;
		}
		return false;
	}

	private record Step(@Nullable InteriorNode edge, @Nullable InteriorNode exit, @Nullable InteriorNode entry) {
	}

}
