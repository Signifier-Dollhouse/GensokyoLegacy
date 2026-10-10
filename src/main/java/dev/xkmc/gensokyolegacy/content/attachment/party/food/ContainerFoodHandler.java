package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Food that lives inside a block entity rather than on the block state.
 *
 * <p>
 * The ordinary {@link BlockFoodHandler} answers from a {@link BlockState} alone, which is
 * what lets its answers be cached: a block has a bounded number of states, so
 * {@link FoodResultCache} can key on them and never grow. A container has no such
 * property — the same {@code steamer_rack} block holds bread on one tick and nothing on
 * the next, and the state does not change when a guest eats from it. Anything derived
 * from a container's contents therefore <b>cannot</b> be cached, and this interface is
 * why: every answer is read fresh, from the level and the position.
 *
 * <p>
 * That is not only a performance note, it is the reason the two are separate interfaces.
 * Nothing here may be answered from a {@code BlockState}, because any such answer would
 * be right once and wrong forever after.
 *
 * <p>
 * A container reports <b>every kind of food it holds</b>, not one. A board with apples,
 * cheese and a carrot is three answers with three different values, and a single
 * averaged one would misprice both the spread and the guest drawing from it.
 */
public interface ContainerFoodHandler {

	/**
	 * Whether this handler knows how to read the given block. Tested against the block
	 * class, the same way {@link BlockFoodHandler#handles} is.
	 */
	boolean handles(Block block);

	/**
	 * Every food in this placed container, one entry per kind of thing held.
	 * <p>
	 * Each entry is a single serving of one item — {@code serves} is always 1 — because
	 * a container's contents are items, and an item is one serving however many of them
	 * are stacked. Two apples are two entries, not one entry with two serves, because
	 * they are two servings a guest can come back for.
	 * <p>
	 * The returned list is freshly built every call and must never be retained: it
	 * describes this instant of this world, and holding on to it is exactly the bug the
	 * cache would otherwise have hidden.
	 */
	List<FoodResult> contents(Level level, BlockPos pos, BlockState state);

	/**
	 * Whether this container counts as put out for the party.
	 * <p>
	 * Not the one shared {@link dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyArea#isLaidOut}
	 * test, because the rule genuinely differs by block: a cutting board is only food at a
	 * party when it is on the table, while a steamer pot may stand on a stove, on the
	 * table or on the ground, and a rack inside it is fed by the pot rather than by
	 * anything it touches. One rule for all containers would have to be the loosest of
	 * them, which would put boards on the floor into the spread.
	 */
	boolean isPutOut(Level level, BlockPos pos, BlockState state);

	/**
	 * Take one serving — one item — out of this container.
	 * <p>
	 * Consumes whatever is there, not a specific item, so a guest handed an apple takes
	 * an apple. The container itself stays: it is furniture the host laid out, and
	 * emptying it is not the same as eating it.
	 *
	 * @return how many items remain afterwards
	 */
	int consume(Level level, BlockPos pos, BlockState state);
}
