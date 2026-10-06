package dev.xkmc.gensokyolegacy.content.party.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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

	public static final int MIN_APPEAL = 1;
	public static final int MAX_APPEAL = 40;

	/**
	 * Appeal of one serving, from its food properties. Nutrition and saturation
	 * carry the value, beneficial effects push it up, and poison or wither makes
	 * it unservable.
	 *
	 * @return the appeal of one serving, or 0 for something nobody would put out
	 */
	public static int of(FoodProperties props) {
		if (BlockFoodHandler.refuses(props)) return 0;
		float bonus = 0;
		for (var e : props.effects()) {
			var holder = e.effect().getEffect();
			if (holder != null && holder.value().isBeneficial()) bonus += BENEFICIAL_BONUS;
		}
		return clamp(Math.round(props.nutrition() * NUTRITION_SCALE + props.saturation() * SATURATION_SCALE + bonus));
	}

	/** The same reading, from a stack. */
	public static int of(ItemStack stack) {
		var props = stack.getFoodProperties(null);
		return props == null ? MIN_APPEAL : of(props);
	}

	/**
	 * Appeal of one serving of this placed block, honouring its handler and then
	 * any override.
	 *
	 * @return the appeal of one serving, or 0 for something not worth serving
	 */
	public static int perServe(RegistryAccess access, BlockState state) {
		var override = PartyFood.of(access, state);
		if (override != null) return override.appeal();
		var props = foodOf(state);
		return props == null ? MIN_APPEAL : of(props);
	}

	/**
	 * Total appeal of this placed block — every serving still on it.
	 *
	 * @return the appeal, or 0 for something not worth serving
	 */
	public static int appeal(RegistryAccess access, BlockState state) {
		var override = PartyFood.of(access, state);
		if (override != null) return override.appeal() * servesOf(state);
		var perServe = perServe(access, state);
		return perServe == 0 ? 0 : perServe * servesOf(state);
	}

	/** How many servings are left on this placed block. */
	public static int servesOf(BlockState state) {
		var handler = handlerOf(state.getBlock());
		return handler == null ? 1 : handler.serves(state);
	}

	/**
	 * Take one serving from a placed block, keeping the world's own block state
	 * in step. The caller must read {@link #perServe} <i>before</i> calling, since
	 * consuming changes the state the appeal is derived from.
	 *
	 * @return servings remaining afterwards
	 */
	public static int consume(ServerLevel level, BlockPos pos, BlockState state) {
		var handler = handlerOf(state.getBlock());
		if (handler == null) {
			level.destroyBlock(pos, false);
			return 0;
		}
		return handler.consume(level, pos, state);
	}

	/** One serving's food properties, or null when there is nothing to read. */
	@Nullable
	private static FoodProperties foodOf(BlockState state) {
		var handler = handlerOf(state.getBlock());
		if (handler != null) return handler.food(state);
		Block block = state.getBlock();
		return new ItemStack(block).getFoodProperties(null);
	}

	private static int clamp(int value) {
		if (value < MIN_APPEAL) return 0;
		return Math.min(value, MAX_APPEAL);
	}

	@Nullable
	private static BlockFoodHandler handlerOf(Block block) {
		// per-class, so a registered base covers its subclasses
		return PartyFoodHandler.of(block);
	}

	public static boolean isRegistered(Block block) {
		return PartyFoodHandler.isRegistered(block);
	}

	private PartyFoodValue() {
	}
}