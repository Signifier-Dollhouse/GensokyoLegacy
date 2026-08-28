package dev.xkmc.gensokyolegacy.content.attachment.gap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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

}
