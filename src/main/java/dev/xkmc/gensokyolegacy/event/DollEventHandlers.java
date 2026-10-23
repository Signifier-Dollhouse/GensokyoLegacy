package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.danmakuapi.api.DanmakuDamageEvent;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageRefactorEntity.CombatData;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItem;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItemData;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.content.item.glove.network.DollGloveSwingPacket;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class DollEventHandlers {

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer sp) {
			GLMeta.DOLL.type().getOrCreate(sp).restore(sp);
		}
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer sp) {
			GLMeta.DOLL.type().getOrCreate(sp).onLogout(sp);
		}
	}

	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer sp) {
			GLMeta.DOLL.type().getOrCreate(sp).onPlayerDeath(sp);
		}
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer sp) {
			GLMeta.DOLL.type().getOrCreate(sp).restore(sp);
		}
	}

	/**
	 * A doll's danmaku keeps the doll as the projectile owner (for friendly-fire filtering) but
	 * credits the owner as the causing entity: the projectile stays the direct source.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onDollDanmakuDamage(DanmakuDamageEvent event) {
		if (!(event.getUser() instanceof BaseDollEntity doll)) return;
		LivingEntity owner = doll.getOwner();
		if (owner == null || owner == doll) return;
		DamageSource old = event.getSource();
		event.setSource(new DamageSource(old.typeHolder(), old.getDirectEntity(), owner));
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		// the base, not the doll: pairing is every kind's, whatever it can do
		if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof BaseDollEntity doll) {
			doll.onJoinLevelCheck();
		}
	}

	/**
	 * Anvil repair for dolls: a damaged doll item plus any wool restores it to full health for 1 wool and a flat level
	 * cost. Vanilla repair logic can't apply — doll health lives in the {@code DOLL_DATA} component, not the vanilla
	 * damage value — so the output is set directly.
	 */
	@SubscribeEvent
	public static void onAnvilRepair(AnvilUpdateEvent event) {
		ItemStack left = event.getLeft();
		ItemStack right = event.getRight();
		if (!(left.getItem() instanceof DollItem)) return;
		if (right.isEmpty() || !right.is(ItemTags.WOOL)) return;
		DollItemData data = left.get(GLItems.DOLL_DATA.get());
		float cur = data == null ? BaseDollEntity.DEFAULT_MAX_HEALTH : data.combat().amount();
		if (cur >= BaseDollEntity.DEFAULT_MAX_HEALTH) return;
		ItemStack out = left.copy();
		out.set(GLItems.DOLL_DATA.get(), new DollItemData(
				new CombatData(BaseDollEntity.DEFAULT_MAX_HEALTH, 0)));
		out.set(DataComponents.REPAIR_COST, left.getOrDefault(DataComponents.REPAIR_COST, 0) + 1);
		event.setOutput(out);
		event.setCost(1);
		event.setMaterialCost(1);
	}

	@SubscribeEvent
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		Player player = event.getEntity();
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof DollGloveItem glove)) return;
		if (!DollGloveItem.getMode(stack).isAttackCommand()) return;
		event.setCanceled(true);
		if (player instanceof ServerPlayer sp && !sp.getCooldowns().isOnCooldown(glove)) {
			DollGloveItem.getMode(stack).performAttack(sp, InteractionHand.MAIN_HAND, stack, glove);
		}
	}

	@SubscribeEvent
	public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
		Player player = event.getEntity();
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof DollGloveItem glove)) return;
		if (!DollGloveItem.getMode(stack).isAttackCommand()) return;
		if (player.getCooldowns().isOnCooldown(glove)) return;
		GensokyoLegacy.HANDLER.toServer(new DollGloveSwingPacket());
	}


}