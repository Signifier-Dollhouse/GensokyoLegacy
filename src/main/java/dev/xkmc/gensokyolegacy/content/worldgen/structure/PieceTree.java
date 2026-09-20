package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Layout topology of one speculative jigsaw placement: parent links
 * recovered from junctions, rigid groups (pieces linked by rigid-only
 * paths, sharing the entry piece as reference) and connected non-rigid
 * subgraphs for joint checks. Pure layout, no world access.
 */
public class PieceTree {

	private final List<PoolElementStructurePiece> pieces;
	private final int[] parent;
	private final Map<Integer, Set<Integer>> children;
	private final int[] rigidId;
	private final Map<Integer, Integer> rigidRoot;
	private final int[] flexId;

	private PieceTree(List<PoolElementStructurePiece> pieces, int[] parent, Map<Integer, Set<Integer>> children,
					  int[] rigidId, Map<Integer, Integer> rigidRoot, int[] flexId) {
		this.pieces = pieces;
		this.parent = parent;
		this.children = children;
		this.rigidId = rigidId;
		this.rigidRoot = rigidRoot;
		this.flexId = flexId;
	}

	public static PieceTree build(List<PoolElementStructurePiece> pieces) {
		int n = pieces.size();
		int[] parent = inferParents(pieces);
		Map<Integer, Set<Integer>> children = new HashMap<>();
		for (int i = 0; i < n; i++) {
			if (parent[i] >= 0) {
				children.computeIfAbsent(parent[i], k -> new HashSet<>()).add(i);
			}
		}
		int[] rigidId = unionFind(n);
		for (int i = 0; i < n; i++) {
			if (parent[i] >= 0 && isRigid(pieces.get(i)) && isRigid(pieces.get(parent[i]))) {
				union(rigidId, i, parent[i]);
			}
		}
		Map<Integer, Integer> rigidRoot = new HashMap<>();
		for (int i = 0; i < n; i++) {
			if (!isRigid(pieces.get(i))) continue;
			rigidRoot.merge(find(rigidId, i), i, Math::min);
		}
		int[] flexId = unionFind(n);
		for (int i = 0; i < n; i++) {
			if (parent[i] >= 0 && !isRigid(pieces.get(i)) && !isRigid(pieces.get(parent[i]))) {
				union(flexId, i, parent[i]);
			}
		}
		return new PieceTree(pieces, parent, children, rigidId, rigidRoot, flexId);
	}

	public int size() {
		return pieces.size();
	}

	public List<PoolElementStructurePiece> pieces() {
		return pieces;
	}

	public PoolElementStructurePiece piece(int i) {
		return pieces.get(i);
	}

	public int parent(int i) {
		return parent[i];
	}

	public Set<Integer> children(int i) {
		return children.getOrDefault(i, Set.of());
	}

	public static boolean isRigid(PoolElementStructurePiece piece) {
		return piece.getElement().getProjection() == StructureTemplatePool.Projection.RIGID;
	}

	public boolean isRigid(int i) {
		return isRigid(pieces.get(i));
	}

	public static int groundOf(PoolElementStructurePiece piece) {
		return piece.getBoundingBox().minY() + piece.getGroundLevelDelta();
	}

	public int groundOf(int i) {
		return groundOf(pieces.get(i));
	}

	/**
	 * Index of the entry (first) rigid piece of the group containing i.
	 */
	public int rigidRootOf(int i) {
		return rigidRoot.get(find(rigidId, i));
	}

	/**
	 * Shifts the start piece's rigid group so its ground sits on spawn.
	 * No-op when the start piece is non-rigid. Returns the applied delta.
	 */
	public int shiftStartGroupToGround(int spawn) {
		if (!isRigid(0)) return 0;
		int delta = spawn - groundOf(0);
		int startGroup = find(rigidId, 0);
		for (int i = 0; i < pieces.size(); i++) {
			if (isRigid(i) && find(rigidId, i) == startGroup) {
				pieces.get(i).move(0, delta, 0);
			}
		}
		return delta;
	}

	/**
	 * Per connected non-rigid subgraph, the placed grounds of adjacent
	 * rigid groups (group roots, tolerating designed internal offsets).
	 */
	public Collection<Set<Integer>> flexJointGrounds() {
		Map<Integer, Set<Integer>> joints = new HashMap<>();
		for (int i = 0; i < pieces.size(); i++) {
			if (isRigid(i)) continue;
			Set<Integer> grounds = joints.computeIfAbsent(find(flexId, i), k -> new HashSet<>());
			if (parent[i] >= 0 && isRigid(parent[i])) {
				grounds.add(groundOf(rigidRootOf(parent[i])));
			}
			for (int c : children(i)) {
				if (isRigid(c)) {
					grounds.add(groundOf(rigidRootOf(c)));
				}
			}
		}
		return joints.values();
	}

	public BoundingBox unionBox() {
		BoundingBox first = pieces.get(0).getBoundingBox();
		int x0 = first.minX(), z0 = first.minZ(), x1 = first.maxX(), z1 = first.maxZ();
		for (PoolElementStructurePiece p : pieces) {
			BoundingBox b = p.getBoundingBox();
			x0 = Math.min(x0, b.minX());
			z0 = Math.min(z0, b.minZ());
			x1 = Math.max(x1, b.maxX());
			z1 = Math.max(z1, b.maxZ());
		}
		return new BoundingBox(x0, first.minY(), z0, x1, first.maxY(), z1);
	}

	/**
	 * Recovers jigsaw parent links from junctions. A child's parent-link
	 * junction source (the parent jigsaw cell) lies inside the parent box
	 * but outside the child's own box; the smallest earlier box containing
	 * such a source wins. Falls back to the start piece.
	 */
	private static int[] inferParents(List<PoolElementStructurePiece> pieces) {
		int n = pieces.size();
		int[] parent = new int[n];
		parent[0] = -1;
		for (int i = 1; i < n; i++) {
			BoundingBox self = pieces.get(i).getBoundingBox();
			int best = -1;
			long bestVol = Long.MAX_VALUE;
			for (var j : pieces.get(i).getJunctions()) {
				int px = j.getSourceX(), pz = j.getSourceZ();
				if (px >= self.minX() && px <= self.maxX() && pz >= self.minZ() && pz <= self.maxZ()) {
					continue;
				}
				for (int k = 0; k < i; k++) {
					BoundingBox b = pieces.get(k).getBoundingBox();
					if (px < b.minX() || px > b.maxX() || pz < b.minZ() || pz > b.maxZ()) continue;
					long vol = (long) (b.maxX() - b.minX() + 1) * (b.maxZ() - b.minZ() + 1);
					if (vol < bestVol) {
						bestVol = vol;
						best = k;
					}
				}
				if (best >= 0) break;
			}
			parent[i] = best >= 0 ? best : 0;
		}
		return parent;
	}

	private static int[] unionFind(int n) {
		int[] id = new int[n];
		for (int i = 0; i < n; i++) id[i] = i;
		return id;
	}

	private static int find(int[] id, int x) {
		while (id[x] != x) {
			id[x] = id[id[x]];
			x = id[x];
		}
		return x;
	}

	private static void union(int[] id, int a, int b) {
		id[find(id, a)] = find(id, b);
	}

}
