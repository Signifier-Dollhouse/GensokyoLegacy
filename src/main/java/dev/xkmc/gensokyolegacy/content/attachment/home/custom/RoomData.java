package dev.xkmc.gensokyolegacy.content.attachment.home.custom;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@SerialClass
public class RoomData {

	@SerialClass
	public static class ColumnData {

		@SerialField
		public int[][] list = new int[0][];

		public void addHeight(int y, int length) {
			int y1 = y + length - 1;
			int n = list.length;
			var old = list;
			list = new int[n + 1][];
			int k = 0;
			for (int i = 0; i < n; i++) {
				if (old[i][0] < y) {
					if (old[i][1] < y) {
						list[k++] = old[i];
					} else {
						throw new IllegalArgumentException("Overlapping column: [%d, %d] collides with [%d, %d]".formatted(old[i][0], old[i][1], y, y1));
					}
				} else if (k == i) {
					if (old[i][0] <= y1) {
						throw new IllegalArgumentException("Overlapping column: [%d, %d] collides with [%d, %d]".formatted(old[i][0], old[i][1], y, y1));
					} else {
						list[k++] = new int[]{y, y1};
						list[k++] = old[i];
					}
				} else list[k++] = old[i];
			}
			if (k == n) {
				list[k] = new int[]{y, y1};
			}
		}

		public boolean isInside(int y) {
			for (var arr : list) {
				if (arr[0] <= y && y <= arr[1])
					return true;
			}
			return false;
		}

	}

	@SerialField
	public ColumnData[][] columns;
	@SerialField
	public int xSize, zSize, xOffset, zOffset;
	@SerialField
	public BoundingBox bound;

	public RoomData() {
	}

	public RoomData(int x0, int y0, int z0, int x1, int y1, int z1) {
		xOffset = x0;
		zOffset = z0;
		xSize = x1 - x0 + 1;
		zSize = z1 - z0 + 1;
		columns = new ColumnData[xSize][zSize];
		for (int x = 0; x < xSize; x++) {
			for (int z = 0; z < zSize; z++) {
				columns[x][z] = new ColumnData();
			}
		}
		bound = new BoundingBox(x0, y0, z0, x1, y1, z1);
	}

	public ColumnData getColumn(int px, int pz) {
		int x = px - xOffset;
		int z = pz - zOffset;
		if (x < 0 || x >= xSize || z < 0 || z >= zSize)
			throw new IllegalArgumentException("Column out of bound: (%d, %d) is outside of (%d, %d) ~ (%d, %d)".formatted(
					px, pz, xOffset, zOffset, xOffset + xSize - 1, zOffset + zSize - 1));
		return columns[x][z];
	}

	public void acceptColumn(BlockPos p, int height) {
		getColumn(p.getX(), p.getZ()).addHeight(p.getY(), height);
	}

	public boolean isInside(BlockPos p) {
		return getColumn(p.getX(), p.getZ()).isInside(p.getY());
	}

}
