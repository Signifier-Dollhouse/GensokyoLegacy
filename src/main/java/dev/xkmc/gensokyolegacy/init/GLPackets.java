package dev.xkmc.gensokyolegacy.init;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectSyncPacket;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataToClient;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollRosterToClient;
import dev.xkmc.gensokyolegacy.content.attachment.misc.FrogSyncPacket;
import dev.xkmc.gensokyolegacy.content.attachment.misc.KoishiStartPacket;
import dev.xkmc.gensokyolegacy.content.client.debug.BlockInfoToClient;
import dev.xkmc.gensokyolegacy.content.client.debug.BlockRequestToServer;
import dev.xkmc.gensokyolegacy.content.client.debug.CharacterInfoToClient;
import dev.xkmc.gensokyolegacy.content.client.debug.CharacterRequestToServer;
import dev.xkmc.gensokyolegacy.content.client.debug.DoorRequestToServer;
import dev.xkmc.gensokyolegacy.content.client.structure.CustomStructureBoundUpdateToClient;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureBoundUpdateToClient;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureEditToServer;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureInfoRequestToServer;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureInfoUpdateToClient;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureRepairToServer;
import dev.xkmc.gensokyolegacy.content.entity.behavior.move.PathDataToClient;
import dev.xkmc.gensokyolegacy.content.entity.foundation.CombatToClient;
import dev.xkmc.gensokyolegacy.content.item.common.network.GloveTargetPacket;
import dev.xkmc.gensokyolegacy.content.item.common.network.SelectorSelectPacket;
import dev.xkmc.gensokyolegacy.content.item.glove.network.DollGloveSwingPacket;
import dev.xkmc.gensokyolegacy.content.item.tool.CatBell;
import dev.xkmc.gensokyolegacy.content.item.tool.Dowser;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaConfirmRecordPacket;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaDeletePacket;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaOpenRenamePacket;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaRenamePacket;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaReorderPacket;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaWheelSelectPacket;
import dev.xkmc.gensokyolegacy.content.rpg.network.DialogClickToServer;
import dev.xkmc.gensokyolegacy.content.rpg.network.DialogCloseToClient;
import dev.xkmc.gensokyolegacy.content.rpg.network.DialogCloseToServer;
import dev.xkmc.gensokyolegacy.content.rpg.network.FirstDialogToClient;
import dev.xkmc.gensokyolegacy.content.rpg.network.QuestLootToClient;
import dev.xkmc.gensokyolegacy.content.rpg.network.QuestStatusToClient;
import dev.xkmc.gensokyolegacy.content.rpg.network.SimpleDialogToClient;
import dev.xkmc.gensokyolegacy.content.rpg.network.TradeStatusToClient;
import dev.xkmc.l2serial.network.PacketHandler;
import dev.xkmc.l2serial.network.SerialPacketBase;
import dev.xkmc.l2serial.network.SimplePacketBase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Builds the mod's single {@link PacketHandler}, kept as {@link GensokyoLegacy#HANDLER}.
 * <p>
 * Every packet is registered here in one chain; the direction only, not the codec, is what
 * each call site decides. Serialized {@code Record} packets use the two-argument forms
 * (the codec comes from l2serial's {@code CodecAdaptor}); anything else passes its own
 * {@link StreamCodec} to the three-argument form.
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
				.toClient(Dowser.DowserToClient.class)
				.toClient(CatBell.MountToClient.class)

				.toServer(BorderUmbrellaWheelSelectPacket.class)
				.toServer(BorderUmbrellaRenamePacket.class)
				.toServer(BorderUmbrellaConfirmRecordPacket.class)
				.toServer(BorderUmbrellaDeletePacket.class)
				.toServer(BorderUmbrellaReorderPacket.class)
				.toClient(BorderUmbrellaOpenRenamePacket.class)

				.toServer(DollGloveSwingPacket.class)
				.toClient(DollRosterToClient.class)

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