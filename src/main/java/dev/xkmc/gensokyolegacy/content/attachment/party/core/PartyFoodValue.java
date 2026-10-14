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
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * How appealing one serving is, derived from what it is to eat: appeal draws a guest in, hunger
 * is what the food feeds and what the planner budgets (select.md §2.1).
 * <p>
 * Everything takes a level and a position so containers can be answered, making the answers a
 * snapshot rather than a transaction.
 *
 * @see "doc/design/party/food.md"
 */
public class PartyFoodValue {

	private static final float NUTRITION_SCALE = 0.5f;
	private static final float SATURATION_SCALE = 2.0f;

	/** A flat bonus per beneficial effect. */
	private static final int BENEFICIAL_BONUS = 4;

	/** The floor for anything that <i>is</i> food; invalid food scores 0 instead. */
	public static final int MIN_APPEAL = 1;
	public static final int MAX_APPEAL = 4000;

	/** @return the appeal of one serving, or 0 for something not worth serving */
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
	 * Every food this block offers. One entry, except a container, which reports one per
	 * kind of thing held. A fresh list every call — never retained.
	 */
	public static List<FoodResult> results(CollisionGetter level, BlockPos pos, BlockState state) {
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

	/** Decided entirely in code; nothing a datapack can write changes the answer. */
	public static boolean isFood(CollisionGetter level, BlockPos pos, BlockState state) {
		for (var result : results(level, pos, state)) {
			if (result.valid()) return true;
		}
		return false;
	}

	/**
	 * One entry per <b>distinct food</b>, so a caller pays for it once however many blocks it is on.
	 * Serve counts multiply nothing: seven slices of one cake is not seven times as good, though it
	 * feeds seven guests via {@link #totalHunger}.
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
			// there is and only a loaded steamer gets long.
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
	 * {@link #appealPerFood} would. Deliberately <b>not</b> deduplicated against the rest of the
	 * party: a block is part of the spread whatever else is there, and dropping a duplicate would
	 * quietly stop it feeding anybody.
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
	 * <b>The guest's number, not the party's</b> — a plate is worth what its servings are collectively
	 * worth to whoever eats them, which is why this multiplies by serves and {@link #appealPerFood}
	 * does not. A container is averaged across what it holds.
	 * <p>
	 * The override is read <b>only after</b> validity, so a row can revalue a dish but cannot conjure one.
	 *
	 * @return 0 when the block is not valid food — the signal to treat it as not party food
	 */
	public static int appealPerServe(RegistryAccess access, Level level, BlockPos pos, BlockState state) {
		var totals = totals(access, level, pos, state);
		return totals[0] <= 0 ? 0 : totals[1] / totals[0];
	}

	/** {@code {serves, appeal × serves, hunger × serves}}, weighted because a guest eats them all. */
	private static int[] totals(RegistryAccess access, Level level, BlockPos pos, BlockState state) {
		var override = PartyFood.of(access, state);
		int serves = 0, appeal = 0, hunger = 0;
		for (var result : results(level, pos, state)) {
			if (!result.valid()) continue;
			serves += result.serves();
			// on a container the override revalues the whole block: there is no finer key
			int per = override != null ? clamp(override.appeal()) : result.appeal();
			appeal += per * result.serves();
			// nutrition is literally the hunger bar: a cutting board's pair of
			// sushi counts twice here, which the appeal above is careful not to do
			hunger += result.nutrition() * result.serves();
		}
		return new int[]{serves, appeal, hunger};
	}

	/**
	 * The quantity the party system <b>plans</b> in, deliberately not the appeal. Never reads the
	 * {@code party_food} override — letting it reach hunger would let a datapack silently change how
	 * many guests a spread seats.
	 *
	 * @return 0 when the block is not valid food
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
	 * Takes one serving, keeping the world in step. Read {@link #appealPerServe} <i>before</i>
	 * calling — consuming changes the state the appeal was derived from.
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
