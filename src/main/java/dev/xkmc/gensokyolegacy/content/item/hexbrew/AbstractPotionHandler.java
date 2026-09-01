package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface AbstractPotionHandler extends HexBrewHandler {

	default void applyEffects(LivingEntity user, ItemStack stack) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (contents == null) return;
		for (MobEffectInstance e : contents.getAllEffects()) {
			user.addEffect(new MobEffectInstance(e));
		}
	}

	default MobEffectCategory getCategory(ItemStack stack) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (contents == null) return MobEffectCategory.NEUTRAL;
		boolean positive = false, negative = false;
		for (MobEffectInstance e : contents.getAllEffects()) {
			var c = e.getEffect().value().getCategory();
			if (c == MobEffectCategory.BENEFICIAL) positive = true;
			if (c == MobEffectCategory.HARMFUL) negative = true;
		}
		return positive & !negative ? MobEffectCategory.BENEFICIAL :
				negative & !positive ? MobEffectCategory.HARMFUL :
				MobEffectCategory.NEUTRAL;
	}


	default int getColor(ItemStack stack) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (contents == null) return -1;
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
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, en -> true)) {
			if (e == thrower) continue;
			if (thrower != null) {
				var ally = e.isAlliedTo(thrower);
				if (taste == MobEffectCategory.HARMFUL && ally)
					continue;
			}
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
