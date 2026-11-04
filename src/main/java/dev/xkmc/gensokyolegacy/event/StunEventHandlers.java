package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Cataclysm-style stun lock: no attacking, jumping, item use or block interaction. Movement speed
 * is reduced by {@code StunEffect} itself.
 */
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class StunEventHandlers {

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onAttackEntity(AttackEntityEvent event) {
		if (event.getEntity().hasEffect(GLEffects.STUN)) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onJump(LivingEvent.LivingJumpEvent event) {
		LivingEntity entity = event.getEntity();
		if (!entity.hasEffect(GLEffects.STUN)) return;
		Vec3 vec = entity.getDeltaMovement();
		entity.setDeltaMovement(vec.x, 0, vec.z);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
		if (event.getEntity().hasEffect(GLEffects.STUN)) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
		if (event.getEntity().hasEffect(GLEffects.STUN)) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		if (event.getEntity().hasEffect(GLEffects.STUN)) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.getEntity().hasEffect(GLEffects.STUN)) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (event.getEntity().hasEffect(GLEffects.STUN)) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (event.getEntity().hasEffect(GLEffects.STUN)) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		if (event.getEntity().hasEffect(GLEffects.STUN)) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onBreakBlock(BlockEvent.BreakEvent event) {
		Player player = event.getPlayer();
		if (player.hasEffect(GLEffects.STUN)) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
		if (event.getEntity() instanceof LivingEntity le && le.hasEffect(GLEffects.STUN))
			event.setCanceled(true);
	}

}
