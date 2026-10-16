package dev.xkmc.gensokyolegacy.content.worldgen.feature.lake;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One lake, dug as a pond into open ground or poured as a pool into a cavern. The shape and the blocks are
 * {@link LakeMaker}'s and {@link LakeConfig}'s; all that the two modes disagree about is where the box goes, so
 * that is all this class decides.
 * <p>
 * A pond is a surface lake: it takes the ground the placement landed near and stands in it, which is why the
 * magical forest's runs in the LAKES step. A pool is neither of the two things a vanilla lake is — it is not poured
 * into whatever the first open block of a downward scan happens to be, which is how {@code lake_underground_water}
 * ends up as a puddle in a one-block pocket, and it is not dropped on the heightmap, which is a pond on the open
 * surface. Instead the column the placement landed in is walked from the floor up to the sky, and the open runs
 * that are roofed, tall enough and floored by solid rock are the sites it can choose from.
 *
 * @see LakeConfig
 * @see "doc/design/gensokyo_biomes.md" §2
 */
public class LakeFeature extends Feature<LakeConfig> {

	private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

	public LakeFeature(Codec<LakeConfig> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<LakeConfig> ctx) {
		WorldGenLevel level = ctx.level();
		LakeConfig config = ctx.config();
		var rand = ctx.random();
		var shape = config.shape();
		var maker = new LakeMaker(this, config);
		for (BlockPos origin : origins(ctx)) {
			maker.pre(rand, rand.nextInt(shape.minComponent(), shape.maxComponent()));
			if (!maker.test(level, origin)) continue;
			maker.gen(level, origin, rand);
			decorate(level, origin, config, rand);
			return true;
		}
		return false;
	}

	public void setAir(WorldGenLevel level, BlockPos pos) {
		setBlock(level, pos, AIR);
		level.scheduleTick(pos, AIR.getBlock(), 0);
		markAboveForPostProcessing(level, pos);
	}

	public void setFluid(WorldGenLevel level, BlockPos pos, BlockState fluid) {
		setBlock(level, pos, fluid);
	}

	public void setBarrier(WorldGenLevel level, BlockPos pos, BlockState barrier) {
		setBlock(level, pos, barrier);
		markAboveForPostProcessing(level, pos);
	}

	/** The box's bottom corner: under the ground for a pond, on the floor of a cavern for a pool. */
	private static List<BlockPos> origins(FeaturePlaceContext<LakeConfig> ctx) {
		return switch (ctx.config().mode()) {
			case SURFACE -> onGround(ctx);
			case UNDERGROUND -> inCavern(ctx);
		};
	}

	/** A pond's box, dropped from the free block above the soil back down to the bottom of the water it holds. */
	private static List<BlockPos> onGround(FeaturePlaceContext<LakeConfig> ctx) {
		BlockPos ground = findGround(ctx.level(), ctx.origin(), 8);
		return ground == null ? List.of() : List.of(ground.below(ctx.config().shape().depth()));
	}

	/**
	 * The caverns of one column, as candidate lake floors.
	 * <p>
	 * The walk goes from the world floor to the topmost rock of the column, and every stretch of open air between
	 * two solid blocks is a room if it carries {@code clearance} blocks of air over its floor and is still roofed
	 * before it reaches the sky, which is what keeps the pool out of the open. It needs no ceiling on its size: a
	 * cavern a hundred blocks high is still a cavern. The rock under the floor has to be solid as well, or the bed
	 * the maker digs would be a shell over a void, and the room has to be in the same biome as the point that chose
	 * the column, so that a cavern crossing a depth band does not get the wrong biome's water.
	 * <p>
	 * A room's block comes back one below its waterline, because the pool is dug into the rock the floor is made of:
	 * the maker's topmost block of water sits on the cavern's own floor, and the air pocket it cuts above that is
	 * what opens the pit into the room. The rooms come back in a random rotation, all of them, so that a column
	 * with several of them does not spend every one of its lakes in the lowest.
	 */
	private static List<BlockPos> inCavern(FeaturePlaceContext<LakeConfig> ctx) {
		WorldGenLevel level = ctx.level();
		LakeConfig config = ctx.config();
		BlockPos origin = ctx.origin();
		int x = origin.getX(), z = origin.getZ();
		int depth = config.shape().depth();
		Holder<Biome> biome = level.getBiome(origin);
		int bottom = level.getMinBuildHeight();
		var pos = new BlockPos.MutableBlockPos();
		var floors = new ArrayList<Integer>();
		// the topmost rock of the column: an open run that reaches it is the sky and not a room
		int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
		int y = bottom + 1;
		while (y <= top) {
			pos.set(x, y, z);
			if (!level.getBlockState(pos).isAir()) {
				y++;
				continue;
			}
			int floor = y - 1;
			while (y <= top && level.getBlockState(pos.set(x, y, z)).isAir()) y++;
			if (y > top) break;
			if (y - 1 - floor < config.clearance()) continue;
			if (floor <= bottom) continue;
			pos.set(x, floor, z);
			if (!level.getBlockState(pos).isSolid() || !level.getBlockState(pos.below()).isSolid()) continue;
			if (!level.getBiome(pos).is(biome)) continue;
			floors.add(floor);
		}
		if (floors.isEmpty()) return List.of();
		int start = ctx.random().nextInt(floors.size());
		var sites = new ArrayList<BlockPos>(floors.size());
		for (int i = 0; i < floors.size(); i++) {
			sites.add(new BlockPos(x, floors.get((start + i) % floors.size()) - (depth - 1), z));
		}
		return sites;
	}

