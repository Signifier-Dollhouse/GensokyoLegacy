package dev.xkmc.gensokyolegacy.content.party.core;

import dev.xkmc.gensokyolegacy.content.party.core.food.FoodResult;
import dev.xkmc.gensokyolegacy.content.party.core.food.FoodResultCache;
import dev.xkmc.gensokyolegacy.content.party.core.food.Servings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
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
 * The {@code party_food} data map is an <b>override only</b>: an entry replaces
 * the whole calculation outright, and a block with no entry is scored from its
 * handler. A datapack can therefore make a foreign block party food with a
 * single tag entry and still get a sensible number, and the data map is only
 * needed to correct a value the heuristic gets wrong.
 */
public class PartyFoodValue {

	/** Nutrition and saturation are worth this much appeal per point. */
	private static final float NUTRITION_SCALE = 0.5f;
	private static final float SATURATION_SCALE = 2.0f;
	/** A beneficial effect is worth a flat bonus per serving. */
	private static final int BENEFICIAL_BONUS = 4;

	/** The floor for anything that <i>is</i> food; invalid food scores 0 instead. */
	public static final int MIN_APPEAL = 1;
	public static final int MAX_APPEAL = 40;

	/**
	 * Appeal of one serving, from its food properties. Nutrition and saturation
	 * carry the value, beneficial effects push it up, and poison or wither makes
	 * it unservable.
	 *
	 * @return the appeal of one serving, or 0 for something not worth serving
	 */
	public static int of(FoodProperties props) {
		if (props.nutrition() <= 0 || Servings.refuses(props)) return 0;
		float bonus = 0;
		for (var e : props.effects()) {
			var holder = e.effect().getEffect();
			if (holder != null && holder.value().isBeneficial()) bonus += BENEFICIAL_BONUS;
		}
		return clamp(Math.round(props.nutrition() * NUTRITION_SCALE + props.saturation() * SATURATION_SCALE + bonus));
	}

	/**
	 * The same reading, from a stack. A stack with no hunger value is worth nothing, not
	 * the minimum — see {@link Servings#of}.
	 */
	public static int of(ItemStack stack) {
		return Servings.of(stack) == null ? 0 : of(stack);
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
	 * Appeal of one serving of this placed block, honouring its handler and then
	 * any override.
	 *
	 * @return the appeal of one serving, or 0 when the block is not valid food. A 0 is
	 *         the signal that the block should be treated as not party food at all:
	 *         it is neither counted towards the budget nor walked to and eaten.
	 */
	public static int perServe(RegistryAccess access, BlockState state) {
		var override = PartyFood.of(access, state);
		if (override != null) return override.appeal();
		var result = result(state);
		return result.valid() ? of(result.food()) : 0;
	}

	/**
	 * Total appeal of this placed block — every serving still on it.
	 *
	 * @return the appeal, or 0 for something not worth serving
	 */
	public static int appeal(RegistryAccess access, BlockState state) {
		var override = PartyFood.of(access, state);
		if (override != null) return override.appeal() * result(state).serves();
		var result = result(state);
		return result.valid() ? of(result.food()) * result.serves() : 0;
	}

	/** How many servings are left on this placed block; 0 when it is not valid food. */
	public static int servesOf(BlockState state) {
		return result(state).serves();
	}

	/**
	 * Take one serving from a placed block, keeping the world's own block state in
	 * step. The caller must read {@link #perServe} <i>before</i> calling, since
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