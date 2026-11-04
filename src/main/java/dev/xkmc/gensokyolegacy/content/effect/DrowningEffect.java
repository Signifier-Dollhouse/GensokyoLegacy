package dev.xkmc.gensokyolegacy.content.effect;

import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorBulletEntity;
import dev.xkmc.gensokyolegacy.init.data.GLDamageTypes;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Extra drowning damage is applied by {@code MobEffectEventHandlers}; this effect only sinks its
 * holder while it is inside any fluid.
 */
public class DrowningEffect extends MobEffect {

	private static final String DROWNING_TICK = "gensokyolegacy:drowning_tick";
	private static final int DROWNING_TICKS = 400;

	public DrowningEffect() {
		super(MobEffectCategory.HARMFUL, 0xFF1E90FF);
	}

	/**
	 * 20% per level of the health the hit actually removed. The source carries no entity, so the
	 * death message is the vanilla drowning one, and the damage type bypasses the hurt cooldown.
	 */
	public static void extraDamage(LivingEntity target, float damage) {
		MobEffectInstance ins = target.getEffect(GLEffects.DROWNING);
		if (ins == null) return;
		CompoundTag data = target.getPersistentData();
		if (data.contains(DROWNING_TICK) && data.getInt(DROWNING_TICK) == target.tickCount) return;
		data.putInt(DROWNING_TICK, target.tickCount);
		float extra = damage * 0.2F * (ins.getAmplifier() + 1);
		if (extra > 0) target.hurt(GLDamageTypes.drowning(target), extra);
	}

	/**
	 * The water anchor leaves level 2 behind on its direct and area hits, the droplets it sheds
	 * level 1.
	 */
	public static void onHit(LivingEntity target, DamageSource source) {
		if (source.getDirectEntity() instanceof AnchorBulletEntity) {
			apply(target, 1);
		} else if (source.getDirectEntity() instanceof ItemBulletEntity bullet &&
				bullet.getItem().is(GLItems.DROPLET)) {
			apply(target, 0);
		}
	}

	public static void apply(LivingEntity target, int amplifier) {
		target.addEffect(new MobEffectInstance(GLEffects.DROWNING, DROWNING_TICKS, amplifier));
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity.level().isClientSide) return true;
		if (entity.isInFluidType()) {
			Vec3 vec = entity.getDeltaMovement();
			entity.setDeltaMovement(vec.x, vec.y - 1, vec.z);
			entity.hurtMarked = true;
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

}
