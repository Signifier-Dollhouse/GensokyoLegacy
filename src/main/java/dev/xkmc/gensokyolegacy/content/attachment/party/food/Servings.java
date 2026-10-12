package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodValue;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared reading of a serving's {@link FoodProperties}.
 *
 * @see doc/design/party/food.md
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

	/** One serving's food properties, or null when there is nothing worth serving. Food with no hunger value is not food. */
	@Nullable
	public static FoodProperties of(ItemStack stack) {
		if (stack.isEmpty()) return null;
		var props = stack.getFoodProperties(null);
		if (props == null || props.nutrition() <= 0 || refuses(props)) return null;
		return props;
	}

	/**
	 * The properties of {@code count} of {@code stack} eaten as <b>one plate</b>, not as
	 * {@code count} servings.
	 * <p>Nutrition, saturation and eating time multiply; an effect does not, because it is
	 * rolled once per item eaten, so {@code n} chances of {@code p} are {@code 1 − (1 − p)ⁿ}.
	 * {@link FoodResult#appeal()} deliberately does not scale: a pair feeds a guest twice
	 * and attracts once.
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