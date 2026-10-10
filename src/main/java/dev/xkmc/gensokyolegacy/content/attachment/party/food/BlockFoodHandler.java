package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.BlockPos;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * How one kind of block food is read and eaten.
 * <p>
 * The party system treats food in whole <b>serves</b> rather than whole blocks, so a
 * handler answers everything about a <i>placed</i> block in one call: what one serving
 * is, what a guest is handed for it, how many are left, and how to take one.
 * <p>
 * <b>One call, not four.</b> It used to be {@code serves} plus {@code food} plus an
 * {@code id}, with the cache assembling the answer out of the pieces. That split put the
 * rules that matter — which id names this food, and what exactly a guest is handed —
 * nowhere in particular: the id was a default method overridden by two handlers and
 * guessed at by everyone else, and the stack a guest picks up had to be reconstructed
 * from that id by the caller, which is how a cutting board's pair of sushi became one
 * piece. A handler that returns the finished {@link FoodResult} has no room to get
 * those two wrong.
 * <p>
 * Each handler still decides for itself which blocks it covers, by {@code instanceof}
 * against the block class it was written for. There is no table mapping blocks to
 * handlers and no special "default" path: the handlers form one ordered list, the first
 * that {@link #handles} the block wins, and the catch-all sits last in that list like
 * any other entry.
 *
 * @see dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodHandler for the list
 */
public interface BlockFoodHandler {

	/**
	 * Whether this handler knows how to read the given block. Implementations test the
	 * block class they were written for, so subclasses are covered automatically —
	 * one handler for {@code FeastBlock} serves every feast, and so on down.
	 */
	boolean handles(Block block);

	/**
	 * Everything one serving of this placed block is, or {@link FoodResult#INVALID} when
	 * there is nothing to serve.
	 * <p>
	 * Final in practice: the three parts below are overridable and this assembles them,
	 * so a handler overrides what makes it different and inherits the rules that are the
	 * same for all of them.
	 */
	default FoodResult result(BlockState state) {
		return result(serving(state), state.getBlock(), serves(state), food(state));
	}

	/**
	 * How many servings are left in this placed block. Never below 0; a block that is
	 * gone or finished reports 0.
	 * <p>
	 * One by default: a block with no serve counter is one indivisible serving, which is
	 * what every single-serve dish is.
	 */
	default int serves(BlockState state) {
		return 1;
	}

	/**
	 * The stack one serving is worth — what a guest is handed, at what count.
	 * <p>
	 * The block's own item by default, which is right for a dish eaten where it lies. A
	 * handler whose mod has a real serving item overrides this with it: the slice a
	 * feast hands out, the piece a pie is cut into, the bowl a pot fills. A handler whose
	 * food has no item form at all — vanilla cake — returns an empty stack, which is what
	 * makes the food named after its block.
	 */
	default ItemStack serving(BlockState state) {
		return new ItemStack(state.getBlock());
	}

	/**
	 * What one serving is worth.
	 * <p>
	 * Read off the serving stack by default, because that is where the properties live
	 * for every food that has an item form. Only a handler whose properties are somewhere
	 * else — vanilla cake's hard-coded numbers, a Cookery block fed from its registry
	 * entry — overrides this.
	 *
	 * @return null when there is nothing worth serving: no properties, no hunger value,
	 * or something {@code FeedModule} refuses
	 */
	@Nullable
	default FoodProperties food(BlockState state) {
		return Servings.of(serving(state));
	}

	/**
	 * Take one serving from the placed block.
	 * <p>
	 * On return the block has been advanced to its next state in the world: a
	 * countdown block steps its property down, a single-serve plate is removed. The
	 * caller is expected to have read the {@link #result} beforehand, since the appeal of
	 * the serving it just ate depends on the state <i>before</i> this call.
	 *
	 * @return how many servings remain afterwards
	 */
	int consume(Level level, BlockPos pos, BlockState state);

	/**
	 * Assemble the finished answer, or {@link FoodResult#INVALID} when there is nothing
	 * to serve.
	 * <p>
	 * The single place the "worthless" rule is applied, so that a handler cannot forget
	 * it: an empty dish, no properties and a poisoned one all come back as the same
	 * value, and every caller only has to ask {@link FoodResult#valid()}.
	 */
	default FoodResult result(ItemStack serving, Block block, int serves, @Nullable FoodProperties food) {
		return FoodResult.of(serving, block, serves, food, this::consume);
	}
}