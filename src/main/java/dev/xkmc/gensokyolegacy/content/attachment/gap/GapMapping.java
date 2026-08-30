package dev.xkmc.gensokyolegacy.content.attachment.gap;

import dev.xkmc.gensokyolegacy.content.block.portal.PortalSide;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public record GapMapping(
		@Nullable BlockPos entryPos,
		@Nullable ResourceLocation entryDim,
		@Nullable BlockPos exitPos,
		@Nullable ResourceLocation exitDim
) {

	public boolean isPending() {
		return entryPos == null || entryDim == null || exitPos == null || exitDim == null;
	}

	public @Nullable BlockPos posAt(PortalSide side) {
		return side == PortalSide.ENTRY ? entryPos : exitPos;
	}

	public @Nullable ResourceLocation dimAt(PortalSide side) {
		return side == PortalSide.ENTRY ? entryDim : exitDim;
	}

	public GapMapping with(PortalSide side, @Nullable BlockPos pos, @Nullable ResourceLocation dim) {
		return side == PortalSide.ENTRY ? new GapMapping(pos, dim, exitPos, exitDim)
				: new GapMapping(entryPos, entryDim, pos, dim);
	}

	public boolean isSidePending(PortalSide side) {
		return posAt(side) == null || dimAt(side) == null;
	}

}
