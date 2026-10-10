package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the party system needs to know about one food, resolved once and then
 * reused — or, for food that lives in a block entity, read fresh every time.
 *
 * <p>
 * A record rather than separate lookups because they are always wanted together: the
 * planner needs the serves, the appeal needs the properties, the readout wants the id,
 * and the guest needs the action.
 *
 * @param id      what this food <i>is</i>: the id of the one item a serving corresponds
 *                to, or the block's own id when it does not correspond to any single
 *                item. Identity for display and diagnostics — see {@link #idOf}.
 * @param serves  how many servings are left; 0 for a container with nothing in it, and
 *                always 1 for one entry of a container's list, because a container
 *                reports each of its foods separately
 * @param food    what one serving is, or null when there is nothing servable
 * @param consume takes one serving, advancing the block
 *
 * <p>
 * A record, but not a value type in practice: {@code consume} is a lambda and lambdas
 * have no value equality, so two results for the same state are never {@code equals}.
 * That is fine and deliberate — results are handed out from {@link FoodResultCache} and
 * never compared. They are passed around as answers, not as keys.
 */
public record FoodResult(ResourceLocation id, int serves, @Nullable FoodProperties food,
		FoodConsume consume) {

	/**
	 * The id of a result that is not food. A placeholder rather than the block's real id,
	 * so that a bug printing it is visibly a bug and not a plausible-looking food name.
	 */
	public static final ResourceLocation INVALID_ID =
			ResourceLocation.fromNamespaceAndPath("gensokyolegacy", "invalid_food");

	/**
	 * The single answer for food that cannot be served: an emptied container, a block
	 * with no food at all, something with no hunger value, or something poisoned.
	 * <p>
	 * Mapping all of those to one value rather than to several near-zero scores is what
	 * lets callers ask a plain question — {@link #valid()} — instead of each re-deciding
	 * what "worthless" means.
	 */
	public static final FoodResult INVALID = new FoodResult(INVALID_ID, 0, null, (level, pos, state) -> 0);

	/**
	 * The id naming a food, by the one rule: the serving item's where the food is a single
	 * item, the block's otherwise.
	 * <p>
	 * A cake is read as a cake, because the serving is the cake block's own item; a
	 * Farmer's Delight feast read through its {@code *_slice} item is named by that item,
	 * because that is the thing a guest is actually being handed; vanilla cake, which has
	 * no slice item at all, falls back to the block. A container entry is always an item,
	 * because a container holds items — that is the whole reason it is here.
	 */
	public static ResourceLocation idOf(@Nullable ItemLike food, BlockState state) {
		if (food != null) {
			var id = BuiltInRegistries.ITEM.getKey(food.asItem());
			if (id != null) return id;
		}
		return BuiltInRegistries.BLOCK.getKey(state.getBlock());
	}

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
	 * Takes the state to act on as a parameter rather than closing over it. The resolved
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
