package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

@SerialClass
public abstract class EffectData {

	public boolean isOwnerStillValid(ServerLevel level, BlockPos ownerPos, BlockState state) {
		return !state.isAir();
	}
}
