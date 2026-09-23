package dev.xkmc.gensokyolegacy.content.worldgen.feature.lake;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds a lake body from a union of ellipsoid blobs, adapted from TheLostLegends.
 * {@code maxHor} x {@code maxHor} footprint, {@code maxVer} tall; {@code valid} marks the
 * carved body, {@code edge} the one-block rim around it (grown {@code margin} - 1 times).
 */
public class LakeMaker {

	private final MagicalForestLakeFeature parent;
	private final int maxHor;
	private final int maxVer;
	private final int[] valid;
	private int[] wet;
	private int[] edge;

	public LakeMaker(MagicalForestLakeFeature parent, int maxHor, int maxVer) {
		this.parent = parent;
		this.maxHor = maxHor;
		this.maxVer = maxVer;
		this.valid = new int[maxHor * maxHor];
		this.edge = new int[maxHor * maxHor];
		this.wet = edge;
	}

	protected boolean isValid(int x, int y, int z) {
		return (valid[x * maxHor + z] & (1 << y)) != 0;
	}

	protected boolean isWet(int x, int y, int z) {
		return (wet[x * maxHor + z] & (1 << y)) != 0;
	}

	protected boolean isEdge(int x, int y, int z) {
		return (edge[x * maxHor + z] & (1 << y)) != 0;
	}

	public void pre(RandomSource rand, MagicalForestLakeFeature.Data data, int size) {
		int offset = data.margin();
		int bottom = 2;
		int maxThick = Math.min(data.depth(), maxVer - bottom - 2);
		for (int trial = 0; trial < size; trial++) {
			double rx = (rand.nextDouble() + 0.5) * data.radius();
			double ry = (rand.nextDouble() + 0.5) * maxThick;
			double rz = (rand.nextDouble() + 0.5) * data.radius();
			double dx = rand.nextDouble() * (maxHor - rx - 2 * offset) + offset + rx / 2;
			double dy = rand.nextDouble() * (maxVer - ry - bottom - 2) + bottom + ry / 2;
			double dz = rand.nextDouble() * (maxHor - rz - 2 * offset) + offset + rz / 2;

			int minX = Math.max(offset, (int) (dx - rx / 2));
			int maxX = Math.min(maxHor - offset - 1, (int) (dx + rx / 2));
			int minZ = Math.max(offset, (int) (dz - rz / 2));
			int maxZ = Math.min(maxHor - offset - 1, (int) (dz + rz / 2));

			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					double x0 = ((double) x - dx) / (rx / 2);
					double z0 = ((double) z - dz) / (rz / 2);
					double y2 = 1 - x0 * x0 - z0 * z0;
					if (y2 < 0) continue;
					double vy = Math.sqrt(y2) * (ry / 2);
					int minY = Math.max(bottom - 1, (int) (dy - vy));
					int maxY = Math.min(Math.min(maxVer - 2, data.depth() + 2), (int) (dy + vy));
					int mask = 0;
					for (int y = minY; y <= maxY; y++) {
						double y0 = ((double) y - dy) / (ry / 2);
						if (y0 * y0 < y2) {
							mask |= 1 << y;
						}
					}
					valid[x * maxHor + z] |= mask;
				}
			}
		}
		for (int dx = 0; dx < maxHor; dx++) {
			for (int dz = 0; dz < maxHor; dz++) {
				int index = dx * maxHor + dz;
				int mask = valid[index] << 1 | valid[index] >> 1;
				if (dx > 0) mask |= valid[index - maxHor];
				if (dx < maxHor - 1) mask |= valid[index + maxHor];
				if (dz > 0) mask |= valid[index - 1];
				if (dz < maxHor - 1) mask |= valid[index + 1];
				edge[dx * maxHor + dz] = ~valid[dx * maxHor + dz] & mask;
			}
		}
		wet = edge;
		for (int i = 1; i < offset; i++) {
			int[] copy = edge.clone();
			for (int dx = 0; dx < maxHor; dx++) {
				for (int dz = 0; dz < maxHor; dz++) {
					int index = dx * maxHor + dz;
					int mask = edge[index] << 1 | edge[index] >> 1;
					if (dx > 0) mask |= edge[index - maxHor];
					if (dx < maxHor - 1) mask |= edge[index + maxHor];
					if (dz > 0) mask |= edge[index - 1];
					if (dz < maxHor - 1) mask |= edge[index + 1];
					copy[index] |= ~valid[index] & ~edge[index] & mask;
				}
			}
			edge = copy;
		}
	}

	public boolean test(WorldGenLevel level, BlockPos origin, MagicalForestLakeFeature.Data data) {
		BlockState fluid = data.fluid();
		var pos = new BlockPos.MutableBlockPos();
		for (int dx = 0; dx < maxHor; dx++) {
			for (int dz = 0; dz < maxHor; dz++) {
				for (int dy = 0; dy < maxVer; dy++) {
					if (!isValid(dx, dy, dz) && !isEdge(dx, dy, dz)) continue;
					pos.setWithOffset(origin, dx - maxHor / 2, dy, dz - maxHor / 2);
					BlockState block = level.getBlockState(pos);
					// never cut through trunks or canopies reaching in from neighbouring chunks
					if (block.is(BlockTags.LOGS) || block.is(BlockTags.LEAVES)) return false;
					if (isEdge(dx, dy, dz)) {
						if (dy >= data.depth() && block.liquid()) return false;
						if (dy < data.depth() && !block.isSolid() && block != fluid) return false;
					}
				}
			}
		}
		return true;
	}

	public void gen(WorldGenLevel level, BlockPos origin, RandomSource rand, MagicalForestLakeFeature.Data data) {
		var pos = new BlockPos.MutableBlockPos();
		for (int dx = 0; dx < maxHor; dx++) {
			for (int dz = 0; dz < maxHor; dz++) {
				for (int dy = 0; dy < maxVer; dy++) {
					if (isValid(dx, dy, dz)) {
						pos.setWithOffset(origin, dx - maxHor / 2, dy, dz - maxHor / 2);
						if (canReplaceBlock(level.getBlockState(pos))) {
							if (dy >= data.depth())
								parent.setAir(level, pos);
							else parent.setFluid(level, pos, data.fluid());
						}
					} else if (isEdge(dx, dy, dz)) {
						pos.setWithOffset(origin, dx - maxHor / 2, dy, dz - maxHor / 2);
						BlockState state = level.getBlockState(pos);
						if ((dy < data.depth() || state.isSolid() && rand.nextInt(2) != 0) &&
								!state.is(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE)) {
							boolean surface = dy >= data.depth() - 1;
							boolean isWet = isWet(dx, dy, dz);
							if (surface) {
								pos.move(Direction.UP);
								surface = level.getBlockState(pos).isAir();
								pos.move(Direction.DOWN);
							}
							if (surface) {
								if (isWet && rand.nextFloat() < 0.4f)
									parent.setBarrier(level, pos, data.barrier());
								else parent.setBarrier(level, pos, data.surface());
							} else if (isWet && rand.nextFloat() < 0.7f)
								parent.setBarrier(level, pos, data.barrier());
							else parent.setBarrier(level, pos, data.padding());
						}
					}
				}
			}
		}
	}

	private boolean canReplaceBlock(BlockState state) {
		return !state.is(BlockTags.FEATURES_CANNOT_REPLACE);
	}

}
