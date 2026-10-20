package dev.xkmc.gensokyolegacy.content.worldgen.feature.ravine;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * One chamber cut into the face of a cliff: a flat floor, a domed ceiling and a mouth that is
 * half an ellipse.
 * <p>
 * The chamber is a quarter of an ellipsoid. Its flat faces are the floor, which is a horizontal
 * plane at the level of the ground outside the cliff, and the cliff itself, which is the vertical
 * plane the ellipsoid is cut against; the dome is what is left of the ellipsoid above the floor.
 * The opening is therefore half an ellipse — flat along the bottom, arched over the top — and the
 * room behind it closes to a point as it goes into the rock.
 * <p>
 * The cliff is found from the heightmap alone, with a box of three slices across the fall and
 * seven along it: the three slices are the wall's two sides and the step between them, and the
 * wall is there when the two outer slices' means stand more than the gap apart. The box is read
 * both ways round, so a wall is found whichever way it looks, and the middle slice is the step
 * into it, which is where the wall's face stands.
 * <p>
 * What the heightmap cannot say is whether there is rock behind the face, so that is read from
 * the blocks: a chamber with no floor under it or no roof over it is not cut at all.
 * <p>
 * The floor is laid in basalt and the middle of it is flooded, which is the ravine's own floor
 * treatment — the plates and their pools — carried into the rock rather than left on the surface.
 *
 * @see RavineCaveConfig
 * @see "doc/design/genbu_ravine_cave.md"
 */
public class RavineCaveFeature extends Feature<RavineCaveConfig> {

	private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

	/**
	 * The slices the box is cut into: the wall's two sides and the step between them.
	 */
	private static final int SLICES = 3;

	/**
	 * How many columns each slice is long.
	 */
	private static final int LONG = 7;

	/**
	 * The least the chamber's air may be, or it is a hole rather than a room.
	 */
	private static final int MIN_HEIGHT = 3;

	/**
	 * How much of the floor the pool covers, as the square of the ellipse it is cut from.
	 */
	private static final double POOL_SHARE = 0.4;

	public RavineCaveFeature(Codec<RavineCaveConfig> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<RavineCaveConfig> ctx) {
		WorldGenLevel level = ctx.level();
		RavineCaveConfig config = ctx.config();
		int x = ctx.origin().getX();
		int z = ctx.origin().getZ();
		Cliff cliff = cliff(level, config, x, z);
		if (cliff == null) return false;
		int floorY = height(level, cliff.extX(), cliff.extZ());
		int topY = height(level, cliff.extX() + cliff.dirX(), cliff.extZ() + cliff.dirZ());
		if (floorY == 0 || topY == 0) return false;
		int height = Math.min(config.height(), topY - 2 - floorY);
		if (height < MIN_HEIGHT) return false;
		return cut(level, ctx.random(), config, cliff, floorY, height);
	}

	/**
	 * The cliff at the site: the column outside the wall and the way the rock lies behind it.
	 * <p>
	 * The box is read across the fall and along the face, and then the other way round. The two
	 * outer slices are the wall's two sides, and the wall is there when they stand clear of each
	 * other. Where the face stands is then read off the three slices themselves: the middle one is
	 * the step between the two sides, so the wall's face is the bigger of the two steps into it,
	 * and the lower column of that pair is the outside.
	 */
	@Nullable
	private static Cliff cliff(WorldGenLevel level, RavineCaveConfig config, int x, int z) {
		Cliff best = null;
		for (boolean acrossX : new boolean[]{true, false}) {
			double first = mean(level, x, z, acrossX, 0);
			double third = mean(level, x, z, acrossX, SLICES - 1);
			if (first == 0 || third == 0) continue;
			double margin = Math.abs(first - third);
			if (margin <= config.gap()) continue;
			if (best != null && margin <= best.margin()) continue;
			int low = height(level, x + (acrossX ? -1 : 0), z + (acrossX ? 0 : -1));
			int mid = height(level, x, z);
			int high = height(level, x + (acrossX ? 1 : 0), z + (acrossX ? 0 : 1));
			if (low == 0 || mid == 0 || high == 0) continue;
			// the face is the bigger of the two steps into the middle slice
			int[] centres = {-1, 0, 1};
			int[] heights = {low, mid, high};
			int pair = Math.abs(mid - low) >= Math.abs(high - mid) ? 0 : 1;
			boolean firstOutside = heights[pair] <= heights[pair + 1];
			int outer = firstOutside ? centres[pair] : centres[pair + 1];
			int inner = firstOutside ? centres[pair + 1] : centres[pair];
			int extX = x + (acrossX ? outer : 0);
			int extZ = z + (acrossX ? 0 : outer);
			int dir = inner > outer ? 1 : -1;
			best = new Cliff(extX, extZ, acrossX ? dir : 0, acrossX ? 0 : dir, margin);
		}
		return best;
	}

