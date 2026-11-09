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
import dev.xkmc.gensokyolegacy.content.item.anchor.network.AnchorSwingPacket;
import dev.xkmc.gensokyolegacy.content.item.glove.network.DollGloveSwingPacket;
import dev.xkmc.gensokyolegacy.content.item.hakkero.Hakkero;
import dev.xkmc.gensokyolegacy.content.item.tool.CatBell;
import dev.xkmc.gensokyolegacy.content.item.tool.Dowser;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.*;
import dev.xkmc.gensokyolegacy.content.rpg.network.*;
import dev.xkmc.l2library.serial.config.PacketHandler;
import dev.xkmc.l2serial.network.BasePacketHandler;
import dev.xkmc.l2serial.network.RecordPacketBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Builds the mod's single {@link PacketHandler}, kept as {@link GensokyoLegacy#HANDLER}. <p> Every packet is registered
 * here in one chain; each call site decides only the direction, not the codec.
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
				.toServer(AnchorSwingPacket.class)
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
		private final List<Function<BasePacketHandler, BasePacketHandler.LoadedPacket<?>>> entries = new ArrayList<>();

		private Builder(int version) {
			this.version = version;
		}

		private <T extends Record & RecordPacketBase> Builder toClient(Class<T> type) {
			entries.add(e -> e.createRecord(type, NetworkDirection.PLAY_TO_CLIENT));
			return this;
		}

		private <T extends Record & RecordPacketBase> Builder toServer(Class<T> type) {
			entries.add(e -> e.createRecord(type, NetworkDirection.PLAY_TO_SERVER));
			return this;
		}

		private PacketHandler build() {
			return new PacketHandler(new ResourceLocation(GensokyoLegacy.MODID, "main"), version,
					entries.toArray(Function[]::new));
		}

	}

}