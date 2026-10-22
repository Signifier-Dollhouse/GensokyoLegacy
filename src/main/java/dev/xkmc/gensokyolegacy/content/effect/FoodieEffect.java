package dev.xkmc.gensokyolegacy.content.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/**
 * Once a second, turns one point of hunger into one point of saturation while the player has more
 * than 16 hunger and room for the saturation. Eating speed is a separate use-tick handler.
 *
 * @see "doc/design/doll/stray.md"
 */
public class FoodieEffect extends EmptyEffect {

	public FoodieEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (!(entity instanceof Player player) || player.level().isClientSide()) return true;
		if (player.tickCount % 20 != 0) return true;
		FoodData food = player.getFoodData();
		int level = food.getFoodLevel();
		float saturation = food.getSaturationLevel();
		if (level <= 16 || saturation >= level) return true;
		int nextLevel = level - 1;
		float nextSaturation = Math.min(saturation + 1, nextLevel);
		if (nextSaturation <= saturation) return true;
		food.setFoodLevel(nextLevel);
		food.setSaturation(nextSaturation);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

}