	/**
	 * The mean height of one slice of the box.
	 */
	private static double mean(WorldGenLevel level, int x, int z, boolean acrossX, int index) {
		double total = 0;
		for (int i = 0; i < LONG; i++) {
			int sx = acrossX ? x + index - SLICES / 2 : x + i - LONG / 2;
			int sz = acrossX ? z + i - LONG / 2 : z + index - SLICES / 2;
			int h = height(level, sx, sz);
			if (h == 0) return 0;
			total += h;
		}
		return total / LONG;
	}

	/**
	 * The cut itself: the quarter ellipsoid, the basalt floor under it and the pool over that.
	 * <p>
	 * The chamber is checked before it is cut, so a site with no floor under it or no roof over it
	 * is left alone rather than half cut. The roof is what keeps the chamber from opening on the
	 * hill above, and the floor is what makes it a room rather than a hole in the cliff.
	 */
	private boolean cut(WorldGenLevel level, RandomSource rand, RavineCaveConfig config, Cliff cliff, int floorY, int height) {
		double a = config.width() / 2.0;
		double b = height;
		double c = config.depth();
		int perpX = -cliff.dirZ();
		int perpZ = cliff.dirX();
		int half = config.width() / 2;
		List<int[]> columns = new ArrayList<>();
		for (int u = -half; u <= half; u++) {
			for (int d = 1; d <= config.depth(); d++) {
				double span = 1.0 - Math.pow(u / a, 2) - Math.pow(d / c, 2);
				if (span <= 0) continue;
				int top = floorY + (int) Math.floor(b * Math.sqrt(span));
				int cx = cliff.extX() + d * cliff.dirX() + u * perpX;
				int cz = cliff.extZ() + d * cliff.dirZ() + u * perpZ;
				columns.add(new int[]{cx, cz, top, d});

			}
		}
		for (int[] column : columns) {
			if (!level.getBlockState(new BlockPos(column[0], floorY - 2, column[1])).isSolid()) return false;
			if (!level.getBlockState(new BlockPos(column[0], column[2] + 1, column[1])).isSolid()) return false;
		}
		Map<int[], Integer> lining = new HashMap<>();
		Set<int[]> carved = new HashSet<>();
		for (var e : columns) {
			carved.add(new int[]{e[0], e[1]});
		}
		for (var e : columns) {
			for (int i = 0; i < 4; i++) {
				var dir = Direction.from2DDataValue(i);
				int[] pos = new int[]{e[0] + dir.getStepX(), e[1] + dir.getStepZ()};
				if (carved.contains(pos)) continue;
				lining.compute(pos, (key, old) -> old == null ? e[2] : Math.max(old, e[2]));
			}
		}
		for (int[] column : columns) {
			boolean room = false;
			for (int y = floorY; y <= column[2]; y++) {
				var pos = new BlockPos(column[0], y, column[1]);
				BlockState state = level.getBlockState(pos);
				if (state.isAir() || state.is(BlockTags.FEATURES_CANNOT_REPLACE)) continue;
				setAir(level, pos);
				room = true;
			}
			if (!room) continue;
			level.setBlock(new BlockPos(column[0], floorY - 1, column[1]), column[3] <= 1 ? config.floor() : config.pool(), 2);
			level.setBlock(new BlockPos(column[0], floorY - 2, column[1]), config.floor(), 2);
			level.setBlock(new BlockPos(column[0], column[2] + 1, column[1]), config.floor(), 2);
			if (column[2] - floorY > MIN_HEIGHT + 1 && rand.nextBoolean()) {
				level.setBlock(new BlockPos(column[0], column[2], column[1]), config.floor(), 2);
				if (column[2] - floorY > MIN_HEIGHT + 2 && rand.nextBoolean()) {
					level.setBlock(new BlockPos(column[0], column[2] - 1, column[1]), config.floor(), 2);
				}
			}
		}

		for (var ent : lining.entrySet()) {
			for (int y = floorY; y <= ent.getValue(); y++) {
				var pos = new BlockPos(ent.getKey()[0], y, ent.getKey()[1]);
				BlockState state = level.getBlockState(pos);
				if (state.isAir() || state.is(BlockTags.FEATURES_CANNOT_REPLACE)) continue;
				level.setBlock(pos, config.floor(), 2);
			}
		}
		return true;
	}

	private static int height(WorldGenLevel level, int x, int z) {
		int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		return h <= level.getMinBuildHeight() ? 0 : h;
	}

	private void setAir(WorldGenLevel level, BlockPos pos) {
		level.setBlock(pos, AIR, 2);
		level.scheduleTick(pos, AIR.getBlock(), 0);
		markAboveForPostProcessing(level, pos);
	}

	/**
	 * Where the cliff stands and which way the rock lies behind it.
	 */
	private record Cliff(int extX, int extZ, int dirX, int dirZ, double margin) {
	}

}
