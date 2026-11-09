package dev.xkmc.gensokyolegacy.content.effect;

import dev.xkmc.gensokyolegacy.compat.l2lib.LegacyHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * An effect that answers every hit on its holder with danmaku owned by whoever inflicted the
 * effect. The owner is kept in persistent data, so a hit can arrive long after the effect was
 * applied.
 */
public abstract class DanmakuRetaliateEffect extends EmptyEffect {

	private final String ownerKey;
	private final String cooldownKey;
	private final int cooldown;

	protected DanmakuRetaliateEffect(MobEffectCategory category, int color, String id, int cooldown) {
		super(category, color);
		this.ownerKey = id + "_owner";
		this.cooldownKey = id + "_cooldown";
		this.cooldown = cooldown;
	}

	public static void apply(LegacyHolder<MobEffect> holder, LivingEntity target, UUID owner, int duration) {
		target.addEffect(new MobEffectInstance(holder.get(), duration, 0));
		if (holder.get() instanceof DanmakuRetaliateEffect effect)
			target.getPersistentData().putUUID(effect.ownerKey, owner);
	}

	public static void onLivingHurt(LivingEntity victim, LegacyHolder<MobEffect> holder) {
		MobEffectInstance instance = victim.getEffect(holder.get());
		if (instance != null && instance.getEffect() instanceof DanmakuRetaliateEffect effect)
			effect.retaliate(victim);
	}

	private void retaliate(LivingEntity victim) {
		if (!(victim.level() instanceof ServerLevel level)) return;
		CompoundTag data = victim.getPersistentData();
		long now = level.getGameTime();
		if (now - data.getLong(cooldownKey) < cooldown) return;
		if (!data.hasUUID(ownerKey)) return;
		Entity owner = level.getEntity(data.getUUID(ownerKey));
		if (!(owner instanceof LivingEntity living)) return;
		data.putLong(cooldownKey, now);
		spawnDanmaku(level, living, victim);
	}

	protected abstract void spawnDanmaku(ServerLevel level, LivingEntity owner, LivingEntity victim);

}
