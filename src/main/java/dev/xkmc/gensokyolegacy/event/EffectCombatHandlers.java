package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorBulletEntity;
import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorHookEntity;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLDamageTypes;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Combat effects shared by the anchor and the drowning effect: extra drowning damage on any hit,
 * plus the stun and drowning a specific anchor hit leaves behind.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class EffectCombatHandlers {

	private static final String DROWNING_TICK = "gensokyolegacy:drowning_tick";
	private static final int STUN_TICKS = 20;
	private static final int STUN_CHARGED_TICKS = 40;
	private static final int DROWNING_TICKS = 400;

	@SubscribeEvent
	public static void onDamagePost(LivingDamageEvent.Post event) {
		LivingEntity target = event.getEntity();
		DamageSource source = event.getSource();
		if (target.level().isClientSide) return;
		if (source.is(GLDamageTypes.DROWNING)) return;
		if (event.getNewDamage() <= 0) return;
		drowningDamage(target, event.getNewDamage());
		if (target.isAlive()) anchorEffects(target, source);
	}

	/**
	 * 20% per level of the health the hit actually removed. The source carries no entity, so the
	 * death message is the vanilla drowning one, and the damage type bypasses the hurt cooldown.
	 */
	private static void drowningDamage(LivingEntity target, float damage) {
		MobEffectInstance ins = target.getEffect(GLEffects.DROWNING);
		if (ins == null) return;
		CompoundTag data = target.getPersistentData();
		if (data.contains(DROWNING_TICK) && data.getInt(DROWNING_TICK) == target.tickCount) return;
		data.putInt(DROWNING_TICK, target.tickCount);
		float extra = damage * 0.2F * (ins.getAmplifier() + 1);
		if (extra > 0) target.hurt(GLDamageTypes.drowning(target), extra);
	}

	private static void anchorEffects(LivingEntity target, DamageSource source) {
		if (source.getDirectEntity() instanceof AnchorHookEntity hook) {
			switch (hook.throwType()) {
				case DESTROY -> stun(target, STUN_TICKS);
				case DESTROY_CHARGED -> stun(target, STUN_CHARGED_TICKS);
				default -> {
				}
			}
		} else if (source.getDirectEntity() instanceof AnchorBulletEntity) {
			stun(target, STUN_TICKS);
			drown(target, 1);
		} else if (source.getDirectEntity() instanceof ItemBulletEntity bullet &&
				bullet.getItem().is(GLItems.DROPLET)) {
			drown(target, 0);
		} else if (source.is(DamageTypes.PLAYER_ATTACK) &&
				source.getDirectEntity() instanceof Player player &&
				player.getMainHandItem().getItem() instanceof AnchorItem) {
			stun(target, STUN_TICKS);
		}
	}

	private static void stun(LivingEntity target, int ticks) {
		target.addEffect(new MobEffectInstance(GLEffects.STUN, ticks));
	}

	private static void drown(LivingEntity target, int amplifier) {
		target.addEffect(new MobEffectInstance(GLEffects.DROWNING, DROWNING_TICKS, amplifier));
	}

}
