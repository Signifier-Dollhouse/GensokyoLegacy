package dev.xkmc.gensokyolegacy.content.party.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How one kind of block food is read and eaten.
 * <p>
 * Block foods are not uniform, and the party system has to treat them in whole
 * <i>serves</i> rather than whole blocks. A vanilla cake is seven servings, a
 * Farmer's Delight feast is six and hands out a separate serving item when
 * clicked, a Youkai's Feasts pot carries two or four servings under its own
 * property, and a YSF saucer is one serving that may not track how much of it is
 * left. So a handler answers three questions about a <b>placed</b> block:
 *
 * <ol>
 *   <li>{@link #serves} — how many servings are left right now;</li>
 *   <li>{@link #food} — what one of those servings actually is, as a
 *       {@link FoodProperties}, either carried by the block itself or reached
 *       through a serving item;</li>
 *   <li>{@link #consume} — take one serving, leaving the block as it now
 *       stands.</li>
 * </ol>
 *
 * Appeal (§5.1) is then derived per serving from {@link #food} rather than
 * assigned per block, which is what lets one cake feed several guests and a
 * picked-over plate be worth less than a fresh one without any of that being
 * special-cased anywhere.
 * <p>
 * <b>Why the food properties are needed at all:</b> they are the appeal's
 * source. Nutrition, saturation and beneficial effects give the per-serving
 * value, and poison or wither make a serving worth nothing.
 *
 * @see PartyFoodHandler for how these are attached to blocks
 */
public interface BlockFoodHandler {

	/**
	 * How many servings are left in this placed block. Never below 0; a block that
	 * is gone or finished reports 0.
	 */
	int serves(BlockState state);

	/**
	 * What one serving of this food is, or null when it is not worth serving
	 * anything — no food properties at all, or something with poison or wither on
	 * it, which {@link PartyFoodValue} scores at 0 anyway.
	 */
	@Nullable
	FoodProperties food(BlockState state);

	/**
	 * Take one serving from the placed block.
	 * <p>
	 * On return the block has been advanced to its next state in the world: a
	 * countdown block steps its property down, a whole plate is removed. The
	 * caller is expected to have already read {@link #food} and
	 * {@link #serves} beforehand, since the appeal of the serving it just ate
	 * depends on the state <i>before</i> this call.
	 *
	 * @return how many servings remain afterwards
	 */
	int consume(Level level, BlockPos pos, BlockState state);

	// ------------------------------------------------------------------
	// The three shapes of block food, and the reasons each exists.
	// ------------------------------------------------------------------

	/**
	 * A block whose servings count down through an {@link IntegerProperty}:
	 * vanilla cake (7 slices, {@code bites}), Farmer's Delight's feasts (6
	 * servings, {@code servings}) and pies (4 bites, {@code bites}), and YSF's
	 * pots ({@code serve}, either 2 or 4 depending on the pot). Consuming steps
	 * the property down and removes the block on the last serve.
	 * <p>
	 * The property is found by name rather than passed in, because the name is
	 * the only thing these blocks agree on — the actual
	 * {@code IntegerProperty} instances belong to the other mods and cannot be
	 * named here without a hard dependency.
	 * <p>
	 * Handles both counting conventions these blocks use: a property that starts at
	 * zero counts bites <i>taken</i> (vanilla cake, FD pies), one that starts at its
	 * maximum counts servings <i>left</i> (FD feasts, YSF pots). See
	 * {@link #countsDown}.
	 *
	 * @param servingItemId id of the item carrying the food properties, or null
	 *                      to read them off the block's own item. Farmer's
	 *                      Delight's block items carry none at all, which is why
	 *                      this is needed.
	 */
	static BlockFoodHandler countdown(@Nullable String servingItemId) {
		var serving = parse(servingItemId);
		return new BlockFoodHandler() {
			@Override
			public int serves(BlockState state) {
				return servesOf(state);
			}

			@Override
			public @Nullable FoodProperties food(BlockState state) {
				var props = propertiesOf(serving, state);
				return props == null || refuses(props) ? null : props;
			}

			@Override
			public int consume(Level level, BlockPos pos, BlockState state) {
				stepDown(level, pos, state);
				return 0;   // the caller re-reads the state for the true count
			}
		};
	}

	/**
	 * A plate eaten all at once, valued as a single serving however it is cut:
	 * YSF saucer foods, which carry their food on a separate item and do not
	 * track what is left on the block. Consuming removes it.
	 *
	 * @param servingItemId id of the item carrying the food properties
	 */
	static BlockFoodHandler whole(@Nullable String servingItemId) {
		var serving = parse(servingItemId);
		return new BlockFoodHandler() {
			@Override
			public int serves(BlockState state) {
				return 1;
			}

			@Override
			public @Nullable FoodProperties food(BlockState state) {
				var props = propertiesOf(serving, state);
				return props == null || refuses(props) ? null : props;
			}

			@Override
			public int consume(Level level, BlockPos pos, BlockState state) {
				level.destroyBlock(pos, false);
				return 0;
			}
		};
	}

	/**
	 * A block whose food is described only inside its own use-handling code,
	 * with no {@link FoodProperties} on any stack. Vanilla cake is the case that
	 * matters: {@code CakeBlock.eat} calls {@code foodData.eat(2, 0.1F)} per
	 * slice and its block item carries no food at all, so reading the item would
	 * find nothing.
	 * <p>
	 * The servings still count down through the block's own {@code bites}
	 * property, so a cake loses a slice at a time.
	 *
	 * @param perServeNutrition  nutrition of one slice
	 * @param perServeSaturation saturation of one slice, as
	 *                           {@code FoodConstants.saturationByModifier}
	 *                           computes it ({@code foodLevel * mod * 2})
	 */
	static BlockFoodHandler slice(int perServeNutrition, float perServeSaturation) {
		var props = new FoodProperties.Builder()
				.nutrition(perServeNutrition)
				.saturationModifier(perServeSaturation / (perServeNutrition * 2f))
				.build();
		return new BlockFoodHandler() {
			@Override
			public int serves(BlockState state) {
				return servesOf(state);
			}

			@Override
			public @Nullable FoodProperties food(BlockState state) {
				return props;
			}

			@Override
			public int consume(Level level, BlockPos pos, BlockState state) {
				stepDown(level, pos, state);
				return 0;
			}
		};
	}

	/**
	 * Step a countdown block to its next state, or remove it on the last serve.
	 * <p>
	 * The step direction is read from the block rather than assumed, because these
	 * blocks do not agree: vanilla cake and FD pies count {@code bites} <b>up</b>
	 * from 0, while FD feasts count {@code servings} <b>down</b> from their max. See
	 * {@link #countsDown} — getting this backwards would eat a feast's servings
	 * upwards and never finish it.
	 */
	static void stepDown(Level level, BlockPos pos, BlockState state) {
		var left = countdownProperty(state);
		if (left == null) {
			level.destroyBlock(pos, false);
			return;
		}
		int value = state.getValue(left);
		int next = countsDown(state, left) ? value - 1 : value + 1;
		if (next < left.getPossibleValues().stream().mapToInt(Integer::intValue).min().orElse(0)
				|| next > left.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0)) {
			level.destroyBlock(pos, false);
			return;
		}
		level.setBlock(pos, state.setValue(left, next), 3);
	}

	/**
	 * Whether this block's remaining-serve property counts down from its maximum as
	 * the food is eaten, rather than up from zero.
	 * <p>
	 * Decided from the property's own range rather than from a hardcoded list: a
	 * property whose <b>default</b> value is its maximum is a "servings left" counter
	 * (FD feast, YSF pot), and one whose default is its minimum is a "bites taken"
	 * counter (vanilla cake, FD pie). This is what keeps the two conventions apart
	 * without naming any foreign property.
	 */
	private static boolean countsDown(BlockState state, IntegerProperty property) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (var value : property.getPossibleValues()) {
			min = Math.min(min, value);
			max = Math.max(max, value);
		}
		int fresh = state.getBlock().defaultBlockState().getValue(property);
		return fresh >= max;
	}

	/** Poison or wither on a serving: the same two FeedModule refuses outright. */
	static boolean refuses(FoodProperties props) {
		for (var e : props.effects()) {
			var holder = e.effect().getEffect();
			if (holder == null) continue;
			if (holder.is(net.minecraft.world.effect.MobEffects.POISON)
					|| holder.is(net.minecraft.world.effect.MobEffects.WITHER)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * How many servings are left, honouring either counting convention.
	 * <p>
	 * A counter that starts at zero counts <i>bites taken</i>, so it is one plus its
	 * value; one that starts at its maximum counts <i>servings left</i>, so it is the
	 * value itself. No property means one indivisible serve.
	 */
	static int servesOf(BlockState state) {
		var left = countdownProperty(state);
		if (left == null) return 1;
		int value = state.getValue(left);
		return countsDown(state, left) ? value : value + 1;
	}

	/**
	 * The block's own remaining-count property, or null when it has none.
	 * <p>
	 * Matched on the names these blocks actually use: {@code bites} (vanilla
	 * cake, FD pies), {@code servings} (FD feasts), {@code serve} (YSF pots).
	 */
	@Nullable
	static IntegerProperty countdownProperty(BlockState state) {
		for (var property : state.getProperties()) {
			if (!(property instanceof IntegerProperty count)) continue;
			var name = count.getName();
			if (name.equals("bites") || name.equals("servings") || name.equals("serve")) {
				return count;
			}
		}
		return null;
	}

	@Nullable
	private static ResourceLocation parse(@Nullable String id) {
		return id == null ? null : ResourceLocation.tryParse(id);
	}

	/**
	 * The food properties of one serving: from the serving item when the block
	 * itself carries none, otherwise from the block's own item.
	 */
	@Nullable
	private static FoodProperties propertiesOf(@Nullable ResourceLocation serving, BlockState state) {
		if (serving != null) {
			var item = BuiltInRegistries.ITEM.get(serving);
			if (item != net.minecraft.world.item.Items.AIR) {
				var props = new ItemStack(item).getFoodProperties(null);
				if (props != null) return props;
			}
		}
		Block block = state.getBlock();
		return new ItemStack(block).getFoodProperties(null);
	}
}