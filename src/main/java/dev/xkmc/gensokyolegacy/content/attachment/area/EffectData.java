package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

@SerialClass
public abstract class EffectData {

	public boolean isOwnerStillValid(ServerLevel level, BlockPos ownerPos, BlockState state) {
		return !state.isAir();
	}

	// pure data; only read client-side. Subclasses return one or more visuals
	// (one per render pass they want to appear in) or empty for invisible.
	public List<AreaEffectVisual> getClientVisual() {
		return List.of();
	}
}
