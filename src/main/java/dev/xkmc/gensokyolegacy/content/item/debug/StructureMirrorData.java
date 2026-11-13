package dev.xkmc.gensokyolegacy.content.item.debug;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Piece tree recorded on the structure mirror stick. Entries are parent-before-child; the root has
 * {@code parent = -1} and no child jigsaw.
 *
 * @see "doc/design/structure/mirror_stick.md"
 */
public record StructureMirrorData(List<Entry> entries) {

	public static final Codec<StructureMirrorData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Entry.CODEC.listOf().fieldOf("entries").forGetter(StructureMirrorData::entries)
	).apply(i, StructureMirrorData::new));

	public StructureMirrorData {
		entries = List.copyOf(entries);
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public int linkCount() {
		int count = 0;
		for (var entry : entries) if (entry.parent() >= 0) count++;
		return count;
	}

	/**
	 * Index of the smallest recorded bound containing {@code pos}, or -1.
	 */
	public int pieceAt(BlockPos pos) {
		int best = -1;
		long bestVolume = Long.MAX_VALUE;
		for (int i = 0; i < entries.size(); i++) {
			var entry = entries.get(i);
			if (!entry.contains(pos)) continue;
			long volume = entry.volume();
			if (volume < bestVolume) {
				bestVolume = volume;
				best = i;
			}
		}
		return best;
	}

	/**
	 * Index of the recorded piece owning this structure block, or -1.
	 */
	public int recorded(BlockPos structureBlock) {
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).block().equals(structureBlock)) return i;
		}
		return -1;
	}

	public boolean isLeaf(int index) {
		return childCount(index) == 0;
	}

	public int childCount(int index) {
		int count = 0;
		for (var entry : entries) {
			if (entry.parent() == index) count++;
		}
		return count;
	}

	public List<Integer> children(int index) {
		List<Integer> ans = new ArrayList<>();
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).parent() == index) ans.add(i);
		}
		return ans;
	}

	/**
	 * Direction from this entry's child jigsaw toward the parent jigsaw, or null when it cannot be
	 * derived uniquely.
	 */
	@Nullable
	public Direction parentDirection(int index) {
		if (index < 0 || index >= entries.size()) return null;
		var entry = entries.get(index);
		if (entry.parent() < 0 || entry.parent() >= entries.size() || entry.childJigsaw() == null) return null;
		var parent = entries.get(entry.parent());
		Direction found = null;
		for (Direction dir : Direction.values()) {
			if (!parent.contains(entry.childJigsaw().relative(dir))) continue;
			if (found != null) return null;
			found = dir;
		}
		return found;
	}

	@Nullable
	public BlockPos parentJigsaw(int index) {
		if (index < 0 || index >= entries.size()) return null;
		var entry = entries.get(index);
		Direction dir = parentDirection(index);
		if (entry.childJigsaw() == null || dir == null) return null;
		return entry.childJigsaw().relative(dir);
	}

	/**
	 * Child entry whose pair has a jigsaw at {@code pos}, or -1. Returns -2 when more than one pair matches.
	 */
	public int pairAt(BlockPos pos) {
		int found = -1;
		for (int i = 0; i < entries.size(); i++) {
			var entry = entries.get(i);
			if (entry.parent() < 0 || entry.childJigsaw() == null) continue;
			boolean match = entry.childJigsaw().equals(pos);
			if (!match) match = pos.equals(parentJigsaw(i));
			if (!match) continue;
			if (found >= 0) return -2;
			found = i;
		}
		return found;
	}

	public StructureMirrorData addChild(int parent, BoundingBox bound, BlockPos block, BlockPos childJigsaw) {
		List<Entry> list = new ArrayList<>(entries);
		list.add(new Entry(bound, block, parent, childJigsaw));
		return new StructureMirrorData(list);
	}

	/**
	 * Drops a leaf entry and renumbers the parent indices after it. The caller must have checked
	 * {@link #isLeaf(int)}.
	 */
	public StructureMirrorData removeLeaf(int index) {
		List<Entry> list = new ArrayList<>(entries.size() - 1);
		for (int i = 0; i < entries.size(); i++) {
			if (i == index) continue;
			var entry = entries.get(i);
			int parent = entry.parent();
			if (parent > index) parent--;
			list.add(new Entry(entry.bound(), entry.block(), parent, entry.childJigsaw()));
		}
		return new StructureMirrorData(list);
	}

	public record Entry(BoundingBox bound, BlockPos block, int parent, @Nullable BlockPos childJigsaw) {

		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				BoundingBox.CODEC.fieldOf("bound").forGetter(Entry::bound),
				BlockPos.CODEC.fieldOf("structure_block").forGetter(Entry::block),
				Codec.INT.fieldOf("parent").forGetter(Entry::parent),
				BlockPos.CODEC.optionalFieldOf("child_jigsaw")
						.forGetter(e -> Optional.ofNullable(e.childJigsaw()))
		).apply(i, (bound, block, parent, jigsaw) -> new Entry(bound, block, parent, jigsaw.orElse(null))));

		public boolean contains(BlockPos pos) {
			return pos.getX() >= bound.minX() && pos.getX() <= bound.maxX()
					&& pos.getY() >= bound.minY() && pos.getY() <= bound.maxY()
					&& pos.getZ() >= bound.minZ() && pos.getZ() <= bound.maxZ();
		}

		public long volume() {
			return (long) (bound.maxX() - bound.minX() + 1)
					* (bound.maxY() - bound.minY() + 1)
					* (bound.maxZ() - bound.minZ() + 1);
		}

	}

}