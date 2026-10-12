package dev.xkmc.gensokyolegacy.content.attachment.area;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Server-owned payload of an effect, sent to the client as-is. Subclasses decide how long the owner
 * block keeps the effect alive and what it looks like.
 *
 * @see doc/design/area_effect.md
 */
@SerialClass
public abstract class EffectData {

	public boolean isOwnerStillValid(ServerLevel level, BlockPos ownerPos, BlockState state) {
		return !state.isAir();
	}

	// one visual per render pass; empty means invisible. The viewer is passed so a subclass can
	// gate on what they carry or wear.
	public List<AreaEffectVisual> getClientVisual(@Nullable Player viewer) {
		return List.of();
	}
}
