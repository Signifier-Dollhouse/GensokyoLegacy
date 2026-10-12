package dev.xkmc.gensokyolegacy.content.attachment.area;

import net.minecraft.resources.ResourceLocation;

/**
 * One render pass over an effect's chunk range: texture, tint, scroll speed and which faces
 * ({@code walls}/{@code top}/{@code bottom}) are drawn. Visuals sharing a texture are batched
 * into a single draw call.
 *
 * @see doc/design/area_effect.md
 */
public record AreaEffectVisual(ResourceLocation texture, float r, float g, float b, float a, float speed,
                               boolean walls, boolean top, boolean bottom) {
}