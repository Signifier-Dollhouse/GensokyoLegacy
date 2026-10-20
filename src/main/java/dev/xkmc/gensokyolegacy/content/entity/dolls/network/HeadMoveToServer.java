package dev.xkmc.gensokyolegacy.content.entity.dolls.network;

import dev.xkmc.gensokyolegacy.content.entity.dolls.SekibankiHeadEntity;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * One finished frame of the client that owns a head's camera (gadget.md §5.3): where the head
 * ended up and where it is looking, rather than which keys were held. The flight model runs on the
 * owning client, so this is a position report and the server only re-applies the leash to it.
 */
public record HeadMoveToServer(int headId, double x, double y, double z, float yaw, float pitch)
		implements SerialPacketBase<HeadMoveToServer> {

	@Override
	public void handle(Player player) {
		// a client may name any entity; only the head it actually owns answers
		if (player instanceof ServerPlayer sp && sp.level().getEntity(headId) instanceof SekibankiHeadEntity head
				&& head.isPossessedBy(sp)) {
			head.acceptMove(sp, x, y, z, yaw, pitch);
		}
	}

}