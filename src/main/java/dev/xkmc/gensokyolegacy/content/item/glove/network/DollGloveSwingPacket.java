package dev.xkmc.gensokyolegacy.content.item.glove.network;

import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Client-to-server left-click-empty while the glove is held in an attack mode:
 * issue the mode command at the cached ray-trace target (never the editor).
 * Empty clicks never reach the server on their own, so the client forwards
 * them; block clicks and entity punches arrive through their own events.
 */
public record DollGloveSwingPacket() implements SerialPacketBase<DollGloveSwingPacket> {

	@Override
	public void handle(Player player) {
		if (player instanceof ServerPlayer sp) {
			ItemStack stack = sp.getMainHandItem();
			if (!(stack.getItem() instanceof DollGloveItem glove)) return;
			var mode = DollGloveItem.getMode(stack);
			if (!mode.isAttackCommand() || sp.getCooldowns().isOnCooldown(glove)) return;
			mode.performAttack(sp, InteractionHand.MAIN_HAND, stack, glove);
		}
	}

}
