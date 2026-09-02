package dev.xkmc.gensokyolegacy.content.attachment.area;

import net.minecraft.resources.ResourceLocation;

public record AreaEffectVisual(ResourceLocation texture, float r, float g, float b, float a, float speed,
		boolean walls, boolean top, boolean bottom) {
}