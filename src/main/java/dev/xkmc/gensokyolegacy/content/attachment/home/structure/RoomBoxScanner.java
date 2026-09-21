package dev.xkmc.gensokyolegacy.content.attachment.home.structure;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Pure template-space room analysis shared by datagen
 * ({@code RoomBoxDatagen}) and the dev verifier
 * ({@code StructureSpaceVerifier}).
 * Every non-solid cell is classified from its column roof
 * (highest occluding block): below-roof air is roofed, the rest is open.
 * Roofed air fully walled in from open air is in-room; roofed air reaching
 * open air (or the template side/top boundary) is under-roof.
 * In-room air is covered by the least possible number of boxes under one
 * hard rule: a box may contain only in-room air and wall/solid blocks,
 * never outdoor or under-roof air. Boxes grow greedily (best gain first)
 * and may span walls and overlap, so rooms joined by solids usually share
 * one box while courtyards and eaves force a split.
 */
public class RoomBoxScanner {

	public static final byte OPEN = 0, UNDER_ROOF = 1, IN_ROOM = 2, SOLID = 3;

	public record RoomBox(BoundingBox box, int cells) {
	}

	public record RoomScan(List<RoomBox> rooms, byte[][][] cells,
						   int occupied, int inRoom, int underRoof, int outdoor,
						   int roofedColumns, int columns) {
	}

	public static RoomScan scan(int sx, int sy, int sz, boolean[][][] solid, boolean[][][] occlude) {
		int[][] roof = new int[sx][sz];
		int occupied = 0, roofed = 0;
		byte[][][] cells = new byte[sx][sy][sz];
		for (int x = 0; x < sx; x++) {
			for (int z = 0; z < sz; z++) {
				int top = -1;
				for (int y = 0; y < sy; y++)
					if (occlude[x][y][z]) top = y;
				roof[x][z] = top;
				if (top >= 0) roofed++;
			}
		}
		for (int x = 0; x < sx; x++)
			for (int y = 0; y < sy; y++)
				for (int z = 0; z < sz; z++) {
					if (solid[x][y][z]) {
						occupied++;
						cells[x][y][z] = SOLID;
					} else if (y < roof[x][z]) {
						cells[x][y][z] = UNDER_ROOF;
					}
				}
		boolean[][][] room = floodRooms(sx, sy, sz, solid, roof);
		List<RoomBox> rooms = new ArrayList<>();
		int inRoom = 0, underRoof = 0, outdoor = 0;
		for (int x = 0; x < sx; x++)
			for (int y = 0; y < sy; y++)
				for (int z = 0; z < sz; z++) {
					if (cells[x][y][z] == SOLID) continue;
					if (roof[x][z] < 0 || y >= roof[x][z]) {
						outdoor++;
					} else if (room[x][y][z]) {
						cells[x][y][z] = IN_ROOM;
						inRoom++;
					} else {
						underRoof++;
					}
				}
		for (var box : optimizeBoxes(solid, room, collectBoxes(sx, sy, sz, solid, room)))
			rooms.add(box);
		return new RoomScan(List.copyOf(rooms), cells, occupied, inRoom, underRoof, outdoor,
				roofed, sx * sz);
	}

