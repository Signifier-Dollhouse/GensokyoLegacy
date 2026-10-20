package dev.xkmc.gensokyolegacy.content.entity.dolls.network;

import dev.xkmc.gensokyolegacy.content.entity.dolls.SekibankiHeadEntity;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Gives the camera back to the player driving a head (gadget.md §6). The enter half needs no packet:
 * it is the head's own shift-click, so this only ever carries a release.
 */
public record HeadReleaseToServer(int headId) implements SerialPacketBase<HeadReleaseToServer> {

	@Override
	public void handle(Player player) {
		// a client may name any entity; only the head it actually owns lets go
		if (player instanceof ServerPlayer sp && sp.level().getEntity(headId) instanceof SekibankiHeadEntity head
				&& head.isPossessedBy(sp)) {
			head.release();
		}
	}

}