package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.danmakuapi.api.DanmakuDamageEvent;
import dev.xkmc.gensokyolegacy.content.attachment.misc.FrogGodCapability;
import dev.xkmc.gensokyolegacy.content.attachment.storage.PendingItemStorage;
import dev.xkmc.gensokyolegacy.content.entity.characters.rumia.RumiaEntity;
import dev.xkmc.gensokyolegacy.content.item.character.TouhouHatItem;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.content.item.glove.network.DollGloveSwingPacket;
import dev.xkmc.gensokyolegacy.content.item.tool.CatBell;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaUnlock;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLDamageTypes;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class MiscEventHandlers {

	@SubscribeEvent
	public static void onAnvilUpdate(AnvilUpdateEvent e) {
		ItemStack left = e.getLeft();
		ItemStack right = e.getRight();
		if (!(left.getItem() instanceof BorderUmbrellaItem)) return;
		var unlock = GLItems.UMBRELLA_UNLOCK.getOrDefault(left, BorderUmbrellaUnlock.DEFAULT);
		if (right.is(Items.CHORUS_FRUIT)) {
			if (unlock.travelUnlocked()) return;
			ItemStack out = left.copy();
			out.set(GLItems.UMBRELLA_UNLOCK.get(), unlock.withTravel(true));
			e.setOutput(out);
			e.setCost(5);
			e.setMaterialCost(1);
		} else if (right.is(Items.ECHO_SHARD)) {
			if (unlock.captureUnlocked()) return;
			ItemStack out = left.copy();
			out.set(GLItems.UMBRELLA_UNLOCK.get(), unlock.withCapture(true));
			e.setOutput(out);
			e.setCost(10);
			e.setMaterialCost(1);
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer sp)) return;
		if (sp.tickCount % 20 != 0) return;
		var storage = PendingItemStorage.get(sp.serverLevel());
		if (!storage.hasPending(sp.getUUID())) return;
		if (storage.deliver(sp) && !storage.hasPending(sp.getUUID()))
			sp.sendSystemMessage(PendingItemStorage.RETURN_MSG);
	}

	@SubscribeEvent
	public static void onShieldBlock(LivingShieldBlockEvent event) {
		if (event.getDamageSource().is(GLDamageTypes.KOISHI)) {
			if (event.getEntity() instanceof Player player) {
				GLMeta.KOISHI_ATTACK.type().getOrCreate(player).onBlock(player);
			}
		}
		if (event.getBlocked() && event.getDamageSource().getDirectEntity() instanceof RumiaEntity rumia) {
			rumia.state.onBlocked();
		}
	}

	@SubscribeEvent
	public static void startTracking(PlayerEvent.StartTracking event) {
		if (event.getTarget() instanceof Frog frog) {
			FrogGodCapability.startTracking(frog, event.getEntity());
		}
	}

	@SubscribeEvent
	public static void onDanmakuDamageType(DanmakuDamageEvent event) {
		var le = event.getUser();
		ItemStack stack = le.getItemBySlot(EquipmentSlot.HEAD);
		if (stack.getItem() instanceof TouhouHatItem hat) {
			event.setSource(hat.modifyDamageType(stack, le, event.getBullet(), event.getSource()));
		}
	}

	@SubscribeEvent
	public static void onLivingTick(EntityTickEvent.Pre event) {
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

	@SubscribeEvent
	public static void onLivingInteraction(PlayerInteractEvent.EntityInteract event) {
		if (event.getTarget() instanceof Cat cat && event.getItemStack().is(GLItems.CAT_BELL)) {
			event.setCancellationResult(GLItems.CAT_BELL.get().interactLivingEntity(event.getItemStack(), event.getEntity(), cat, event.getHand()));
			event.setCanceled(true);
		}
	}

}
