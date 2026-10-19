package dev.xkmc.gensokyolegacy.content.entity.dolls.network;

import dev.xkmc.gensokyolegacy.content.entity.dolls.SekibankiHeadEntity;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The head's own right click on a block (gadget.md §6). Carries the block rather than a click, so
 * the server can re-trace the head's crosshair and run the block's empty-hand interaction itself:
 * the body behind the camera neither swings nor loses anything.
 */
public record HeadUseToServer(int headId, BlockPos pos, Direction face, Vec3 location)
		implements SerialPacketBase<HeadUseToServer> {

	@Override
	public void handle(Player player) {
		if (player instanceof ServerPlayer sp && sp.level().getEntity(headId) instanceof SekibankiHeadEntity head) {
			head.useBlock(sp, new BlockHitResult(location, face, pos, false));
		}
	}

}