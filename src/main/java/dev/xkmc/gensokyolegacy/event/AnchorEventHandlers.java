package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorItem;
import dev.xkmc.gensokyolegacy.content.item.anchor.network.AnchorSwingPacket;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Left-click plumbing for the anchor: an empty click throws the mode's throw and an entity click
 * swings at a radius of 3, in every mode. Both are dead while a hook is out.
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
		if (AnchorItem.isLinked(stack)) return;
		GensokyoLegacy.HANDLER.toServer(new AnchorSwingPacket());
	}

	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		Player player = event.getEntity();
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof AnchorItem)) return;
		event.setCanceled(true);
		if (AnchorItem.isLinked(stack)) return;
		if (!(player instanceof ServerPlayer sp)) return;
		AnchorItem.melee(sp, event.getTarget());
	}

}
