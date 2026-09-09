package dev.xkmc.gensokyolegacy.util;

import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class LavaEffectsHelper {

	public static boolean lavaVision(LivingEntity le) {
		return le.hasEffect(GLEffects.LAVA_AFFINITY);
	}

	public static float lavaSwim(LivingEntity le, double v) {
		if (le.hasEffect(GLEffects.LAVA_AFFINITY)) {
			return (float) (v * 2);
		}
		return (float) v;
	}

	public static boolean canLavaSwim(LivingEntity le) {
		return le.hasEffect(GLEffects.LAVA_AFFINITY) ||
				le.hasEffect(MobEffects.FIRE_RESISTANCE);
	}

	public static boolean noFire(LivingEntity le) {
		return le.hasEffect(GLEffects.LAVA_AFFINITY) ||
				le.hasEffect(MobEffects.FIRE_RESISTANCE);
	}

	public static boolean fireImmune(Entity entity, DamageSource source) {
		if (entity instanceof LivingEntity le && source.is(DamageTypeTags.IS_FIRE)) {
			return noFire(le);
		}
		return false;
	}

}
