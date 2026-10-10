package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.attachment.party.food.BlockFoodHandler;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.FoodResult;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.FoodResultCache;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.Servings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How appealing one serving of food is, derived from what it actually is to eat.
 * <p>
 * The appeal of a block food is {@code serves × perServeAppeal}, not a number
 * attached to the block. A vanilla cake is seven servings of a modest slice, a
 * Farmer's Delight feast is six servings of something richer; both are read
 * through a {@link BlockFoodHandler}, which is what knows how many servings a
 * placed block has left and how to take one.
 * <p>
 * <b>Two quantities, deliberately not the same one.</b> Appeal is how much a dish
 * draws a guest in — it is what {@link PartyMath#weight} scales by, and nothing else
 * consumes it. Hunger is how much the dish actually feeds anyone, and that is what the
 * planner budgets and what a guest's {@code fulfillment} is measured against. Splitting
 * them is what lets a rare-effect delicacy be worth drawing a crowd without seating one.
 *
 * <p>
 * The {@code party_food} data map <b>revalues, it does not opt in</b>. Whether a block
 * is party food at all is decided entirely by code: a registered
 * {@link BlockFoodHandler} must recognise it and find something worth eating in it. A
 * datapack row is consulted only afterwards, and only to replace the number the
 * heuristic derived — it can say "this cake is worth twice what its properties imply",
 * never "this barrel is a cake".
 */
public class PartyFoodValue {

	/**
	 * Nutrition and saturation are worth this much appeal per point.
	 */
	private static final float NUTRITION_SCALE = 0.5f;
	private static final float SATURATION_SCALE = 2.0f;
	/**
	 * A beneficial effect is worth a flat bonus per serving.
	 */
	private static final int BENEFICIAL_BONUS = 4;

	/**
	 * The floor for anything that <i>is</i> food; invalid food scores 0 instead.
	 */
	public static final int MIN_APPEAL = 1;
	public static final int MAX_APPEAL = 4000;

	/**
	 * Appeal of one serving, from its food properties. Nutrition and saturation
	 * carry the value, beneficial effects push it up, and poison or wither makes
	 * it unservable.
	 *
	 * @return the appeal of one serving, or 0 for something not worth serving
	 */
	public static int appealOf(FoodProperties props) {
		if (props.nutrition() <= 0 || Servings.refuses(props)) return 0;
		float bonus = 0;
		for (var e : props.effects()) {
			var holder = e.effect().getEffect();
			if (holder != null && holder.value().isBeneficial()) bonus += BENEFICIAL_BONUS;
		}
		return clamp(Math.round(props.nutrition() * NUTRITION_SCALE + props.saturation() * SATURATION_SCALE + bonus));
	}

	/**
	 * The resolved result for this placed block, from {@link FoodResultCache}.
	 * <p>
	 * This is the one entry point everything else goes through: the appeal, the serve
	 * count and consuming are all read off a single cached result rather than each
	 * resolving the block independently.
	 */
	public static FoodResult result(BlockState state) {
		return FoodResultCache.of(state);
	}

	/**
	 * Whether this block is party food at all.
	 * <p>
	 * There is no {@code party_food} tag and no datapack opt-in: a block is food
	 * because a registered {@link BlockFoodHandler} recognises it <i>and</i> there is
	 * something on it worth eating. Nothing a datapack can write changes that answer.
	 * <p>
	 * {@code party_food} is read further down, by {@link #appealPerServe}, and only to
	 * replace a number for a block that already passed here.
	 */
	public static boolean isFood(BlockState state) {
		return result(state).valid();
	}

	/**
	 * Appeal of one serving of this placed block, honouring its handler and then
	 * any override.
	 * <p>
	 * The override is consulted <b>only after</b> validity, so it can revalue a dish
	 * but cannot conjure one: a {@code party_food} row on a block no handler can serve
	 * scores 0 exactly as it would without the row. Validated first, therefore, rather
	 * than checking the override first and letting it short-circuit the test — that way
	 * this method's "0 when not valid food" is true on every path and not just the
	 * common one.
	 *
	 * @return the appeal of one serving, or 0 when the block is not valid food. A 0 is
	 * the signal that the block should be treated as not party food at all:
	 * it is neither counted towards the budget nor walked to and eaten.
	 */
	public static int appealPerServe(RegistryAccess access, BlockState state) {
		var result = result(state);
		if (!result.valid()) return 0;
		var override = PartyFood.of(access, state);
		return override != null ? override.appeal() : appealOf(result.food());
	}

	/**
	 * Total appeal of this placed block — every serving still on it.
	 *
	 * @return the appeal, or 0 for something not worth serving
	 */
	public static int totalAppeal(RegistryAccess access, BlockState state) {
		return appealPerServe(access, state) * result(state).serves();
	}

	/**
	 * Hunger one serving restores: the actual food value, before any seasoning.
	 * <p>
	 * This is the quantity the party system <b>plans</b> in, and it is deliberately not
	 * the appeal. Appeal answers "how nice is this to eat" and feeds nothing but
	 * {@link PartyMath#weight}; hunger answers "how much does this actually sustain
	 * anyone", and is what a guest's {@code fulfillment} is measured against. A dish with
	 * a rare effect is worth more than its ingredients suggest — that is appeal — without
	 * filling anyone up, so a spread of such dishes looks magnificent and feeds nobody.
	 * <p>
	 * Plain {@code nutrition()}, because that is literally the hunger bar. Inventing a
	 * second weighting here would only make the number harder to predict against the
	 * vanilla food a host is actually laying out on their tables.
	 *
	 * @return the hunger of one serving, 0 when the block is not valid food
	 */
	public static int hungerOf(FoodProperties props) {
		return props.nutrition() <= 0 || Servings.refuses(props) ? 0 : props.nutrition();
	}

	/**
	 * Hunger of one serving of this placed block.
	 * <p>
	 * Never reads the {@code party_food} override, and takes no {@code RegistryAccess}:
	 * that map revalues <em>appeal</em>, and letting it reach the planning quantity would
	 * let a datapack silently change how many guests a spread seats — a much larger claim
	 * than adjusting how attractive the food looks.
	 *
	 * @return the hunger of one serving, or 0 when the block is not valid food
	 */
	public static int hungerPerServe(BlockState state) {
		var result = result(state);
		return result.valid() ? hungerOf(result.food()) : 0;
	}

	/** Hunger of this placed block — every serving still on it. */
	public static int totalHunger(BlockState state) {
		return hungerPerServe(state) * result(state).serves();
	}

	/**
	 * Take one serving from a placed block, keeping the world's own block state in
	 * step. The caller must read {@link #appealPerServe} <i>before</i> calling, since
	 * consuming changes the state the appeal is derived from.
	 *
	 * @return servings remaining afterwards
	 */
	public static int consume(ServerLevel level, BlockPos pos, BlockState state) {
		return result(state).consume().consume(level, pos, state);
	}

	private static int clamp(int value) {
		if (value < MIN_APPEAL) return 0;
		return Math.min(value, MAX_APPEAL);
	}

	private PartyFoodValue() {
	}
}