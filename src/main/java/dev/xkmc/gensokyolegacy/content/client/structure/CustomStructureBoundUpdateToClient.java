package dev.xkmc.gensokyolegacy.content.client.structure;

import dev.xkmc.gensokyolegacy.content.attachment.home.custom.RoomData;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record CustomStructureBoundUpdateToClient(
		StructureKey key, RoomData data
) implements SerialPacketBase<CustomStructureBoundUpdateToClient>, IStructureBound {

	@Override
	public void handle(Player player) {
		StructureInfoClientManager.setStructure(this);
	}

	@Override
	public List<Box> rooms() {
		return List.of(Box.of(data().bound));
	}

	@Override
	public Box house() {
		return Box.of(data().bound.inflatedBy(1));
	}

	@Override
	public Box structure() {
		return Box.of(data().bound.inflatedBy(1));
	}

}
