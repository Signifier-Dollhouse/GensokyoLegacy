package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorItem;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorMode;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorThrow;
import dev.xkmc.gensokyolegacy.content.item.anchor.network.AnchorSwingPacket;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Left-click plumbing for the anchor: Destroy and Voyage only throw on an empty click, and Storm
 * throws on every left click.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class AnchorEventHandlers {

	@SubscribeEvent
	public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
		Player player = event.getEntity();
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof AnchorItem)) return;
		GensokyoLegacy.HANDLER.toServer(new AnchorSwingPacket());
	}

	@SubscribeEvent
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		Player player = event.getEntity();
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof AnchorItem)) return;
		if (AnchorItem.getMode(stack) != AnchorMode.STORM) return;
		event.setCanceled(true);
		if (player instanceof ServerPlayer sp) {
			AnchorItem.throwAnchors(sp, InteractionHand.MAIN_HAND, AnchorThrow.STORM);
		}
	}

	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		Player player = event.getEntity();
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof AnchorItem)) return;
		if (AnchorItem.getMode(stack) != AnchorMode.STORM) return;
		event.setCanceled(true);
		if (player instanceof ServerPlayer sp) {
			AnchorItem.throwAnchors(sp, InteractionHand.MAIN_HAND, AnchorThrow.STORM);
		}
	}

}
