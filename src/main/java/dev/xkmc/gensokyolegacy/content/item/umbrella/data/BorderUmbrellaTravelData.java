package dev.xkmc.gensokyolegacy.content.item.umbrella.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public record BorderUmbrellaTravelData(Vec3 origin, float yRot, float xRot, BlockPos target) {

	public BorderUmbrellaTravelData() {
		this(Vec3.ZERO, 0f, 0f, BlockPos.ZERO);
	}
}
