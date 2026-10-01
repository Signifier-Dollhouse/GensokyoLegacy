package dev.xkmc.gensokyolegacy.content.client.structure;

import dev.xkmc.gensokyolegacy.content.attachment.datamap.StructureInterior;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.IHomeHolder;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public record StructureBoundUpdateToClient(
		StructureKey key, Box structure, ArrayList<Box> house, ArrayList<Box> rooms, StructureInterior interior
) implements SerialPacketBase<StructureBoundUpdateToClient>, IStructureBound {

	public static void clickBlockInServer(Player player, BlockPos pos) {
		if (!(player instanceof ServerPlayer sp)) return;
		var home = IHomeHolder.find(sp.serverLevel(), pos);
		if (home == null || !home.isValid()) return;
		GensokyoLegacy.HANDLER.toClientPlayer(home.toBoundPacket(), sp);
	}

	public StructureBoundUpdateToClient(StructureKey key, BoundingBox structure, List<BoundingBox> house, List<BoundingBox> rooms, StructureInterior interior) {
		this(key, Box.of(structure),
				house.stream().map(Box::of).collect(ArrayList::new, ArrayList::add, ArrayList::addAll),
				rooms.stream().map(Box::of).collect(ArrayList::new, ArrayList::add, ArrayList::addAll), interior);
	}

	@Override
	public void handle(Player player) {
		StructureInfoClientManager.setStructure(this);
	}

}
