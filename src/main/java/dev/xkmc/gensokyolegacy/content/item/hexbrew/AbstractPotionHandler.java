package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import dev.xkmc.gensokyolegacy.util.PotionData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface AbstractPotionHandler extends HexBrewHandler {

	default PotionData potion(ItemStack stack) {
		PotionData data = PotionData.read(stack);
		return data.isEmpty() ? defaultPotion() : data;
	}

	default void applyEffects(LivingEntity user, ItemStack stack) {
		for (MobEffectInstance e : potion(stack).getAllEffects()) {
			user.addEffect(new MobEffectInstance(e));
		}
	}

	default MobEffectCategory getCategory(ItemStack stack) {
		boolean positive = false, negative = false;
		for (MobEffectInstance e : potion(stack).getAllEffects()) {
			var c = e.getEffect().getCategory();
			if (c == MobEffectCategory.BENEFICIAL) positive = true;
			if (c == MobEffectCategory.HARMFUL) negative = true;
		}
		return positive & !negative ? MobEffectCategory.BENEFICIAL :
				negative & !positive ? MobEffectCategory.HARMFUL :
						MobEffectCategory.NEUTRAL;
	}


	default int getColor(ItemStack stack) {
		var contents = potion(stack);
		if (contents.isEmpty()) return -1;
		return contents.getColor();
	}


	default float radius(ItemStack stack) {
		return 4;
	}

	@Override
	default void onHit(Level level, Vec3 pos, @Nullable Entity thrower, ItemStack stack) {
		if (level.isClientSide) return;
		float r = radius(stack);
		AABB box = new AABB(pos, pos).inflate(r);
		var taste = getCategory(stack);
		Entity credit = HexBrewHandler.damageOwner(thrower);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, en -> true)) {
			var ally = e == thrower || e == credit ||
					HexBrewHandler.isAllied(e, thrower) || HexBrewHandler.isAllied(e, credit);
			if (taste == MobEffectCategory.HARMFUL && ally)
				continue;
			if (e.distanceToSqr(pos) > r * r) continue;
			applyEffects(e, stack);
		}
		level.levelEvent(2002, BlockPos.containing(pos), getColor(stack));
	}

	@Override
	default void onDrink(LivingEntity user, ItemStack stack, Level level) {
		if (level.isClientSide) return;
		applyEffects(user, stack);
	}

}
