package dev.xkmc.gensokyolegacy.init.data.structure;

import dev.xkmc.gensokyolegacy.content.attachment.home.structure.RoomBoxScanner;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Template NBT to room-box bridge. Room boxes themselves are hardcoded in
 * {@code StructureConfigBuilder} from verified scan output; this class only
 * parses template data into the grids {@link RoomBoxScanner} needs, for the
 * dev verifier ({@code StructureSpaceVerifier}) cross-checking live
 * templates against the baked values. Game code never scans at runtime.
 */
public class RoomBoxDatagen {

	public static RoomBoxScanner.RoomScan run(CompoundTag tag, HolderGetter<Block> lookup) {
		ListTag sizeTag = tag.getList("size", Tag.TAG_INT);
		int sx = sizeTag.getInt(0), sy = sizeTag.getInt(1), sz = sizeTag.getInt(2);
		boolean[][][] solid = new boolean[sx][sy][sz];
		boolean[][][] occlude = new boolean[sx][sy][sz];
		if (tag.contains("palettes", Tag.TAG_LIST)) {
			for (var entry : tag.getList("palettes", Tag.TAG_COMPOUND)) {
				CompoundTag palette = (CompoundTag) entry;
				fill(palette.getList("palette", Tag.TAG_COMPOUND),
						palette.getList("blocks", Tag.TAG_COMPOUND),
						lookup, sx, sy, sz, solid, occlude);
			}
		} else {
			fill(tag.getList("palette", Tag.TAG_COMPOUND),
					tag.getList("blocks", Tag.TAG_COMPOUND),
					lookup, sx, sy, sz, solid, occlude);
		}
		return RoomBoxScanner.scan(sx, sy, sz, solid, occlude);
	}

	private static void fill(ListTag palette, ListTag blocks, HolderGetter<Block> lookup,
							 int sx, int sy, int sz, boolean[][][] solid, boolean[][][] occlude) {
		BlockState[] states = new BlockState[palette.size()];
		for (int i = 0; i < states.length; i++)
			states[i] = NbtUtils.readBlockState(lookup, palette.getCompound(i));
		for (var entry : blocks) {
			CompoundTag block = (CompoundTag) entry;
			ListTag posTag = block.getList("pos", Tag.TAG_INT);
			int x = posTag.getInt(0), y = posTag.getInt(1), z = posTag.getInt(2);
			if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz)
				continue;
			BlockState state = states[block.getInt("state")];
			if (state.isAir()) continue;
			solid[x][y][z] = true;
			occlude[x][y][z] = state.canOcclude();
		}
	}

}
