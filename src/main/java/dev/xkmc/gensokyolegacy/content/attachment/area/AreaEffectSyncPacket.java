package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2serial.network.SerialPacketBase;
import dev.xkmc.l2serial.serialization.codec.PacketCodec;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Client sync for one effect: ADD/UPDATE carry the entry, REMOVE carries only the id.
 *
 * @see "doc/design/area_effect.md"
 */
public record AreaEffectSyncPacket(
		Action action,
		UUID id,
		@Nullable AreaEffectEntry entry
) implements SerialPacketBase<AreaEffectSyncPacket> {

	// honours @SerialField(toClient = false), so server-only payload fields never cross the wire
	public static final StreamCodec<RegistryFriendlyByteBuf, AreaEffectSyncPacket> CODEC = StreamCodec.of(
			(buf, packet) -> PacketCodec.to(buf, packet, AreaEffectSyncPacket.class, SerialField::toClient),
			buf -> Objects.requireNonNull(PacketCodec.from(buf, AreaEffectSyncPacket.class, null, SerialField::toClient)));

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
