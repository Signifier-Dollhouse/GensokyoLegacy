package dev.xkmc.gensokyolegacy.content.attachment.home.core;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-box room bound for preset structures.
 * Wraps the world-mapped room boxes with union-box helpers,
 * so wander, search and verify sample inside real room boxes
 * instead of the over-approximated union.
 */
public class MultiStructureBound {

	private final List<BoundingBox> boxes;
	private final BoundingBox union;
	private final List<StructureBound> parts;
	private final int[] prefix;
	private final int total;

	public MultiStructureBound(List<BoundingBox> boxes) {
		this.boxes = List.copyOf(boxes);
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE;
		int x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
		this.parts = new ArrayList<>(this.boxes.size());
		this.prefix = new int[this.boxes.size() + 1];
		for (int i = 0; i < this.boxes.size(); i++) {
			var b = this.boxes.get(i);
			x0 = Math.min(x0, b.minX());
			y0 = Math.min(y0, b.minY());
			z0 = Math.min(z0, b.minZ());
			x1 = Math.max(x1, b.maxX());
			y1 = Math.max(y1, b.maxY());
			z1 = Math.max(z1, b.maxZ());
			var part = new StructureBound(b);
			parts.add(part);
			prefix[i + 1] = prefix[i] + part.getSize();
		}
		this.total = prefix[this.boxes.size()];
		this.union = this.boxes.isEmpty() ? null :
				new BoundingBox(x0, y0, z0, x1, y1, z1);
	}

	public static MultiStructureBound of(BoundingBox single) {
		return new MultiStructureBound(List.of(single));
	}

	public static MultiStructureBound of(List<BoundingBox> boxes) {
		return new MultiStructureBound(boxes);
	}

	public List<BoundingBox> boxes() {
		return boxes;
	}

	public BoundingBox union() {
		return union;
	}

	public boolean isEmpty() {
		return boxes.isEmpty();
	}

	public boolean isInside(BlockPos pos) {
		for (var b : boxes) {
			if (b.isInside(pos)) return true;
		}
		return false;
	}

	public int getTotalSize() {
		return total;
	}

	public void resolve(BlockPos.MutableBlockPos pos, int step) {
		if (step < 0 || step >= total) {
			throw new IllegalArgumentException("invalid step: " + step + " out of " + total);
		}
		int i = 0;
		while (i + 1 < prefix.length && prefix[i + 1] <= step) i++;
		parts.get(i).resolve(pos, step - prefix[i]);
	}

	public void randomPos(RandomSource rand, BlockPos.MutableBlockPos pos) {
		int step = rand.nextInt(total);
		resolve(pos, step);
	}

	/**
	 * Intersect every room box with the given box, for area-limited search.
	 */
	public MultiStructureBound intersect(BoundingBox other) {
		List<BoundingBox> ans = new ArrayList<>();
		for (var b : boxes) {
			if (!b.intersects(other)) continue;
			int x0 = Math.max(b.minX(), other.minX());
			int y0 = Math.max(b.minY(), other.minY());
			int z0 = Math.max(b.minZ(), other.minZ());
			int x1 = Math.min(b.maxX(), other.maxX());
			int y1 = Math.min(b.maxY(), other.maxY());
			int z1 = Math.min(b.maxZ(), other.maxZ());
			if (x0 <= x1 && y0 <= y1 && z0 <= z1)
				ans.add(new BoundingBox(x0, y0, z0, x1, y1, z1));
		}
		return new MultiStructureBound(ans);
	}

}
