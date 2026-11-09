package dev.xkmc.gensokyolegacy.content.item.anchor.network;

import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorItem;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorMode;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorThrow;
import dev.xkmc.l2serial.network.RecordPacketBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * An empty left click with the anchor held: the throw cannot reach {@code use}, and empty clicks
 * never reach the server on their own, so the client forwards them.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public record AnchorSwingPacket() implements RecordPacketBase {

	@Override
	public void handle(Player player) {
		if (!(player instanceof ServerPlayer sp)) return;
		ItemStack stack = sp.getMainHandItem();
		if (!(stack.getItem() instanceof AnchorItem)) return;
		AnchorThrow type = switch (AnchorItem.getMode(stack)) {
			case DESTROY -> AnchorThrow.DESTROY;
			case VOYAGE -> AnchorThrow.GRAB;
			case STORM -> AnchorThrow.STORM;
		};
		AnchorItem.throwAnchors(sp, InteractionHand.MAIN_HAND, type);
	}

}
