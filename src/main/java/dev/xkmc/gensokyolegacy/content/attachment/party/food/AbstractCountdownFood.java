package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Shared serve count and consume for block foods that count themselves down as they are eaten.
 * The direction is read off the block, never assumed: a fresh state at the property maximum
 * counts down (servings left), one at the minimum counts up (bites taken).
 *
 * @see "doc/design/party/food_mods.md"
 */
public abstract class AbstractCountdownFood implements BlockFoodHandler {

	/**
	 * The property names these block classes use for their serve count.
	 */
	private static final String[] COUNT_NAMES = {"bites", "servings", "serve"};

	@Override
	public abstract boolean handles(Block block);

	@Override
	public int serves(BlockState state) {
		var left = countProperty(state);
		if (left == null) return 1;   // no counter: one indivisible serve
		int value = state.getValue(left);
		return countsDown(state, left) ? value : value + 1;
	}

	@Override
	public int consume(Level level, BlockPos pos, BlockState state) {
		var left = countProperty(state);
		if (left == null) {
			level.destroyBlock(pos, false);
			return 0;
		}
		int value = state.getValue(left);
		int next = countsDown(state, left) ? value - 1 : value + 1;
		if (next < minimum(left) || next > maximum(left)) {
			level.destroyBlock(pos, false);
			return 0;
		}
		level.setBlock(pos, state.setValue(left, next), 3);
		return countsDown(state, left) ? next : next + 1;
	}

	/**
	 * The block's own serve-count property, found by name because the
	 * {@code IntegerProperty} instances belong to the other mods.
	 *
	 * @return null when the block has no counter
	 */
	@Nullable
	protected static IntegerProperty countProperty(BlockState state) {
		for (var property : state.getProperties()) {
			if (!(property instanceof IntegerProperty count)) continue;
			var name = count.getName();
			for (var candidate : COUNT_NAMES) {
				if (name.equals(candidate)) return count;
			}
		}
		return null;
	}

	/**
	 * Whether a fresh block counts down from its maximum (servings left) rather than up from zero (bites taken).
	 */
	private static boolean countsDown(BlockState state, IntegerProperty property) {
		return state.getBlock().defaultBlockState().getValue(property) >= maximum(property);
	}

	private static int minimum(IntegerProperty property) {
		int min = Integer.MAX_VALUE;
		for (var value : property.getPossibleValues()) min = Math.min(min, value);
		return min;
	}

	private static int maximum(IntegerProperty property) {
		int max = Integer.MIN_VALUE;
		for (var value : property.getPossibleValues()) max = Math.max(max, value);
		return max;
	}
}