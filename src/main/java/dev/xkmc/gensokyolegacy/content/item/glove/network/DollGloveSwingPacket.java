package dev.xkmc.gensokyolegacy.content.item.glove.network;

import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Left-click-empty in an attack mode: the mode command at the cached ray-trace target, never the editor.
 * Empty clicks never reach the server on their own, so the client forwards them.
 * @see "doc/design/doll/glove.md"
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