	/**
	 * Waterline planting from the {@code decorations} list: floating plants on open water, shallow ones in 1-deep
	 * water over matching soil, shore ones on matching soil next to water. List order is priority; it runs on the
	 * freshly carved pond.
	 */
	private void decorate(WorldGenLevel level, BlockPos origin, LakeConfig config, RandomSource rand) {
		if (config.decorations().isEmpty()) return;
		int width = config.shape().maxWidth();
		var pos = new BlockPos.MutableBlockPos();
		for (int dx = 0; dx < width; dx++) {
			for (int dz = 0; dz < width; dz++) {
				int x = origin.getX() + dx - width / 2;
				int z = origin.getZ() + dz - width / 2;
				for (int dy = config.shape().maxHeight() - 1; dy >= 0; dy--) {
					pos.set(x, origin.getY() + dy, z);
					BlockState state = level.getBlockState(pos);
					if (state.isAir()) continue;
					if (state.is(Blocks.WATER) && level.getBlockState(pos.above()).isAir()) {
						decorateWater(level, pos.immutable(), config, rand);
					} else if (state.isSolid() && level.getFluidState(pos).isEmpty() &&
							level.getBlockState(pos.above()).isAir()) {
						decorateShore(level, pos.immutable(), config, rand);
					}
					break;
				}
			}
		}
	}

	private void decorateWater(WorldGenLevel level, BlockPos surface, LakeConfig config, RandomSource rand) {
		var probe = new BlockPos.MutableBlockPos();
		probe.set(surface);
		int depth = 0;
		while (level.getBlockState(probe).is(Blocks.WATER)) {
			depth++;
			probe.move(0, -1, 0);
		}
		BlockState soil = level.getBlockState(probe);
		BlockPos top = surface.above();
		for (var deco : config.decorations()) {
			if (deco.target() == LakeConfig.Target.FLOATING) {
				if (depth >= 2 && level.getBlockState(top).isAir() && rand.nextFloat() < deco.chance())
					setBlock(level, top, deco.block());
			} else if (deco.target() == LakeConfig.Target.SHALLOW && depth == 1) {
				if (level.getBlockState(surface).is(Blocks.WATER) && level.getBlockState(top).isAir() &&
						soil.is(deco.soil()) && rand.nextFloat() < deco.chance()) {
					setBlock(level, surface, deco.block());
					if (!deco.upper().isAir()) setBlock(level, top, deco.upper());
				}
			}
		}
	}

	private void decorateShore(WorldGenLevel level, BlockPos ground, LakeConfig config, RandomSource rand) {
		BlockPos top = ground.above();
		BlockState soil = level.getBlockState(ground);
		for (var deco : config.decorations()) {
			if (deco.target() != LakeConfig.Target.SHORE) continue;
			if (soil.is(deco.soil()) && level.getBlockState(top).isAir() &&
					nextToWater(level, ground) && rand.nextFloat() < deco.chance())
				setBlock(level, top, deco.block());
		}
	}

	private static boolean nextToWater(WorldGenLevel level, BlockPos ground) {
		var pos = new BlockPos.MutableBlockPos();
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			pos.setWithOffset(ground, dir);
			// shore ground usually sits level with the water surface, sometimes one above it
			if (level.getBlockState(pos).is(Blocks.WATER)) return true;
			pos.move(0, 1, 0);
			if (level.getBlockState(pos).is(Blocks.WATER)) return true;
		}
		return false;
	}

	/**
	 * Ground search from TheLostLegends OnGroundFeature: down through air to the soil, then
	 * back up to the first free block.
	 */
	@Nullable
	private static BlockPos findGround(LevelAccessor level, BlockPos origin, int maxStep) {
		var pos = new BlockPos.MutableBlockPos();
		pos.set(origin);
		while (level.isEmptyBlock(pos)) {
			pos.move(0, -1, 0);
			maxStep--;
			if (maxStep < 0) return null;
			if (level.isOutsideBuildHeight(pos)) return null;
		}
		maxStep++;
		while (!level.isEmptyBlock(pos)) {
			pos.move(0, 1, 0);
			maxStep--;
			if (maxStep < 0) return null;
			if (level.isOutsideBuildHeight(pos)) return null;
		}
		return pos;
	}

}
