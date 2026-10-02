package dev.xkmc.gensokyolegacy.content.item.targeting.network;

import dev.xkmc.gensokyolegacy.content.item.targeting.GloveTargeting;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.UUID;

/**
 * Client-to-server ray-trace target sync (glove.md §2), one packet for every
 * {@link GloveTargeting} glove. Sent on every client cache refresh while the glove is held; the
 * glove identifies its own slot, and the server stores the UUID as a hint and re-validates it on
 * every use ({@code GloveTargetAttachment}).
 */
public record GloveTargetPacket(Item glove, UUID target) implements SerialPacketBase<GloveTargetPacket> {

	@Override
	public void handle(Player player) {
		// a client may claim any item at all; only a glove that really implements the contract
		// gets a slot written for it
		if (player instanceof ServerPlayer sp && glove instanceof GloveTargeting) {
			GLMeta.GLOVE_TARGET.type().getOrCreate(sp).mark(glove, target, sp.level().getGameTime());
		}
	}

}