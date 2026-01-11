package dev.xkmc.gensokyolegacy.content.client.structure;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.IHomeHolder;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record StructureBoundUpdateToClient(
		StructureKey key, Box structure, Box house, Box room
) implements SerialPacketBase<StructureBoundUpdateToClient>, IStructureBound {

	public static void clickBlockInServer(Player player, BlockPos pos) {
		if (!(player instanceof ServerPlayer sp)) return;
		var home = IHomeHolder.find(sp.serverLevel(), pos);
		if (home == null || !home.isValid()) return;
		GensokyoLegacy.HANDLER.toClientPlayer(home.toBoundPacket(), sp);
	}

	public StructureBoundUpdateToClient(StructureKey key, BoundingBox structure, BoundingBox house, BoundingBox room) {
		this(key, Box.of(structure), Box.of(house), Box.of(room));
	}

	@Override
	public void handle(Player player) {
		StructureInfoClientManager.setStructure(this);
	}

}
