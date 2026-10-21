package dev.xkmc.gensokyolegacy.init;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectSyncPacket;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataToClient;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollRosterToClient;
import dev.xkmc.gensokyolegacy.content.attachment.misc.FrogSyncPacket;
import dev.xkmc.gensokyolegacy.content.attachment.misc.KoishiStartPacket;
import dev.xkmc.gensokyolegacy.content.attachment.party.network.PartyGiftToClient;
import dev.xkmc.gensokyolegacy.content.client.debug.*;
import dev.xkmc.gensokyolegacy.content.client.structure.*;
import dev.xkmc.gensokyolegacy.content.entity.behavior.move.PathDataToClient;
import dev.xkmc.gensokyolegacy.content.entity.dolls.network.HeadMoveToServer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.network.HeadReleaseToServer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.network.HeadUseToServer;
import dev.xkmc.gensokyolegacy.content.entity.foundation.CombatToClient;
import dev.xkmc.gensokyolegacy.content.item.common.network.GloveTargetPacket;
import dev.xkmc.gensokyolegacy.content.item.common.network.SelectorSelectPacket;
import dev.xkmc.gensokyolegacy.content.item.glove.network.DollGloveSwingPacket;
import dev.xkmc.gensokyolegacy.content.item.hakkero.Hakkero;
import dev.xkmc.gensokyolegacy.content.item.tool.CatBell;
import dev.xkmc.gensokyolegacy.content.item.tool.Dowser;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.*;
import dev.xkmc.gensokyolegacy.content.rpg.network.*;
import dev.xkmc.l2serial.network.PacketHandler;
import dev.xkmc.l2serial.network.SerialPacketBase;
import dev.xkmc.l2serial.network.SimplePacketBase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Builds the mod's single {@link PacketHandler}, kept as {@link GensokyoLegacy#HANDLER}. <p> Every packet is registered
 * here in one chain; each call site decides only the direction, not the codec. Serialized {@code Record} packets use
 * the two-argument forms (the codec comes from l2serial's {@code CodecAdaptor}); anything else passes its own {@link
 * StreamCodec} to the three-argument form.
 */
public class GLPackets {

	private GLPackets() {}

	public static PacketHandler create(int version) {
		return new Builder(version)
				// debug packets, plus the shared structure block editing
				.toClient(CharDataToClient.class)
				.toClient(PathDataToClient.class)
				.toServer(BlockRequestToServer.class)
				.toClient(BlockInfoToClient.class)
				.toServer(CharacterRequestToServer.class)
				.toServer(DoorRequestToServer.class)
				.toClient(CharacterInfoToClient.class)
				.toClient(StructureBoundUpdateToClient.class)
				.toClient(CustomStructureBoundUpdateToClient.class)
				.toServer(StructureInfoRequestToServer.class)
				.toClient(StructureInfoUpdateToClient.class)
				.toServer(StructureRepairToServer.class)
				.toServer(StructureEditToServer.class)
				.toClient(CombatToClient.class)
				.toClient(AreaEffectSyncPacket.class)

				// quest and trade status, then the dialog packets:
				// the dialog screen runs on no container menu, so it syncs over its own packets
				.toClient(QuestStatusToClient.class)
				.toClient(QuestLootToClient.class)
				.toClient(TradeStatusToClient.class)
				.toClient(FirstDialogToClient.class)
				.toClient(SimpleDialogToClient.class)
				.toClient(DialogCloseToClient.class)
				.toServer(DialogClickToServer.class)
				.toServer(DialogCloseToServer.class)

				.toClient(FrogSyncPacket.class)
				.toClient(KoishiStartPacket.class)
				.toClient(PartyGiftToClient.class)
				.toClient(Dowser.DowserToClient.class)
				.toClient(CatBell.MountToClient.class)
				.toClient(Hakkero.BroomBoostToClient.class)

				.toServer(BorderUmbrellaWheelSelectPacket.class)
				.toServer(BorderUmbrellaRenamePacket.class)
				.toServer(BorderUmbrellaConfirmRecordPacket.class)
				.toServer(BorderUmbrellaDeletePacket.class)
				.toServer(BorderUmbrellaReorderPacket.class)
				.toClient(BorderUmbrellaOpenRenamePacket.class)

				.toServer(DollGloveSwingPacket.class)
				.toClient(DollRosterToClient.class)

				// the head's own hands while its owner looks out of it: the camera arrives on
				// vanilla's set-camera packet, so only these three ever cross the wire
				.toServer(HeadMoveToServer.class)
				.toServer(HeadUseToServer.class)
				.toServer(HeadReleaseToServer.class)

				// the shared glove target cache (glove.md §2): one packet for every targeting glove
				.toServer(GloveTargetPacket.class)
				// the shared selector-wheel mode pick: one packet for every wheel-owning item
				.toServer(SelectorSelectPacket.class)
				.build();
	}

	private static class Builder {

		private final int version;
		private final List<Function<PacketHandler, PacketHandler.PacketConfiguration<?>>> entries = new ArrayList<>();

		private Builder(int version) {
			this.version = version;
		}

		private <T extends Record & SerialPacketBase<T>> Builder toClient(Class<T> type) {
			entries.add(e -> e.create(type, PacketHandler.NetDir.PLAY_TO_CLIENT));
			return this;
		}

		private <T extends Record & SerialPacketBase<T>> Builder toServer(Class<T> type) {
			entries.add(e -> e.create(type, PacketHandler.NetDir.PLAY_TO_SERVER));
			return this;
		}

		private <T extends SimplePacketBase> Builder toClient(Class<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
			entries.add(e -> e.create(type, codec, PacketHandler.NetDir.PLAY_TO_CLIENT));
			return this;
		}

		private <T extends SimplePacketBase> Builder toServer(Class<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
			entries.add(e -> e.create(type, codec, PacketHandler.NetDir.PLAY_TO_SERVER));
			return this;
		}

		private PacketHandler build() {
			return new PacketHandler(GensokyoLegacy.MODID, version,
					entries.toArray(Function[]::new));
		}

	}

}