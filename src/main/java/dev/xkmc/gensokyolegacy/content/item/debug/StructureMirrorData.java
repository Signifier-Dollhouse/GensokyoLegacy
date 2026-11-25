package dev.xkmc.gensokyolegacy.content.item.debug;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
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
	 * Parent-side jigsaw stored on the child entry, or null.
	 */
	@Nullable
	public BlockPos parentJigsaw(int index) {
		if (index < 0 || index >= entries.size()) return null;
		return entries.get(index).parentJigsaw();
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
			if (!match && entry.parentJigsaw() != null) match = entry.parentJigsaw().equals(pos);
			if (!match) continue;
			if (found >= 0) return -2;
			found = i;
		}
		return found;
	}

	public StructureMirrorData addChild(int parent, BoundingBox bound, BlockPos block,
	                                    BlockPos parentJigsaw, BlockPos childJigsaw) {
		List<Entry> list = new ArrayList<>(entries);
		list.add(new Entry(bound, block, parent, parentJigsaw, childJigsaw));
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
			list.add(new Entry(entry.bound(), entry.block(), parent, entry.parentJigsaw(), entry.childJigsaw()));
		}
		return new StructureMirrorData(list);
	}

	public record Entry(BoundingBox bound, BlockPos block, int parent,
	                    @Nullable BlockPos parentJigsaw, @Nullable BlockPos childJigsaw) {

		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				BoundingBox.CODEC.fieldOf("bound").forGetter(Entry::bound),
				BlockPos.CODEC.fieldOf("structure_block").forGetter(Entry::block),
				Codec.INT.fieldOf("parent").forGetter(Entry::parent),
				BlockPos.CODEC.optionalFieldOf("parent_jigsaw")
						.forGetter(e -> Optional.ofNullable(e.parentJigsaw())),
				BlockPos.CODEC.optionalFieldOf("child_jigsaw")
						.forGetter(e -> Optional.ofNullable(e.childJigsaw()))
		).apply(i, (bound, block, parent, parentJigsaw, childJigsaw) ->
				new Entry(bound, block, parent, parentJigsaw.orElse(null), childJigsaw.orElse(null))));

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