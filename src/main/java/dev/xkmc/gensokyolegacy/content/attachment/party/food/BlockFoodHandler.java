package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.BlockPos;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * How one kind of block food is read and eaten.
 * <p>
 * The party system treats food in whole <b>serves</b> rather than whole blocks, so a
 * handler answers three questions about a <i>placed</i> block: how many servings are
 * left, what one of them is, and how to take one.
 * <p>
 * Each handler also decides for itself which blocks it covers, by {@code instanceof}
 * against the block class it was written for. There is no table mapping blocks to
 * handlers and no special "default" path: the handlers form one ordered list, the first
 * that {@link #handles} the block wins, and the catch-all sits last in that list like
 * any other entry.
 * <p>
 * <b>Why the food properties are needed at all:</b> they are the appeal's source.
 * Nutrition, saturation and beneficial effects give the per-serving value, and poison
 * or wither make a serving worth nothing (§2.2).
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
	 * How many servings are left in this placed block. Never below 0; a block that is
	 * gone or finished reports 0.
	 */
	int serves(BlockState state);

	/**
	 * What one serving of this food is, or null when it is not worth serving
	 * anything — no food properties at all, or something with poison or wither on it.
	 */
	@Nullable
	FoodProperties food(BlockState state);

	/**
	 * Take one serving from the placed block.
	 * <p>
	 * On return the block has been advanced to its next state in the world: a
	 * countdown block steps its property down, a single-serve plate is removed. The
	 * caller is expected to have already read {@link #food} and {@link #serves}
	 * beforehand, since the appeal of the serving it just ate depends on the state
	 * <i>before</i> this call.
	 *
	 * @return how many servings remain afterwards
	 */
	int consume(Level level, BlockPos pos, BlockState state);
}