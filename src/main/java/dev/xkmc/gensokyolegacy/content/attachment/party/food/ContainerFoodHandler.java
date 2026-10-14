package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Food that lives inside a block entity rather than on the block state, so every answer is read
 * fresh — {@link FoodResultCache} must never be routed through here.
 * @see "doc/design/party/food.md"
 */
public interface ContainerFoodHandler {

	/** Tested against the block class, as {@link BlockFoodHandler#handles} is. */
	boolean handles(Block block);

	/**
	 * Every food held, one entry per kind of thing — never a single averaged answer.
	 * {@code serves} is how often a guest can come back, a property of the container: a rack hands
	 * out one item at a time, a cutting board hands over the whole plate.
	 * <p>Freshly built every call, never retained.
	 */
	List<FoodResult> contents(CollisionGetter level, BlockPos pos, BlockState state);

	/** Not the shared furniture test: the rule genuinely differs by container. */
	boolean isPutOut(Level level, BlockPos pos, BlockState state);

	/** Takes whatever is there, not a specific item; the container itself stays. */
	int consume(Level level, BlockPos pos, BlockState state);
}
