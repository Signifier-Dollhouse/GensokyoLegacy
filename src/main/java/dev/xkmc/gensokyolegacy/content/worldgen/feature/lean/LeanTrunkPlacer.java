package dev.xkmc.gensokyolegacy.content.worldgen.feature.lean;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A trunk that leans away from a wall it stands against.
 * <p>
 * The maples grow in the mountain band, where the walls of the tengu settlement stand in the middle of a grove: a
 * straight trunk there is a tree whose crown is cut in half by the wall next to it. This placer looks for a wall on
 * the four sides of the tree, and if it finds one, the trunk steps away from it as it rises — one block across for
 * every {@code lean_rise} blocks up, so every column of the trunk carries the same two logs.
 * <p>
 * A tree is not refused for want of room: the trunk stops where something stands in its way, and the leaves skip
 * what they cannot grow over.
 *
 * @see "doc/design/magical_forest_vegetation.md" §9.1
 */
public class LeanTrunkPlacer extends TrunkPlacer {

	public static final MapCodec<LeanTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> trunkPlacerParts(i)
			.and(Codec.intRange(1, 16).fieldOf("lean_rise").forGetter(p -> p.leanRise))
			.and(Codec.intRange(1, 16).fieldOf("wall_height").forGetter(p -> p.wallHeight))
			.apply(i, LeanTrunkPlacer::new));

	/**
	 * The blocks of rise one block of lean takes: two up for one across is the maple's lean.
	 */
	private final int leanRise;
	/**
	 * How tall a wall has to stand before the tree leans away from it.
	 */
	private final int wallHeight;

	public LeanTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, int leanRise, int wallHeight) {
		super(baseHeight, heightRandA, heightRandB);
		this.leanRise = leanRise;
		this.wallHeight = wallHeight;
	}

	@Override
	protected TrunkPlacerType<?> type() {
		return GLWorldGen.LEAN_TRUNK_PLACER.get();
	}

	/**
	 * The vanilla tree feature asks whether a column is free before it grows anything, and refuses the tree when the
	 * column is not. A leaning tree does not stand in a column, and a maple that has no room for its crown is a maple
	 * with a cut crown rather than no maple at all, so the question is asked of the path instead, in
	 * {@link #placeTrunk}.
	 */
	@Override
	public boolean isFree(LevelSimulatedReader level, BlockPos pos) {
		return true;
	}

	@Override
	public List<FoliagePlacer.FoliageAttachment> placeTrunk(
			LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter,
			RandomSource random, int freeTreeHeight, BlockPos pos,
			TreeConfiguration config) {
		var wall = findWall(level, pos, random);
		var path = path(pos, freeTreeHeight, wall);
		int trunk = freeHeight(level, path);
		if (trunk < 1) return List.of();
		setDirtAt(level, blockSetter, random, pos.below(), config);
		for (int h = 0; h < trunk; h++) placeLog(level, blockSetter, random, path.get(h), config);
		return crown(path, trunk, freeTreeHeight, config, random);
	}

	/**
	 * The wall beside the tree, if there is one. A tree with walls on two sides picks one of them, and a tree with
	 * none stands straight.
	 */
	@Nullable
	private Direction findWall(LevelSimulatedReader level, BlockPos base, RandomSource random) {
		var walls = new ArrayList<Direction>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (stackHeight(level, base.relative(side)) >= wallHeight) walls.add(side);
		}
		if (walls.isEmpty()) return null;
		return walls.get(random.nextInt(walls.size()));
	}

	/**
	 * The trunk's own line: one block across for every {@code lean_rise} blocks of rise, so the base column carries
	 * the same two logs as every column after it, and the trunk never reaches the wall it leans away from.
	 */
	private List<BlockPos> path(BlockPos base, int height, @Nullable Direction wall) {
		var path = new ArrayList<BlockPos>(height + 1);
		Direction lean = wall == null ? null : wall.getOpposite();
		for (int h = 0; h <= height; h++) {
			if (lean == null) {
				path.add(base.above(h));
				continue;
			}
			path.add(base.relative(lean, Math.min(height - 1, h) / leanRise).above(h));
		}
		return path;
	}

	/**
	 * How many of the path's logs fit before the path runs into something.
	 */
	private int freeHeight(LevelSimulatedReader level, List<BlockPos> path) {
		for (int h = 0; h < path.size() - 1; h++) {
			if (!TreeFeature.validTreePos(level, path.get(h))) return h;
		}
		return path.size() - 1;
	}

	/**
	 * The crown, one attachment per layer and each on the trunk's own line at that height: the leaves follow the lean
	 * instead of hanging in the air beside the tree. The attachment's radius offset carries the layer's distance from
	 * the top of the crown, which is what the foliage placer needs to size the layer.
	 */
	private List<FoliagePlacer.FoliageAttachment> crown(List<BlockPos> path, int trunk, int freeTreeHeight,
														TreeConfiguration config, RandomSource random) {
		int height = config.foliagePlacer.foliageHeight(random, freeTreeHeight, config);
		var attachments = new ArrayList<FoliagePlacer.FoliageAttachment>();
		for (int h = Math.max(0, trunk - height); h <= trunk; h++) {
			attachments.add(new FoliagePlacer.FoliageAttachment(path.get(h), trunk - h, false));
		}
		return attachments;
	}

	/**
	 * How many blocks of wall stand on the given column.
	 */
	private int stackHeight(LevelSimulatedReader level, BlockPos pos) {
		int height = 0;
		while (height < 16 && isWall(level, pos.above(height))) height++;
		return height;
	}

	/**
	 * A wall is anything that stands there and stops you. A tree is not a wall: its logs and leaves do not lean.
	 */
	private static boolean isWall(LevelSimulatedReader level, BlockPos pos) {
		return level.isStateAtPosition(pos, state -> !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)
				&& !state.getCollisionShape(EmptyBlockGetter.INSTANCE, pos).isEmpty());
	}

}
