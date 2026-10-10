package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodValue;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared reading of a serving's {@link FoodProperties}.
 * <p>
 * Every block food whose food lives on an item — feasts, pies, pots, saucer dishes —
 * does the same three lines with it, so it lives here rather than in each handler.
 */
public final class Servings {

	/**
	 * Poison or wither on a serving: the same two {@code FeedModule} refuses.
	 */
	public static boolean refuses(FoodProperties props) {
		for (var effect : props.effects()) {
			var holder = effect.effect().getEffect();
			if (holder == null) continue;
			if (holder.is(net.minecraft.world.effect.MobEffects.POISON)
					|| holder.is(net.minecraft.world.effect.MobEffects.WITHER)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * One serving's food properties, or null when there is nothing worth serving.
	 * <p>
	 * <b>Food with no hunger value is not food.</b> A block item can carry
	 * {@link FoodProperties} while being worth nothing to eat — a decorative item, a
	 * husk, a mod's placeholder. Such a serving is reported as absent rather than given
	 * a nominal score, because a placeholder scoring the minimum would still count
	 * towards a party's budget and still be walked to and eaten by a guest, costing a
	 * guest's whole visit for nothing.
	 */
	@Nullable
	public static FoodProperties of(ItemStack stack) {
		if (stack.isEmpty()) return null;
		var props = stack.getFoodProperties(null);
		if (props == null || props.nutrition() <= 0 || refuses(props)) return null;
		return props;
	}

	/**
	 * The properties of {@code count} of {@code stack} eaten as <b>one plate</b>.
	 *
	 * <p>
	 * Not the same thing as {@code count} servings, and the party system needs it to be
	 * exact: a guest cannot eat one of the two nigiri off a board and leave the other,
	 * because the board hands the stack over whole. That makes the pair one dish twice
	 * the size, not two dishes — and {@link PartyFoodValue} prices a dish as
	 * {@code serves × perServeAppeal}, so the count has to move into the properties or
	 * the board is valued as a single piece of sushi.
	 *
	 * <p>
	 * Nutrition, saturation and eating time all multiply, because a guest really does sit
	 * through both. An effect does not: it is rolled once per item eaten, so
	 * {@code n} chances of {@code p} are {@code 1 − (1 − p)ⁿ}, which is the only reading
	 * of "doubles the odds" that is actually true. {@code canAlwaysEat} and the container
	 * are properties of the item rather than of the portion, and pass through.
	 *
	 * <p>
	 * A count of one returns the item's own properties unchanged rather than a rebuilt
	 * copy of them, because that is the overwhelmingly common case and it is the case
	 * where a copy would be pure waste.
	 */
	@Nullable
	public static FoodProperties of(ItemStack stack, int count) {
		var one = of(stack);
		if (one == null || count <= 1) return one;
		return plate(one, count);
	}

	private static FoodProperties plate(FoodProperties props, int count) {
		var list = new ArrayList<FoodProperties.PossibleEffect>();
		for (int i = 0; i < count; i++)
			list.addAll(props.effects());
		return new FoodProperties(props.nutrition() * count, props.saturation() * count,
				props.canAlwaysEat(), props.eatSeconds() * count, props.usingConvertsTo(), list);
	}

	private Servings() {
	}
}