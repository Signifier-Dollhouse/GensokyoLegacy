package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.effect.DrowningEffect;
import dev.xkmc.gensokyolegacy.content.effect.StunEffect;
import dev.xkmc.gensokyolegacy.content.entity.characters.maiden.MaidenEntity;
import dev.xkmc.gensokyolegacy.content.item.character.TouhouHatItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLDamageTypes;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.util.LavaEffectsHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class MobEffectEventHandlers {

	/** Foodie */
	@SubscribeEvent
	public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
		if (!(event.getEntity() instanceof Player player)) return;
		if (!player.hasEffect(GLEffects.FOODIE.get())) return;
		if (!event.getItem().getItem().isEdible()) return;
		int duration = event.getDuration();
		if (duration > 1) event.setDuration(duration - 1);
	}

	/** Drowning and Stun */
	@SubscribeEvent
	public static void onDamagePost(LivingDamageEvent event) {
		LivingEntity target = event.getEntity();
		DamageSource source = event.getSource();
		if (target.level().isClientSide) return;
		if (source.is(GLDamageTypes.DROWNING)) return;
		if (event.getAmount() <= 0) return;
		DrowningEffect.extraDamage(target, event.getAmount());
		if (!target.isAlive()) return;
		StunEffect.onHit(target, source);
		DrowningEffect.onHit(target, source);
	}

	/** Lava Affinity */
	@SubscribeEvent
	public static void immunityCheck(LivingAttackEvent event) {
		if (LavaEffectsHelper.fireImmune(event.getEntity(), event.getSource())) {
			event.setCanceled(true);
		}
	}

	/** Koishi */
	@SubscribeEvent
	public static void onAttack(LivingAttackEvent event) {
		if (event.getSource().getEntity() instanceof LivingEntity le) {
			if (le instanceof Player player) {
				removeKoishi(player);
			}
		}
	}

	/** Koishi */
	@SubscribeEvent
	public static void onTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		var e = event.player;
		if (e.getLastHurtMob() instanceof MaidenEntity ||
				e.getLastHurtByMob() instanceof MaidenEntity) {
			removeKoishi(e);
		}
	}

	public static void removeKoishi(LivingEntity le) {
		if (le instanceof Player player) {
			if (player.hasEffect(GLEffects.UNCONSCIOUS.get())) {
				player.removeEffect(GLEffects.UNCONSCIOUS.get());
			}
			var hat = GLItems.KOISHI_HAT.get();
			if (TouhouHatItem.getEquippedHats(player).stream().anyMatch(e -> e.is(hat))) {
				if (player.getCooldowns().getCooldownPercent(hat, 0) < 0.5)
					player.getCooldowns().addCooldown(hat, 200);
			}
		}
	}

}
