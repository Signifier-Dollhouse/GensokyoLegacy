package dev.xkmc.gensokyolegacy.content.item.glove.network;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Client-to-server ray-trace target sync (glove.md §2). Sent on every client
 * cache refresh while the glove is held; the server stores it as a hint and
 * re-validates on every use.
 */
public record DollGloveTargetPacket(UUID target) implements SerialPacketBase<DollGloveTargetPacket> {

	@Override
	public void handle(Player player) {
		if (player instanceof ServerPlayer sp) {
			var commands = GLMeta.DOLL.type().getOrCreate(sp).commands;
			commands.gloveTarget = target;
			commands.gloveTargetTime = sp.level().getGameTime();
		}
	}

}
