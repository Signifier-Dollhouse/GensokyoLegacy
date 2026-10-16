package dev.xkmc.gensokyolegacy.content.worldgen.feature.lake;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * Builds a lake body from a union of ellipsoid blobs, adapted from TheLostLegends: a {@code maxWidth} square
 * footprint, {@code maxHeight} tall; {@code valid} is the carved body, {@code edge} the rim around it, grown
 * {@code margin} - 1 times.
 * <p>
 * What a rim <em>is</em> depends on where the lake sits, and so does what has to hold for the lake to fit there: a
 * pond is dug into ground and brings a rim of its own, while a cavern pool dams itself with a wall under the
 * waterline. See {@code LakeConfig.Mode}.
 *
 * @see LakeConfig
 * @see "doc/design/gensokyo_biomes.md" §2
 */
public class LakeMaker {

	private final LakeFeature parent;
	private final LakeConfig config;
	private final int maxHor;
	private final int maxVer;
	private final int[] valid;
	private int[] wet;
	private int[] edge;

	public LakeMaker(LakeFeature parent, LakeConfig config) {
		this.parent = parent;
		this.config = config;
		this.maxHor = config.shape().maxWidth();
		this.maxVer = config.shape().maxHeight();
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

	public void pre(RandomSource rand, int size) {
		var shape = config.shape();
		int depth = shape.depth();
		int radius = shape.componentRadius();
		int offset = shape.margin();
		int bottom = 2;
		int maxThick = Math.min(depth, maxVer - bottom - 2);
		// A blob is dropped inside the water zone rather than inside the box, so that its middle is `depth` - 1 blocks
		// of water deep and only its rim tapers: a box taller than its lake carries room above the waterline only so
		// that the clearance can be checked, and spreading the blobs over the box would leave them a lens of water
		// sitting on the bed. The mask may still reach past the waterline into the air the maker cuts above it.
		int bodyLow = bottom - 1;
		int bodyHigh = bodyTop();
		Arrays.fill(valid, 0);
		for (int trial = 0; trial < size; trial++) {
			double rx = (rand.nextDouble() + 0.5) * radius;
			double ry = (rand.nextDouble() + 0.5) * maxThick;
			double rz = (rand.nextDouble() + 0.5) * radius;
			double dx = rand.nextDouble() * (maxHor - rx - 2 * offset) + offset + rx / 2;
			double dy = bodyLow + ry / 2 + rand.nextDouble() * Math.max(0, depth - 1 - bodyLow - ry);
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
					int minY = Math.max(bodyLow, (int) (dy - vy));
					int maxY = Math.min(bodyHigh, (int) (dy + vy));
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

	public boolean test(WorldGenLevel level, BlockPos origin) {
		if (blocked(level, origin)) return false;
		return switch (config.mode()) {
			case SURFACE -> groundFits(level, origin);
			case UNDERGROUND -> cavernFits(level, origin);
		};
	}

	/**
	 * The highest block of the water standing on this footprint cell, or -1 where the footprint holds no water at
	 * all. A blob never reaches the box floor, so the walk down from the waterline finds the top of it.
	 */
	private int waterTop(int dx, int dz) {
		for (int dy = config.shape().depth() - 1; dy >= 1; dy--) {
			if (isValid(dx, dy, dz)) return dy;
		}
		return -1;
	}

	/** Nothing of a neighbouring chunk's tree may be cut through, in either mode. */
	private boolean blocked(WorldGenLevel level, BlockPos origin) {
		var pos = new BlockPos.MutableBlockPos();
		for (int dx = 0; dx < maxHor; dx++) {
			for (int dz = 0; dz < maxHor; dz++) {
				for (int dy = 0; dy < maxVer; dy++) {
					if (!isValid(dx, dy, dz) && !isEdge(dx, dy, dz)) continue;
					pos.setWithOffset(origin, dx - maxHor / 2, dy, dz - maxHor / 2);
					BlockState block = level.getBlockState(pos);
					// never cut through trunks or canopies reaching in from neighbouring chunks
					if (block.is(BlockTags.LOGS) || block.is(BlockTags.LEAVES)) return true;
				}
			}
		}
		return false;
	}

	/** A pond is only dug into ground: its rim under the waterline is solid, and nothing floods it from above. */
	private boolean groundFits(WorldGenLevel level, BlockPos origin) {
		int depth = config.shape().depth();
		var pos = new BlockPos.MutableBlockPos();
		for (int dx = 0; dx < maxHor; dx++) {
			for (int dz = 0; dz < maxHor; dz++) {
				for (int dy = 0; dy < maxVer; dy++) {
					if (!isEdge(dx, dy, dz)) continue;
					pos.setWithOffset(origin, dx - maxHor / 2, dy, dz - maxHor / 2);
					BlockState block = level.getBlockState(pos);
					if (dy >= depth && block.liquid()) return false;
					// rim below the waterline must be solid ground: no ponds on ocean or over cavities
					if (dy < depth && !block.isSolid()) return false;
				}
			}
		}
		return true;
	}

	/**
	 * A cavern pool, checked over the whole footprint rather than the one column it was found in. Every column of
	 * water stands on a bed of solid rock with solid rock under it — a bed over a void would be a shell, and a pool
	 * over a spring would pour into it — holds no liquid, and keeps {@code clearance} of open air over its highest
	 * water, which is what makes the cavern a hall rather than a crevice the lake is rammed into.
	 * <p>
	 * The air pocket the maker cuts above the waterline counts towards that clearance, because a cavern pool is dug
	 * into the floor and the blocks it opens are the floor itself: only what is above that pocket has to be air in
	 * the world already. A column whose water is shallower than the pocket's top therefore asks for nothing.
	 */
	private boolean cavernFits(WorldGenLevel level, BlockPos origin) {
		var pos = new BlockPos.MutableBlockPos();
		for (int dx = 0; dx < maxHor; dx++) {
			for (int dz = 0; dz < maxHor; dz++) {
				int top = waterTop(dx, dz);
				if (top < 0) continue;
				pos.setWithOffset(origin, dx - maxHor / 2, 0, dz - maxHor / 2);
				if (!level.getBlockState(pos).isSolid() || !level.getBlockState(pos.below()).isSolid()) return false;
				for (int dy = 1; dy <= top; dy++) {
					pos.setWithOffset(origin, dx - maxHor / 2, dy, dz - maxHor / 2);
					// a leak path in the water would drain the pool
					if (level.getBlockState(pos).liquid()) return false;
				}
				for (int dy = bodyTop() + 1; dy <= top + config.clearance(); dy++) {
					pos.setWithOffset(origin, dx - maxHor / 2, dy, dz - maxHor / 2);
					if (!level.getBlockState(pos).isAir()) return false;
				}
			}
		}
		return true;
	}

	/** The highest block of the carved body: {@code depth} - 1 of water and two blocks of air above it. */
	private int bodyTop() {
		return Math.min(maxVer - 2, config.shape().depth() + 2);
	}

	public void gen(WorldGenLevel level, BlockPos origin, RandomSource rand) {
		int depth = config.shape().depth();
		var material = config.material();
		var pos = new BlockPos.MutableBlockPos();
		for (int dx = 0; dx < maxHor; dx++) {
			for (int dz = 0; dz < maxHor; dz++) {
				for (int dy = 0; dy < maxVer; dy++) {
					boolean body = isValid(dx, dy, dz);
					if (!body && !isEdge(dx, dy, dz)) continue;
					pos.setWithOffset(origin, dx - maxHor / 2, dy, dz - maxHor / 2);
					BlockState state = level.getBlockState(pos);
					if (body) {
						if (canReplaceBlock(state)) {
							if (dy >= depth) parent.setAir(level, pos);
							else parent.setFluid(level, pos, material.fluid());
						}
						continue;
					}
					if (config.mode() == LakeConfig.Mode.UNDERGROUND) {
						// The basin wall, under the waterline only: a cavern floor is flat, so with no rim to dam it
						// the pool would spread over the whole floor and flood the cavern instead of standing in it.
						if (dy < depth && !state.is(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE))
							parent.setBarrier(level, pos, material.padding());
						continue;
					}
					if ((dy < depth || state.isSolid() && rand.nextInt(2) != 0) &&
							!state.is(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE)) {
						boolean surface = dy >= depth - 1;
						boolean isWet = isWet(dx, dy, dz);
						if (surface) {
							pos.move(Direction.UP);
							surface = level.getBlockState(pos).isAir();
							pos.move(Direction.DOWN);
						}
						if (surface) {
							if (isWet && rand.nextFloat() < 0.4f)
								parent.setBarrier(level, pos, material.barrier());
							else parent.setBarrier(level, pos, material.surface());
						} else if (isWet && rand.nextFloat() < 0.7f)
							parent.setBarrier(level, pos, material.barrier());
						else parent.setBarrier(level, pos, material.padding());
					}
				}
			}
		}
	}

	private boolean canReplaceBlock(BlockState state) {
		return !state.is(BlockTags.FEATURES_CANNOT_REPLACE);
	}

}
