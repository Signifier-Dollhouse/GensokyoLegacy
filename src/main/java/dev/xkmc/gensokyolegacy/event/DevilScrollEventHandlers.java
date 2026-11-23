package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.danmakuapi.content.entity.DanmakuLaserEntity;
import dev.xkmc.gensokyolegacy.content.item.devil.DevilScrollItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * The scroll's three reactions: devil lasers on any non-laser damage, fall immunity, and the
 * superhuman knockback rules.
 *
 * @see "doc/design/item/devils_scroll.md"
 */
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class DevilScrollEventHandlers {

	private static final double LAUNCH_UP = 1.0;
	private static final double SLAM_DOWN = -1.5;
	private static final double KNOCK_HORIZONTAL = 0.4;
	private static final float GROUND_STRENGTH = 1.5F;

	@SubscribeEvent
	public static void onDamage(LivingDamageEvent.Post event) {
		if (event.getNewDamage() <= 0) return;
		DamageSource source = event.getSource();
		if (!(source.getEntity() instanceof ServerPlayer player)) return;
		// the scroll's own lasers are owned by the player: without this they would chain forever
		if (source.getDirectEntity() instanceof DanmakuLaserEntity) return;
		if (!DevilScrollItem.isDevil(player)) return;
		if (player.getCooldowns().isOnCooldown(GLItems.DEVILS_SCROLL.get())) return;
		player.getCooldowns().addCooldown(GLItems.DEVILS_SCROLL.get(), DevilScrollItem.LASER_COOLDOWN);
		DevilScrollItem.summonLasers(player, event.getEntity());
	}

	@SubscribeEvent
	public static void onFall(LivingFallEvent event) {
		if (event.getEntity() instanceof Player player && DevilScrollItem.isSuperhuman(player)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onKnockback(LivingKnockBackEvent event) {
		LivingEntity target = event.getEntity();
		if (target instanceof Player player && DevilScrollItem.isSuperhuman(player)) {
			event.setCanceled(true);
			return;
		}
		if (!(target.getLastHurtByMob() instanceof Player player)) return;
		if (!DevilScrollItem.isSuperhuman(player)) return;
		if (player.isShiftKeyDown()) {
			launch(event, target, LAUNCH_UP);
		} else if (!player.onGround()) {
			launch(event, target, SLAM_DOWN);
		} else {
			event.setStrength(Math.max(event.getStrength(), GROUND_STRENGTH));
		}
	}

	/**
	 * Cancelling is what lets the vertical variants work: vanilla caps the on-ground lift at 0.4 and
	 * halves the target's velocity, so the custom variants set the whole motion themselves.
	 */
	private static void launch(LivingKnockBackEvent event, LivingEntity target, double y) {
		event.setCanceled(true);
		double x = event.getRatioX();
		double z = event.getRatioZ();
		double len = Math.sqrt(x * x + z * z);
		if (len > 1e-5) {
			x = -x / len * KNOCK_HORIZONTAL;
			z = -z / len * KNOCK_HORIZONTAL;
		} else {
			x = 0;
			z = 0;
		}
		target.setDeltaMovement(x, y, z);
		target.hasImpulse = true;
		if (target instanceof Player player) player.hurtMarked = true;
	}

}
