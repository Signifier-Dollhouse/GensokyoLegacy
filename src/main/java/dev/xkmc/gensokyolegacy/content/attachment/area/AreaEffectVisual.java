package dev.xkmc.gensokyolegacy.content.attachment.area;

import net.minecraft.resources.ResourceLocation;

/**
 * One render pass over an effect's chunk range: texture, tint, scroll speed and which of
 * {@code walls}/{@code top}/{@code bottom} are drawn. Shared texture, one draw call.
 *
 * @see "doc/design/area_effect.md"
 */
public record AreaEffectVisual(ResourceLocation texture, float r, float g, float b, float a, float speed,
                               boolean walls, boolean top, boolean bottom) {
}