package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the party system needs to know about one food, resolved once and then
 * reused — or, for food that lives in a block entity, read fresh every time.
 *
 * <p>
 * A record rather than separate lookups because they are always wanted together: the
 * planner needs the serves, the appeal needs the properties, the readout wants the id,
 * the guest needs the stack she is handed, and the world needs the action.
 *
 * @param serving  the stack a guest is actually handed: the item one serving is worth,
 *                 at the count one serving is. A cutting board hands over a whole plate
 *                 of sushi, so its serving is two; everything else is one. Empty when
 *                 the food has no item form of its own — vanilla cake, whose slices
 *                 exist only as numbers in code — which is what makes {@link #id()} fall
 *                 back to the block
 * @param blockId  the block this food came off, and the id used when there is no
 *                 serving item
 * @param serves   how many servings are left; 0 for a container with nothing in it, and
 *                 however many times a guest can come back for this entry on a container
 *                 that hands out part of a stack at a time
 * @param food     what one serving is worth, or null when there is nothing servable
 * @param consume  takes one serving, advancing the block
 *
 * <p>
 * A record, but not a value type in practice: {@code consume} is a lambda and lambdas
 * have no value equality, so two results for the same state are never {@code equals}.
 * That is fine and deliberate — results are handed out from {@link FoodResultCache} and
 * never compared. They are passed around as answers, not as keys.
 *
 * <p>
 * <b>{@link #serving} is shared, so never hand it out as-is.</b> A cached result is one
 * object for every guest and every draw, and an {@link ItemStack} is mutable: a guest
 * holding it, or anything else that shrinks or empties it, would be editing the party's
 * own answer. {@link #serving()} hands it over and {@link #servingCopy()} hands over one
 * that is hers to do what she likes with.
 */
public record FoodResult(ItemStack serving, ResourceLocation blockId, int serves,
		@Nullable FoodProperties food,
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
	public static final FoodResult INVALID =
			new FoodResult(ItemStack.EMPTY, INVALID_ID, 0, null, (level, pos, state) -> 0);

	/**
	 * What this food <i>is</i>, by one rule.
	 * <p>
	 * The serving item's id where the food has an item form — a feast is named by the
	 * slice a guest is handed, a cutting board by the sushi on it — and the block's own
	 * id otherwise, which is the right answer for vanilla cake and nothing else.
	 * <p>
	 * Derived rather than stored, because the two can disagree and one of them has to be
	 * the truth: a result carries both the serving and the block, and the serving is the
	 * more specific of the two.
	 */
	public ResourceLocation id() {
		if (serving.isEmpty()) return blockId;
		return BuiltInRegistries.ITEM.getKey(serving.getItem());
	}

	/** A stack of this serving, hers to keep: see the note on {@link #serving}. */
	public ItemStack servingCopy() {
		return serving.copy();
	}

	/**
	 * Whether these food properties are worth serving at all.
	 * <p>
	 * No hunger value, or poison or wither on it. Written here rather than in each
	 * handler because it is the rule for a <i>result</i>: a serving is what is left after
	 * this test, and a handler that forgets to apply it cannot make a dud look like food.
	 */
	public static boolean servable(@Nullable FoodProperties food) {
		return food != null && food.nutrition() > 0 && !Servings.refuses(food);
	}

	/**
	 * Assemble a result from what a handler found, or {@link #INVALID} when there is
	 * nothing to serve.
	 *
	 * @param serving the stack one serving is worth, or empty where the food has no item
	 *                form of its own
	 * @param block   the block it came off, which is what the id falls back to
	 */
	public static FoodResult of(ItemStack serving, Block block, int serves,
			@Nullable FoodProperties food, FoodConsume consume) {
		if (serves <= 0 || !servable(food)) return INVALID;
		return new FoodResult(serving, BuiltInRegistries.BLOCK.getKey(block), serves, food, consume);
	}

	/**
	 * Whether this is food a guest may actually eat.
	 * <p>
	 * Every reason a serving is worthless is checked here, not relied upon upstream: a
	 * container with nothing left in it, missing food properties, properties with no
	 * hunger value, and poison or wither.
	 */
	public boolean valid() {
		return serves > 0 && servable(food);
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