	private static final int[][] DIRS = {
			{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	private static boolean[][][] floodRooms(int sx, int sy, int sz, boolean[][][] solid, int[][] roof) {
		boolean[][][] seen = new boolean[sx][sy][sz];
		boolean[][][] room = new boolean[sx][sy][sz];
		for (int x = 0; x < sx; x++)
			for (int z = 0; z < sz; z++)
				for (int y = 0; y < Math.min(sy, roof[x][z]); y++) {
					if (solid[x][y][z] || seen[x][y][z]) continue;
					List<int[]> component = new ArrayList<>();
					boolean open = false;
					Queue<int[]> queue = new ArrayDeque<>();
					queue.add(new int[]{x, y, z});
					seen[x][y][z] = true;
					while (!queue.isEmpty()) {
						int[] cell = queue.poll();
						component.add(cell);
						for (var d : DIRS) {
							int nx = cell[0] + d[0], ny = cell[1] + d[1], nz = cell[2] + d[2];
							if (ny < 0) continue;
							if (nx < 0 || ny >= sy || nz < 0 || nz >= sz || nx >= sx) {
								open = true;
								continue;
							}
							if (solid[nx][ny][nz]) continue;
							if (ny >= roof[nx][nz]) {
								open = true;
								continue;
							}
							if (!seen[nx][ny][nz]) {
								seen[nx][ny][nz] = true;
								queue.add(new int[]{nx, ny, nz});
							}
						}
					}
					if (!open) {
						for (var cell : component)
							room[cell[0]][cell[1]][cell[2]] = true;
					}
				}
		return room;
	}

	private static List<RoomBox> collectBoxes(int sx, int sy, int sz, boolean[][][] solid, boolean[][][] room) {
		// forbidden inside a box: outdoor or under-roof air.
		// wall/solid cells are don't-care: they need no covering but may
		// sit inside any box, so boxes freely span walls.
		boolean[][][] blocked = new boolean[sx][sy][sz];
		boolean[][][] uncovered = new boolean[sx][sy][sz];
		for (int x = 0; x < sx; x++)
			for (int y = 0; y < sy; y++)
				for (int z = 0; z < sz; z++) {
					if (solid[x][y][z]) continue;
					if (room[x][y][z]) uncovered[x][y][z] = true;
					else blocked[x][y][z] = true;
				}
		// multi-start greedy: seed scan order x/y/z x ascending/descending,
		// expansion order forward/reversed; keep fewest boxes, then smallest
		// total volume. All deterministic, so datagen output is stable.
		List<RoomBox> best = null;
		long bestVol = Long.MAX_VALUE;
		int[][] orders = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};
		for (var order : orders)
			for (int sxn : new int[]{1, -1})
				for (int syn : new int[]{1, -1})
					for (int szn : new int[]{1, -1})
						for (boolean rev : new boolean[]{false, true}) {
							var boxes = greedyCover(sx, sy, sz, blocked, copyOf(sx, sy, sz, uncovered), order,
									new int[]{sxn, syn, szn}, rev);
							boxes = optimizeBoxes(solid, room, boxes);
							long vol = volumeOf(boxes);
							if (best == null || boxes.size() < best.size() ||
									boxes.size() == best.size() && vol < bestVol) {
								best = boxes;
								bestVol = vol;
							}
						}
		if (best == null) return List.of();
		List<RoomBox> ans = new ArrayList<>();
		for (var b : best) {
			var box = b.box();
			int cells = 0;
			for (int x = box.minX(); x <= box.maxX(); x++)
				for (int y = box.minY(); y <= box.maxY(); y++)
					for (int z = box.minZ(); z <= box.maxZ(); z++)
						if (room[x][y][z]) cells++;
			ans.add(new RoomBox(box, cells));
		}
		return ans;
	}

	private static boolean[][][] copyOf(int sx, int sy, int sz, boolean[][][] src) {
		boolean[][][] ans = new boolean[sx][sy][sz];
		for (int x = 0; x < sx; x++)
			for (int y = 0; y < sy; y++)
				System.arraycopy(src[x][y], 0, ans[x][y], 0, sz);
		return ans;
	}

	private static long volumeOf(List<RoomBox> boxes) {
		long ans = 0;
		for (var b : boxes) {
			var box = b.box();
			ans += (long) (box.maxX() - box.minX() + 1) *
					(box.maxY() - box.minY() + 1) *
					(box.maxZ() - box.minZ() + 1);
		}
		return ans;
	}

	private static List<RoomBox> greedyCover(int sx, int sy, int sz,
											boolean[][][] blocked, boolean[][][] uncovered,
											int[] order, int[] sign, boolean rev) {
		List<RoomBox> ans = new ArrayList<>();
		int[][] dirs = rev ? reversed(DIRS) : DIRS;
		while (true) {
			int[] seed = findSeed(sx, sy, sz, uncovered, order, sign);
			if (seed == null) break;
			int x0 = seed[0], y0 = seed[1], z0 = seed[2];
			int x1 = seed[0], y1 = seed[1], z1 = seed[2];
			uncovered[x0][y0][z0] = false;
			while (true) {
				int[] best = null;
				int bestGain = 0;
				for (var d : dirs) {
					int nx0 = x0 + Math.min(0, d[0]), ny0 = y0 + Math.min(0, d[1]), nz0 = z0 + Math.min(0, d[2]);
					int nx1 = x1 + Math.max(0, d[0]), ny1 = y1 + Math.max(0, d[1]), nz1 = z1 + Math.max(0, d[2]);
					if (nx0 < 0 || ny0 < 0 || nz0 < 0 || nx1 >= sx || ny1 >= sy || nz1 >= sz)
						continue;
					int gain = 0;
					boolean ok = true;
					for (int x = nx0; x <= nx1 && ok; x++)
						for (int y = ny0; y <= ny1 && ok; y++)
							for (int z = nz0; z <= nz1 && ok; z++) {
								if (blocked[x][y][z]) ok = false;
								else if (uncovered[x][y][z]) gain++;
							}
					if (ok && gain > bestGain) {
						bestGain = gain;
						best = new int[]{nx0, ny0, nz0, nx1, ny1, nz1};
					}
				}
				if (best == null) break;
				x0 = best[0];
				y0 = best[1];
				z0 = best[2];
				x1 = best[3];
				y1 = best[4];
				z1 = best[5];
				for (int x = x0; x <= x1; x++)
					for (int y = y0; y <= y1; y++)
						for (int z = z0; z <= z1; z++)
							uncovered[x][y][z] = false;
			}
			ans.add(new RoomBox(new BoundingBox(x0, y0, z0, x1, y1, z1), -1));
		}
		return ans;
	}

	private static int[][] reversed(int[][] dirs) {
		int[][] ans = new int[dirs.length][];
		for (int i = 0; i < dirs.length; i++)
			ans[i] = dirs[dirs.length - 1 - i];
		return ans;
	}

	private static int[] findSeed(int sx, int sy, int sz, boolean[][][] uncovered, int[] order, int[] sign) {
		int[] size = {sx, sy, sz};
		for (int i0 = 0; i0 < size[order[0]]; i0++)
			for (int i1 = 0; i1 < size[order[1]]; i1++)
				for (int i2 = 0; i2 < size[order[2]]; i2++) {
					int[] c = new int[3];
					c[order[0]] = sign[order[0]] > 0 ? i0 : size[order[0]] - 1 - i0;
					c[order[1]] = sign[order[1]] > 0 ? i1 : size[order[1]] - 1 - i1;
					c[order[2]] = sign[order[2]] > 0 ? i2 : size[order[2]] - 1 - i2;
					if (uncovered[c[0]][c[1]][c[2]]) return c;
				}
		return null;
	}

	/**
	 * Shrinks the greedy cover to fixpoint: drop boxes whose in-room cells
	 * are all covered by other boxes, and merge any pair whose bounding
	 * union holds no outdoor/under-roof air. Both steps keep every box
	 * within in-room air + solids only.
	 */
	private static List<RoomBox> optimizeBoxes(boolean[][][] solid, boolean[][][] room, List<RoomBox> boxes) {
		boolean changed = true;
		while (changed) {
			changed = false;
			for (int i = boxes.size() - 1; i >= 0; i--) {
				if (isRedundant(room, boxes, i)) {
					boxes.remove(i);
					changed = true;
				}
			}
			outer:
			for (int i = 0; i < boxes.size(); i++) {
				for (int j = i + 1; j < boxes.size(); j++) {
					var merged = tryMerge(solid, room, boxes.get(i), boxes.get(j));
					if (merged != null) {
						boxes.set(i, merged);
						boxes.remove(j);
						changed = true;
						break outer;
					}
				}
			}
		}
		return boxes;
	}

	private static boolean isRedundant(boolean[][][] room, List<RoomBox> boxes, int skip) {
		var target = boxes.get(skip).box();
		for (int x = target.minX(); x <= target.maxX(); x++)
			for (int y = target.minY(); y <= target.maxY(); y++)
				for (int z = target.minZ(); z <= target.maxZ(); z++) {
					if (!room[x][y][z]) continue;
					boolean covered = false;
					for (int i = 0; i < boxes.size() && !covered; i++) {
						if (i == skip) continue;
						if (boxes.get(i).box().isInside(x, y, z)) covered = true;
					}
					if (!covered) return false;
				}
		return true;
	}

	private static RoomBox tryMerge(boolean[][][] solid, boolean[][][] room, RoomBox a, RoomBox b) {
		var ab = a.box();
		var bb = b.box();
		int x0 = Math.min(ab.minX(), bb.minX()), y0 = Math.min(ab.minY(), bb.minY()), z0 = Math.min(ab.minZ(), bb.minZ());
		int x1 = Math.max(ab.maxX(), bb.maxX()), y1 = Math.max(ab.maxY(), bb.maxY()), z1 = Math.max(ab.maxZ(), bb.maxZ());
		int cells = 0;
		for (int x = x0; x <= x1; x++)
			for (int y = y0; y <= y1; y++)
				for (int z = z0; z <= z1; z++) {
					if (solid[x][y][z]) continue;
					if (!room[x][y][z]) return null;
					cells++;
				}
		return new RoomBox(new BoundingBox(x0, y0, z0, x1, y1, z1), cells);
	}

}
