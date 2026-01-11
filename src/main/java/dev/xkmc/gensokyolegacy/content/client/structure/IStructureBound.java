package dev.xkmc.gensokyolegacy.content.client.structure;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

public interface IStructureBound {

	Box house();

	Box structure();

	Box room();

	record Box(int x0, int y0, int z0, int x1, int y1, int z1) {

		public static Box of(BoundingBox box) {
			return new Box(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
		}

		public BoundingBox toBox() {
			return new BoundingBox(x0, y0, z0, x1, y1, z1);
		}

	}
}
