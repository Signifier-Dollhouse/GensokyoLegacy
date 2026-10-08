package dev.xkmc.gensokyolegacy.content.party.core.food;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Shared reading of a serving's {@link FoodProperties}.
 * <p>
 * Every block food whose food lives on an item — feasts, pies, pots, saucer dishes —
 * does the same three lines with it, so it lives here rather than in each handler.
 */
public final class Servings {

	/** Poison or wither on a serving: the same two {@code FeedModule} refuses. */
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

	private Servings() {
	}
}