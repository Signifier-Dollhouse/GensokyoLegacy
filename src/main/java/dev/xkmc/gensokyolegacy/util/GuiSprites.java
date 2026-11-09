package dev.xkmc.gensokyolegacy.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.20.1 has no GUI sprite atlas: {@code textures/gui/sprites/<path>.png} are drawn as loose textures.
 */
public class GuiSprites {

	public static ResourceLocation texture(ResourceLocation sprite) {
		return sprite.withPrefix("textures/gui/sprites/").withSuffix(".png");
	}

	public static void blit(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h) {
		g.blit(texture(sprite), x, y, w, h, 0, 0, w, h, w, h);
	}

	public static void blit(GuiGraphics g, ResourceLocation sprite, int x, int y, int z, int w, int h) {
		g.pose().pushPose();
		g.pose().translate(0, 0, z);
		blit(g, sprite, x, y, w, h);
		g.pose().popPose();
	}

	public static void blitScaled(GuiGraphics g, ResourceLocation sprite, int x, int y, int z, int w, int h, int texW, int texH) {
		g.pose().pushPose();
		g.pose().translate(0, 0, z);
		g.blit(texture(sprite), x, y, w, h, 0, 0, texW, texH, texW, texH);
		g.pose().popPose();
	}

	public static void blitNineSliced(GuiGraphics g, ResourceLocation sprite, int x, int y, int z, int w, int h,
									  int left, int top, int right, int bottom, int texW, int texH) {
		g.pose().pushPose();
		g.pose().translate(0, 0, z);
		g.blitNineSliced(texture(sprite), x, y, w, h, left, top, right, bottom, texW, texH, 0, 0);
		g.pose().popPose();
	}

}
