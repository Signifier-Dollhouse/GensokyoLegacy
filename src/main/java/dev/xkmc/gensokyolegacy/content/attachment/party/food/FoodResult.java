package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.BlockPos;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the party system needs to know about one state of a placed block food,
 * resolved once and then reused.
 *
 * <p>
 * A record rather than three separate lookups because all three are derived from the
 * same block state and are always wanted together: the planner needs the serves, the
 * appeal needs the properties, and the guest needs the action.
 *
 * @param serves  how many servings are left; 0 for a container with nothing in it
 * @param food    what one serving is, or null when there is nothing servable
 * @param consume takes one serving, advancing the block
 *
 * <p>
 * A record, but not a value type in practice: {@code consume} is a lambda and lambdas
 * have no value equality, so two results for the same state are never {@code equals}.
 * That is fine and deliberate — results are handed out from {@link FoodResultCache} and
 * never compared. They are passed around as answers, not as keys.
 */
public record FoodResult(int serves, @Nullable FoodProperties food, FoodConsume consume) {

	/**
	 * The single answer for food that cannot be served: an emptied container, a block
	 * with no food at all, something with no hunger value, or something poisoned.
	 * <p>
	 * Mapping all of those to one value rather than to several near-zero scores is what
	 * lets callers ask a plain question — {@link #valid()} — instead of each re-deciding
	 * what "worthless" means.
	 */
	public static final FoodResult INVALID = new FoodResult(0, null, (level, pos, state) -> 0);

	/**
	 * Whether this is food a guest may actually eat.
	 * <p>
	 * Every reason a serving is worthless is checked here, not relied upon upstream: a
	 * container with nothing left in it, missing food properties, properties with no
	 * hunger value, and poison or wither. Handlers filter most of these already, but
	 * this is the one place the rule is written down, and a handler that forgets to
	 * filter cannot accidentally make a dud look like food.
	 */
	public boolean valid() {
		if (serves <= 0 || food == null) return false;
		return food.nutrition() > 0 && !Servings.refuses(food);
	}

	/**
	 * Take one serving from the placed block.
	 * <p>
	 * Takes the state to act on as a parameter rather than closing over it. The cached
	 * result belongs to the state it was built from, but by the time a guest gets round
	 * to eating, the world may have moved on — another guest may have taken a slice, or
	 * the player may have broken the block — and consuming against a stale state would
	 * put it back or resurrect it.
	 */
	@FunctionalInterface
	public interface FoodConsume {
		/**
		 * @return how many servings remain afterwards
		 */
		int consume(Level level, BlockPos pos, BlockState state);
	}
}