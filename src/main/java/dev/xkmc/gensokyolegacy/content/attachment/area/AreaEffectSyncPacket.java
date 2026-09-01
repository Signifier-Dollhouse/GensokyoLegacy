package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record AreaEffectSyncPacket(
		Action action,
		UUID id,
		@Nullable AreaEffectEntry entry
) implements SerialPacketBase<AreaEffectSyncPacket> {

	public enum Action {
		ADD, UPDATE, REMOVE
	}

	public static void sendAdd(ServerPlayer player, AreaEffectEntry entry) {
		GensokyoLegacy.HANDLER.toClientPlayer(new AreaEffectSyncPacket(Action.ADD, entry.id, entry), player);
	}

	public static void sendUpdate(ServerPlayer player, AreaEffectEntry entry) {
		GensokyoLegacy.HANDLER.toClientPlayer(new AreaEffectSyncPacket(Action.UPDATE, entry.id, entry), player);
	}

	public static void sendRemove(ServerPlayer player, UUID id) {
		GensokyoLegacy.HANDLER.toClientPlayer(new AreaEffectSyncPacket(Action.REMOVE, id, null), player);
	}

	@Override
	public void handle(Player player) {
		ClientAreaEffectTracker.onSync(this);
	}
}
