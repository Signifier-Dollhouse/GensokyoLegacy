package dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain;

import dev.xkmc.gensokyolegacy.content.worldgen.structure.PieceTree;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * World access and sampling for one terrain predicate, plus the 9-point footprint ring (4 corners, 4 edge
 * centres, centre) shared by both rules.
 */
public record TerrainFit(Structure.GenerationContext ctx, PieceTree tree, int seaLevel) {

	public int height(int x, int z) {
		return height(ctx, x, z);
	}

	public static int height(Structure.GenerationContext ctx, int x, int z) {
		LevelHeightAccessor level = ctx.heightAccessor();
		return ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
				level, ctx.randomState());
	}

	public int[] ring(BoundingBox box) {
		int x0 = box.minX(), x1 = box.maxX();
		int z0 = box.minZ(), z1 = box.maxZ();
		int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
		return new int[]{
				height(x0, z0), height(x0, z1), height(x1, z0), height(x1, z1),
				height(cx, z0), height(cx, z1), height(x0, cz), height(x1, cz),
				height(cx, cz)
		};
	}

	public static int min(int[] values) {
		int ans = Integer.MAX_VALUE;
		for (int v : values) {
			if (v < ans) ans = v;
		}
		return ans;
	}

	public static int max(int[] values) {
		int ans = Integer.MIN_VALUE;
		for (int v : values) {
			if (v > ans) ans = v;
		}
		return ans;
	}

}
