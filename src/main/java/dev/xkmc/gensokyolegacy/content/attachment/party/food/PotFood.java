package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.youkaishomecoming.content.block.food.BowlBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A Youkai's Feasts bowl or pot: two or four servings counted <em>down</em>. The food is found by
 * permuting the block's id first, and only then by reflecting {@code BowlBlock.food}.
 *
 * @see "doc/design/party/food_mods.md"
 */
public class PotFood extends AbstractCountdownFood {

	private static final Field FOOD = foodField();
	private static final Method CONSUME = consumeMethod();

	@Override
	public boolean handles(Block block) {
		return block instanceof BowlBlock;
	}

	/**
	 * Take one serving, and let the bowl decide what it becomes.
	 * <p><b>Not {@code destroyBlock}, which is what the countdown default does.</b> A bowl already
	 * knows what is left of it, and delegating is the only way to be right about <i>which</i> empty
	 * bowl appears.
	 *
	 * @return servings remaining, read off the counter before the block stepped it
	 */
	@Override
	public int consume(Level level, BlockPos pos, BlockState state) {
		int before = serves(state);
		if (CONSUME == null) return super.consume(level, pos, state);
		try {
			// the receiver is the block, not the state: the method is a member of
			// BowlBlock and the state is only its first argument
			CONSUME.invoke(state.getBlock(), state, level, pos);
		} catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
			return super.consume(level, pos, state);
		}
		return Math.max(before - 1, 0);
	}

	/**
	 * The bowl the pot fills: the mod's own item where it ships one, the block's hidden field otherwise.
	 */
	@Override
	public ItemStack serving(BlockState state) {
		var block = state.getBlock();
		var permuted = Permutations.itemOf(block);
		if (permuted != null) return new ItemStack(permuted);
		if (FOOD == null || !(block instanceof BowlBlock bowl)) return ItemStack.EMPTY;
		try {
			var food = (ItemLike) FOOD.get(bowl);
			return food == null ? ItemStack.EMPTY : new ItemStack(food);
		} catch (IllegalAccessException | RuntimeException e) {
			return ItemStack.EMPTY;   // the field went away; this bowl serves nothing
		}
	}

	/**
	 * {@code BowlBlock.food}, located once. Null when it cannot be read.
	 */
	private static Field foodField() {
		try {
			var field = BowlBlock.class.getDeclaredField("food");
			field.setAccessible(true);
			return field;
		} catch (NoSuchFieldException | RuntimeException e) {
			return null;
		}
	}

	/**
	 * {@code BowlBlock.consume(BlockState, Level, BlockPos)}, located once. Null when it
	 * cannot be read, in which case {@link #consume} falls back to destroying the block.
	 */
	private static Method consumeMethod() {
		try {
			var method = BowlBlock.class.getDeclaredMethod("consume",
					BlockState.class, Level.class, BlockPos.class);
			method.setAccessible(true);
			return method;
		} catch (NoSuchMethodException | RuntimeException e) {
			return null;
		}
	}
}