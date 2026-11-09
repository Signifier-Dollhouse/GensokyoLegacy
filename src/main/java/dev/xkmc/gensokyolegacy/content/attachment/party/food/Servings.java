package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Shared reading of a serving's {@link FoodProperties}.
 *
 * @see "doc/design/party/food.md"
 */
public final class Servings {

	/**
	 * Poison or wither on a serving: the same two {@code FeedModule} refuses.
	 */
	public static boolean refuses(FoodProperties props) {
		for (var effect : props.getEffects()) {
			var holder = effect.getFirst().getEffect();
			if (holder == null) continue;
			if (holder == MobEffects.POISON || holder == MobEffects.WITHER) {
				return true;
			}
		}
		return false;
	}

	/**
	 * One serving's food properties, or null when there is nothing worth serving. Food with no hunger value is not
	 * food.
	 */
	@Nullable
	public static FoodProperties of(ItemStack stack) {
		if (stack.isEmpty()) return null;
		var props = stack.getFoodProperties(null);
		if (props == null || props.getNutrition() <= 0 || refuses(props)) return null;
		return props;
	}

	/**
	 * The properties of {@code count} of {@code stack} eaten as <b>one plate</b>, not as {@code count}
	 * servings.
	 * <p>Nutrition, saturation and eating time multiply; an effect does not, because it is rolled once
	 * per item eaten, so {@code n} chances of {@code p} are {@code 1 − (1 − p)ⁿ}. {@link FoodResult#appeal()}
	 * deliberately does not scale: a pair feeds a guest twice and attracts once.
	 */
	@Nullable
	public static FoodProperties of(ItemStack stack, int count) {
		var one = of(stack);
		if (one == null || count <= 1) return one;
		return plate(one, count);
	}

	private static FoodProperties plate(FoodProperties props, int count) {
		var builder = new FoodProperties.Builder()
				.nutrition(props.getNutrition() * count)
				.saturationMod(props.getSaturationModifier());
		if (props.isMeat()) builder.meat();
		if (props.canAlwaysEat()) builder.alwaysEat();
		if (props.isFastFood()) builder.fast();
		for (int i = 0; i < count; i++)
			for (var pair : props.getEffects())
				builder.effect(new MobEffectInstance(pair.getFirst()), pair.getSecond());
		return builder.build();
	}

	private Servings() {
	}
}