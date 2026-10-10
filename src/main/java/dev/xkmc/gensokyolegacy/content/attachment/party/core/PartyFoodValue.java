package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.attachment.party.food.BlockFoodHandler;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.FoodResult;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.FoodResultCache;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.Servings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

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
 *
 * <p>
 * <b>Everything here takes a level and a position</b>, not just a block state. Most food
 * describes itself entirely by its state and is cached; food in a container cannot be,
 * because the same block holds different things on different ticks. Rather than have two
 * parallel sets of methods — one state-only, one not — there is one set that takes the
 * world and routes each block the way it has to be answered. The cost is that the common
 * case carries two arguments it does not use.
 *
 * <p>
 * That has a price worth naming: the answers below are <b>a snapshot, not a transaction</b>.
 * Reading the serves and then consuming is two separate looks at the world, so two guests
 * converging on the same dish can both read it as present and the second finds nothing
 * there. The consequence is bounded — a guest occasionally eats one dish less than she
 * banked — and closing it properly would mean holding a reservation across ticks, which
 * is a far larger change than the window is worth.
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
	 * Every food this placed block offers, as a fresh list.
	 *
	 * <p>
	 * One entry for nearly everything: the whole serving count of one kind of food. A
	 * container is the exception and reports <b>one entry per kind of thing it holds</b>,
	 * with {@code serves} set to how many of that item there are — two apples are two
	 * servings of one entry, not two entries, because they are one food a guest comes
	 * back to.
	 *
	 * <p>
	 * Never a retained reference. A container's answer describes this instant; a
	 * state-derived one is safely cached and shared, but nothing here lets a caller tell
	 * the two apart, so all of them are treated as a snapshot.
	 */
	public static List<FoodResult> results(Level level, BlockPos pos, BlockState state) {
		var container = PartyFoodHandler.containerOf(state.getBlock());
		// a container is never answered from the cache: its contents are not in the state
		if (container != null) return container.contents(level, pos, state);
		return List.of(FoodResultCache.of(state));
	}

	/** How many servings of anything this block offers. */
	public static int serves(Level level, BlockPos pos, BlockState state) {
		int total = 0;
		for (var result : results(level, pos, state)) total += Math.max(0, result.serves());
		return total;
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
	public static boolean isFood(Level level, BlockPos pos, BlockState state) {
		for (var result : results(level, pos, state)) {
			if (result.valid()) return true;
		}
		return false;
	}

	/**
	 * Appeal of one serving of this placed block, honouring its handler and then
	 * any override.
	 * <p>
	 * For a container this is the <b>average across what it holds</b>, not the appeal of
	 * any one thing on it: a board of apples and Roquefort is worth what its servings are
	 * collectively worth, and there is no single dish there to price. Uniform contents —
	 * which is most containers in practice — make the average exactly the one value.
	 * <p>
	 * The override is consulted <b>only after</b> validity, so it can revalue a dish
	 * but cannot conjure one: a {@code party_food} row on a block no handler can serve
	 * scores 0 exactly as it would without the row. Nothing invalid is counted at all,
	 * rather than being counted as a zero the override could then fill in — that is what
	 * makes "0 when not valid food" true on every path and not just the common one.
	 *
	 * @return the appeal of one serving, or 0 when the block is not valid food. A 0 is
	 * the signal that the block should be treated as not party food at all:
	 * it is neither counted towards the budget nor walked to and eaten.
	 */
	public static int appealPerServe(RegistryAccess access, Level level, BlockPos pos, BlockState state) {
		var totals = totals(access, level, pos, state);
		return totals[0] <= 0 ? 0 : totals[1] / totals[0];
	}

	/**
	 * Total appeal of this placed block — every serving still on it.
	 *
	 * @return the appeal, or 0 for something not worth serving
	 */
	public static int totalAppeal(RegistryAccess access, Level level, BlockPos pos, BlockState state) {
		return totals(access, level, pos, state)[1];
	}

	/**
	 * {@code {serves, totalAppeal, totalHunger}}, from one pass over the block's results.
	 *
	 * <p>
	 * One pass because these three are asked together and because the third reason matters:
	 * for a container each pass is a fresh look at a block entity, and reading it three
	 * times to compute one number invites three different answers.
	 *
	 * <p>
	 * An index triple rather than three small records, because it is built once per block
	 * for the whole of a party — a scan asks this of every position it walks — and a
	 * three-field record per query is a heap cost the arithmetic reads perfectly well
	 * without.
	 */
	private static int[] totals(RegistryAccess access, Level level, BlockPos pos, BlockState state) {
		var override = PartyFood.of(access, state);
		int serves = 0, appeal = 0, hunger = 0;
		for (var result : results(level, pos, state)) {
			if (!result.valid()) continue;
			serves += result.serves();
			// the override revalues the block. On a container that means everything on it:
			// there is no finer key to reach for, and a row saying what a board is worth
			// should mean the whole board rather than one apple on it
			int per = override != null ? clamp(override.appeal()) : appealOf(result.food());
			appeal += per * result.serves();
			hunger += hungerOf(result.food()) * result.serves();
		}
		return new int[]{serves, appeal, hunger};
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
	 * Hunger of one serving of this placed block, averaged the same way
	 * {@link #appealPerServe} averages appeal.
	 *
	 * <p>
	 * Never reads the {@code party_food} override, and takes no {@code RegistryAccess}:
	 * that map revalues <em>appeal</em>, and letting it reach the planning quantity would
	 * let a datapack silently change how many guests a spread seats — a much larger claim
	 * than adjusting how attractive the food looks.
	 *
	 * @return the hunger of one serving, or 0 when the block is not valid food
	 */
	public static int hungerPerServe(Level level, BlockPos pos, BlockState state) {
		var totals = totals(level.registryAccess(), level, pos, state);
		return totals[0] <= 0 ? 0 : totals[2] / totals[0];
	}

	/** Hunger of this placed block — every serving still on it. */
	public static int totalHunger(Level level, BlockPos pos, BlockState state) {
		return totals(level.registryAccess(), level, pos, state)[2];
	}

	/**
	 * Take one serving from a placed block, keeping the world's own block state in
	 * step. The caller must read {@link #appealPerServe} <i>before</i> calling, since
	 * consuming changes the state the appeal is derived from.
	 *
	 * <p>
	 * On a container this takes one item, and specifically one of the kind that was
	 * read: a guest who drew for an apple takes an apple, even if the cheese was on top
	 * of the pile.
	 *
	 * @return servings remaining afterwards
	 */
	public static int consume(ServerLevel level, BlockPos pos, BlockState state) {
		for (var result : results(level, pos, state)) {
			if (result.valid()) return result.consume().consume(level, pos, state);
		}
		return 0;
	}

	private static int clamp(int value) {
		if (value < MIN_APPEAL) return 0;
		return Math.min(value, MAX_APPEAL);
	}

	private PartyFoodValue() {
	}
}
