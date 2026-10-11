package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.attachment.party.food.BlockFoodHandler;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.FoodAppeal;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.FoodResult;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.FoodResultCache;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.Servings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * How appealing one serving of food is, derived from what it actually is to eat.
 * <p>
 * <b>Appeal is a property of a food, not of a quantity of it.</b> A vanilla cake is seven
 * servings of a modest slice and a Farmer's Delight feast is six of something richer, and
 * neither is worth seven or six times as much as one slice or one portion: a party counts
 * each <i>food</i> once, so {@link #appealPerFood} answers in {@link FoodAppeal}s naming
 * the food, and the caller is expected to keep a set of the ids it has already paid for.
 * What it does multiply by is the serving count is the guest's own number,
 * {@link #appealPerServe} — a guest eats seven slices and is moved by seven of them, which
 * is why the same quantity is deliberately kept in two shapes.
 * <p>
 * <b>Two quantities, deliberately not the same one.</b> Appeal is how much a dish
 * draws a guest in — it is what {@link PartyMath#weight} scales by, and nothing else
 * consumes it. Hunger is how much the dish actually feeds anyone, and that is what the
 * planner budgets and what a guest's {@code fulfillment} is measured against. Splitting
 * them is what lets a rare-effect delicacy be worth drawing a crowd without seating one.
 * Hunger is the quantity that scales with what is left on the block; appeal is not.
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
	 * with {@code serves} set to how many times a guest can come back for it — a steamer
	 * hands out one item at a time, so a stack of three is three servings of one entry,
	 * while a cutting board hands the whole stack over at once, so the same stack there is
	 * one serving carrying three items' worth of properties. Either way the two numbers
	 * agree on the total, and they differ on what a guest actually does.
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
	 * What a placed block offers a party, appeal-wise: one entry per <b>distinct food</b>
	 * on it.
	 *
	 * <p>
	 * <b>A list rather than a number, and that is the whole point.</b> Appeal is counted
	 * once per food id and not once per block, so the caller has to be able to recognise
	 * the same food arriving twice — a second cake by the door is the same cake as the one
	 * at the far end, and ten blocks of one casserole are one casserole. A bare {@code int}
	 * carries no id and so can only be summed, which is exactly the answer this replaced.
	 * The caller keeps the set of ids it has already paid for and drops the rest; see
	 * {@code PartyArea.scan}.
	 *
	 * <p>
	 * Neither {@link #serves()} nor the serving count multiplies anything here. A cake
	 * with seven slices left is one entry worth one slice, because what the party is
	 * measuring is how good the spread is and seven slices of one cake is not seven times
	 * as good. That does not make the cake less worth putting out — it still feeds seven
	 * guests, through {@link #totalHunger}, which is the other quantity and deliberately
	 * does scale.
	 *
	 * <p>
	 * The override is honoured per entry, exactly as {@link #appealPerServe} honours it
	 * per serving: a {@code party_food} row revalues what is on the block, and there is
	 * no finer key on a container to reach for.
	 *
	 * @return one entry per food id worth serving, empty when nothing here attracts
	 */
	public static List<FoodAppeal> appealPerFood(RegistryAccess access, Level level, BlockPos pos,
			BlockState state) {
		var override = PartyFood.of(access, state);
		var ans = new ArrayList<FoodAppeal>();
		for (var result : results(level, pos, state)) {
			if (!result.valid()) continue;
			int per = override != null ? clamp(override.appeal()) : result.appeal();
			if (per <= 0) continue;
			// A linear scan, because the list is one entry long for nearly every block
			// there is and only a loaded steamer gets long. The point of the check is the
			// steamer: two racks of the same bun are one food, not two.
			boolean seen = false;
			for (var entry : ans) {
				if (!entry.id().equals(result.id())) continue;
				seen = true;
				break;
			}
			if (seen) continue;
			ans.add(new FoodAppeal(result.id(), per));
		}
		return ans;
	}

	/**
	 * Whether this block puts anything attractive on the table, without building the list
	 * {@link #appealPerFood} would.
	 *
	 * <p>
	 * The same test, short-circuiting on the first food that passes, because the guest
	 * sweep asks it of every block in a thirteen-cube box and only ever wants the yes or
	 * the no. Kept beside it rather than inlined as {@code !appealPerFood(..).isEmpty()}
	 * so there is one definition of "attracts" that cannot drift into two answers.
	 *
	 * <p>
	 * Deliberately <b>not</b> deduplicated against the rest of the party: this asks what
	 * this block is worth, and a block is part of the spread whatever else the spread
	 * already has. Dropping a duplicate here would quietly delete a second cake from the
	 * food list and stop it feeding anybody.
	 */
	public static boolean attracts(RegistryAccess access, Level level, BlockPos pos, BlockState state) {
		var override = PartyFood.of(access, state);
		for (var result : results(level, pos, state)) {
			if (!result.valid()) continue;
			int per = override != null ? clamp(override.appeal()) : result.appeal();
			return per > 0;
		}
		return false;
	}

	/**
	 * Appeal of one serving of this placed block, honouring its handler and then
	 * any override.
	 * <p>
	 * <b>This is the guest's number, not the party's.</b> It is what one plate is worth to
	 * the guest who eats it, and it multiplies by the number of servings, because she eats
	 * them all: the party adds up {@link FoodAppeal}s and dedupes them, while a single
	 * guest accumulates {@link #appealPerServe} once per plate she finishes. The two are
	 * meant to disagree — the same seven slices of cake are one entry of appeal to the
	 * party and seven servings of it to whoever eats them.
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
	 * {@code {serves, weightedAppeal, totalHunger}}, from one pass over the block's
	 * results.
	 *
	 * <p>
	 * The appeal here is <b>weighted</b> rather than a total on purpose: it is the sum over
	 * servings, because {@link #appealPerServe} averages it back out and a guest eating
	 * three slices is moved by three of them. Nothing party-wide reads this number — that
	 * is {@link #appealPerFood}, which does not weight at all.
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
			int per = override != null ? clamp(override.appeal()) : result.appeal();
			appeal += per * result.serves();
			// the result's own nutrition, already the plate's: a cutting board hands over
			// a pair of sushi and says so here, which is the one thing the appeal above
			// is careful not to do
			hunger += result.nutrition() * result.serves();
		}
		return new int[]{serves, appeal, hunger};
	}

	/**
	 * Hunger of one serving of this placed block, averaged the same way
	 * {@link #appealPerServe} averages appeal.
	 *
	 * <p>
	 * <b>This is the quantity the party system <i>plans</i> in, and it is deliberately not
	 * the appeal.</b> Appeal answers "how nice is this to eat" and feeds nothing but
	 * {@link PartyMath#weight}; hunger answers "how much does this actually sustain
	 * anyone", and is what a guest's {@code fulfillment} is measured against. A dish with
	 * a rare effect is worth more than its ingredients suggest — that is appeal — without
	 * filling anyone up, so a spread of such dishes looks magnificent and feeds nobody.
	 *
	 * <p>
	 * Plain {@link FoodResult#nutrition()} per result, summed over its servings, because
	 * nutrition is literally the hunger bar. Inventing a second weighting here would only
	 * make the number harder to predict against the vanilla food a host is actually laying
	 * out on their tables.
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
