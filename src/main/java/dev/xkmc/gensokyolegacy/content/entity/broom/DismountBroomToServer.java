package dev.xkmc.gensokyolegacy.content.entity.broom;

import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.network.RecordPacketBase;
import net.minecraft.world.entity.player.Player;

/**
 * Asks the server to get the sender off their broom. A use packet cannot carry this: it repeats
 * while the button is held, and the client's mount arrives late, so the server would toggle the
 * ride off before the player ever sees it.
 */
public record DismountBroomToServer() implements RecordPacketBase {

	@Override
	public void handle(Player player) {
		if (player.getVehicle() instanceof BroomEntity broom && broom.getControllingPassenger() == player) {
			player.stopRiding();
			player.getCooldowns().addCooldown(GLItems.BROOM.get(), 10);
		}
	}

}
