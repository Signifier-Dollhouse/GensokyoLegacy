package dev.xkmc.gensokyolegacy.content.worldgen.feature.stair;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * A staircase of one block, turned into a row of pools.
 * <p>
 * A block of the stair stone is a <em>middle stair</em> when it is the top block of its column, has a step up on one
 * side and a step down on the other, stands on two solid blocks and has no water beside it. Such a block is the tread
 * of a step in the middle of a slope, and a slope of them is what a steep surface is made of.
 * <p>
 * The carve takes the tread out and fills the block under it, so the step becomes a pool one block deep with a floor
 * of its own, and the run of treads beside it becomes a rim of water along the slope. The run is found by a
 * breadth-first walk over the three by three of blocks on the same level, each of them tried the same way, which stops
 * at the reach the config gives.
 *
 * @see StairPoolConfig
 * @see "doc/design/gensokyo_biomes.md" §1
 */
public class StairPoolFeature extends Feature<StairPoolConfig> {

	public StairPoolFeature(Codec<StairPoolConfig> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<StairPoolConfig> ctx) {
		WorldGenLevel level = ctx.level();
		StairPoolConfig config = ctx.config();
		BlockPos origin = ctx.origin().below();
		if (!level.getBlockState(origin).is(config.stair().getBlock())) {
			return false;
		}
		BlockPos stair = isMiddleStair(level, origin, config) ? origin : findStair(level, origin, config);
		if (stair == null) {
			return false;
		}
		carve(level, stair, config);
		return true;
	}

	/**
	 * The middle stair in the five by five of columns around the position, if there is one.
	 * <p>
	 * The tread of a step is not always the block the placement landed on, but it is the top block of a column near
	 * it, and the columns of a slope are at different heights — so each of them is read from the heightmap rather
	 * than from the level the position is on.
	 */
	@Nullable
	private BlockPos findStair(WorldGenLevel level, BlockPos origin, StairPoolConfig config) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				int x = origin.getX() + dx, z = origin.getZ() + dz;
				BlockPos pos = new BlockPos(x, level.getHeight(config.heightmap(), x, z), z);
				if (isMiddleStair(level, pos, config)) {
					return pos;
				}
			}
		}
		return null;
	}

	/**
	 * The carve and the run of treads beside it, a breadth-first walk over the three by three of blocks on the same
	 * level, each of them tried the same way and each of them reaching one step further.
	 */
	private void carve(WorldGenLevel level, BlockPos start, StairPoolConfig config) {
		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		int d2 = config.reach() * config.reach();
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			if (pos.distSqr(start) > d2) continue;
			if (!seen.add(pos) || !isMiddleStair(level, pos, config))
				continue;
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			level.setBlock(pos.below(), config.fill(), 2);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0) continue;
					queue.add(pos.offset(dx, 0, dz));
				}
			}
		}
	}

	/**
	 * Whether the block is a middle stair: the top block of its column, with a step up on one side and a step down on
	 * the other, standing on two solid blocks, and with no water beside it.
	 */
	private boolean isMiddleStair(WorldGenLevel level, BlockPos pos, StairPoolConfig config) {
		var block = config.stair().getBlock();
		if (!level.getBlockState(pos).is(block) || !level.getBlockState(pos.above()).isAir()) {
			return false;
		}
		boolean higher = false, lower = false;
		for (var dir : Direction.Plane.HORIZONTAL) {
			BlockPos side = pos.relative(dir);
			BlockState state = level.getBlockState(side);
			if (state.getFluidState().is(FluidTags.WATER)) {
				return false;
			}
			if (state.is(block)) {
				higher |= level.getBlockState(side.above()).is(block);
			} else if (state.isAir()) {
				var low = level.getBlockState(side.below());
				if (low.is(block)) lower = true;
				else if (!low.isSolid() && !low.getFluidState().is(FluidTags.WATER)) return false;
			}
		}
		if (!higher || !lower) {
			return false;
		}
		return level.getBlockState(pos.below()).isSolid() && level.getBlockState(pos.below(2)).isSolid();
	}

}
