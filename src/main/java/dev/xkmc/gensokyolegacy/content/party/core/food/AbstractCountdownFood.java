package dev.xkmc.gensokyolegacy.content.party.core.food;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Shared behaviour for every block food that counts its servings down as it is eaten:
 * how many are left, and how to take one.
 *
 * <p>
 * Two conventions are in use and they must not be confused, because assuming the wrong
 * one silently breaks a whole category of food:
 *
 * <table border="1">
 *   <caption>Counting conventions</caption>
 *   <tr><th>Block</th><th>Property</th><th>Fresh</th><th>Counts</th></tr>
 *   <tr><td>vanilla {@code CakeBlock}</td><td>{@code bites} 0..6</td><td>0</td>
 *       <td><b>up</b> — bites taken</td></tr>
 *   <tr><td>FD {@code PieBlock}</td><td>{@code bites} 0..3</td><td>0</td>
 *       <td><b>up</b></td></tr>
 *   <tr><td>FD {@code FeastBlock}</td><td>{@code servings} 0..4</td><td>4</td>
 *       <td><b>down</b> — servings left</td></tr>
 *   <tr><td>YSF {@code PotFoodBlock}</td><td>{@code serve} 1..2 / 1..4</td><td>max</td>
 *       <td><b>down</b></td></tr>
 * </table>
 *
 * <p>
 * The direction is read off the block rather than configured: a property whose
 * <em>default</em> state value is its maximum is a "servings left" counter, and one
 * whose default is its minimum is a "bites taken" counter. Getting this wrong is not a
 * cosmetic bug — assuming "always decrement" would step a feast's servings <em>up</em>
 * towards the maximum it already sits at, so every guest eats and the block never
 * changes, and the party can never be satisfied.
 *
 * <p>
 * The property is found by name because the {@code IntegerProperty} instances belong to
 * the other mods; naming one would mean a hard dependency.
 */
public abstract class AbstractCountdownFood implements BlockFoodHandler {

	/** The property names these block classes actually use for their serve count. */
	private static final String[] COUNT_NAMES = {"bites", "servings", "serve"};

	/**
	 * Each subclass tests the one block class it was written for; subclasses of that
	 * class are covered by the same {@code instanceof}.
	 */
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
	 * The block's own remaining-count property, or null when it has none.
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
	 * Whether this block's counter counts down from its maximum as the food is eaten,
	 * rather than up from zero. Decided from the property's own range, so no foreign
	 * property has to be named.
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