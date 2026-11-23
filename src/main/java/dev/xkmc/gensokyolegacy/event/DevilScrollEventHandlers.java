package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.danmakuapi.content.entity.DanmakuLaserEntity;
import dev.xkmc.gensokyolegacy.content.item.devil.DevilScrollItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
	private static final double KNOCK_SPEED = 2.0;

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
			launch(event, target, 0);
		}
	}

	/**
	 * Dash hits knock the target along the dash direction instead of away from the player.
	 */
	public static void applyKnockback(LivingEntity target, Vec3 dir, double speed) {
		if (target instanceof Player player && DevilScrollItem.isSuperhuman(player)) return;
		double len = dir.length();
		setMotion(target, len > 1e-6 ? dir.scale(speed / len) : Vec3.ZERO);
	}

	/**
	 * Every variant overwrites the whole motion: vanilla halves the target's velocity and caps the
	 * on-ground lift, so the target's previous speed is ignored.
	 */
	private static void launch(LivingKnockBackEvent event, LivingEntity target, double y) {
		event.setCanceled(true);
		double x = event.getRatioX();
		double z = event.getRatioZ();
		double len = Math.sqrt(x * x + z * z);
		if (len > 1e-5) {
			x = -x / len * KNOCK_SPEED;
			z = -z / len * KNOCK_SPEED;
		} else {
			x = 0;
			z = 0;
		}
		setMotion(target, new Vec3(x, y, z));
	}

	private static void setMotion(LivingEntity target, Vec3 v) {
		target.setDeltaMovement(v);
		target.hasImpulse = true;
		if (target instanceof Player player) player.hurtMarked = true;
	}

}
