package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.danmakuapi.api.DanmakuDamageEvent;
import dev.xkmc.gensokyolegacy.content.attachment.misc.FrogGodCapability;
import dev.xkmc.gensokyolegacy.content.attachment.storage.PendingItemStorage;
import dev.xkmc.gensokyolegacy.content.entity.characters.rumia.RumiaEntity;
import dev.xkmc.gensokyolegacy.content.entity.foundation.IDimensionBoundEntity;
import dev.xkmc.gensokyolegacy.content.item.character.TouhouHatItem;
import dev.xkmc.gensokyolegacy.content.item.hakkero.HakkeroMenu;
import dev.xkmc.gensokyolegacy.content.item.tool.CatBell;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaUnlock;
import dev.xkmc.gensokyolegacy.content.rpg.trigger.KillTrigger;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLDamageTypes;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class MiscEventHandlers {

	/** Umbrella craft */
	@SubscribeEvent
	public static void onAnvilUpdate(AnvilUpdateEvent e) {
		ItemStack left = e.getLeft();
		ItemStack right = e.getRight();
		if (!(left.getItem() instanceof BorderUmbrellaItem)) return;
		var unlock = GLItems.UMBRELLA_UNLOCK.getOrDefault(left, BorderUmbrellaUnlock.DEFAULT);
		if (right.is(Items.CHORUS_FRUIT)) {
			if (unlock.travelUnlocked()) return;
			ItemStack out = left.copy();
			GLItems.UMBRELLA_UNLOCK.set(out, unlock.withTravel(true));
			e.setOutput(out);
			e.setCost(5);
			e.setMaterialCost(1);
		} else if (right.is(Items.ECHO_SHARD)) {
			if (unlock.captureUnlocked()) return;
			ItemStack out = left.copy();
			GLItems.UMBRELLA_UNLOCK.set(out, unlock.withCapture(true));
			e.setOutput(out);
			e.setCost(10);
			e.setMaterialCost(1);
		}
	}

	/** Hakkero, Shelf storage*/
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		if (!(event.player instanceof ServerPlayer sp)) return;
		if (sp.containerMenu instanceof HakkeroMenu furnace) furnace.tick();
		if (sp.tickCount % 20 != 0) return;
		var storage = PendingItemStorage.get(sp.serverLevel());
		if (!storage.hasPending(sp.getUUID())) return;
		if (storage.deliver(sp) && !storage.hasPending(sp.getUUID()))
			sp.sendSystemMessage(PendingItemStorage.RETURN_MSG);
	}

	/** Koishi, Rumia*/
	@SubscribeEvent
	public static void onShieldBlock(ShieldBlockEvent event) {
		if (event.getDamageSource().is(GLDamageTypes.KOISHI)) {
			if (event.getEntity() instanceof Player player) {
				GLMeta.KOISHI_ATTACK.type().getOrCreate(player).onBlock(player);
			}
		}
		if (event.getDamageSource().getDirectEntity() instanceof RumiaEntity rumia) {
			rumia.state.onBlocked();
		}
	}

	/** Frog sync */
	@SubscribeEvent
	public static void startTracking(PlayerEvent.StartTracking event) {
		if (event.getTarget() instanceof Frog frog) {
			FrogGodCapability.startTracking(frog, event.getEntity());
		}
	}

	/** Hat danmaku effects */
	@SubscribeEvent
	public static void onDanmakuDamageType(DanmakuDamageEvent event) {
		var le = event.getUser();
		for (ItemStack stack : TouhouHatItem.getEquippedHats(le)) {
			if (stack.getItem() instanceof TouhouHatItem hat) {
				event.setSource(hat.modifyDamageType(stack, le, event.getBullet(), event.getSource()));
			}
		}
	}

	/** Cat bell */
	@SubscribeEvent
	public static void onLivingTick(LivingEvent.LivingTickEvent event) {
		if (event.getEntity() instanceof Cat cat && cat.level() instanceof ServerLevel) {
			if (cat.isPassenger() && cat.getVehicle() instanceof ServerPlayer player) {
				if (cat.getTags().contains("CatBell")) {
					if (player.getXRot() < -45) {
						cat.unRide();
						GensokyoLegacy.HANDLER.toClientPlayer(new CatBell.MountToClient(cat.getId(), player.getId(), false), player);
					}
				}
			}
		}
	}

	/** IDimensionBoundEntity */
	@SubscribeEvent
	public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
		if (event.getEntity() instanceof IDimensionBoundEntity) {
			event.setCanceled(true);
		}
	}

	/** Cat bell */
	@SubscribeEvent
	public static void onLivingInteraction(PlayerInteractEvent.EntityInteract event) {
		if (event.getTarget() instanceof Cat cat && GLItems.CAT_BELL.isIn(event.getItemStack())) {
			event.setCancellationResult(GLItems.CAT_BELL.get().interactLivingEntity(event.getItemStack(), event.getEntity(), cat, event.getHand()));
			event.setCanceled(true);
		}
	}

	/** Quest */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void triggerDeath(LivingDeathEvent event) {
		if (event.getEntity().getKillCredit() instanceof ServerPlayer sp) {
			GLMeta.QUEST.type().getOrCreate(sp).dispatch(sp, new KillTrigger(sp, event.getEntity()));
		}
	}

}
