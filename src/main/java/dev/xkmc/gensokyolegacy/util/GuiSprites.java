package dev.xkmc.gensokyolegacy.util;

import com.mojang.blaze3d.systems.RenderSystem;
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

	/**
	 * Port of 1.21.1 {@code GuiGraphics.blitNineSlicedSprite}/{@code blitTiledSprite}, which upstream
	 * drives via nine-slice sprite metadata. 1.20.1's {@code GuiGraphics.blitNineSliced} is not
	 * equivalent: its {@code blitRepeating} center-crops partial tiles ({@code (tile - slice) / 2}),
	 * so the last piece of a tiled edge shows a shifted part of the pattern, and it never enables
	 * blend, so translucent sprites render opaque.
	 */
	public static void blitNineSliced(GuiGraphics g, ResourceLocation sprite, int x, int y, int z, int w, int h,
									  int left, int top, int right, int bottom, int texW, int texH) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		int i = Math.min(left, w / 2);
		int j = Math.min(right, w / 2);
		int k = Math.min(top, h / 2);
		int l = Math.min(bottom, h / 2);
		if (w == texW && h == texH) {
			piece(g, sprite, texW, texH, 0, 0, x, y, z, w, h);
		} else if (h == texH) {
			piece(g, sprite, texW, texH, 0, 0, x, y, z, i, h);
			tiled(g, sprite, x + i, y, z, w - j - i, h, i, 0, texW - j - i, texH, texW, texH);
			piece(g, sprite, texW, texH, texW - j, 0, x + w - j, y, z, j, h);
		} else if (w == texW) {
			piece(g, sprite, texW, texH, 0, 0, x, y, z, w, k);
			tiled(g, sprite, x, y + k, z, w, h - l - k, 0, k, texW, texH - l - k, texW, texH);
			piece(g, sprite, texW, texH, 0, texH - l, x, y + h - l, z, w, l);
		} else {
			piece(g, sprite, texW, texH, 0, 0, x, y, z, i, k);
			tiled(g, sprite, x + i, y, z, w - j - i, k, i, 0, texW - j - i, k, texW, texH);
			piece(g, sprite, texW, texH, texW - j, 0, x + w - j, y, z, j, k);
			piece(g, sprite, texW, texH, 0, texH - l, x, y + h - l, z, i, l);
			tiled(g, sprite, x + i, y + h - l, z, w - j - i, l, i, texH - l, texW - j - i, l, texW, texH);
			piece(g, sprite, texW, texH, texW - j, texH - l, x + w - j, y + h - l, z, j, l);
			tiled(g, sprite, x, y + k, z, i, h - l - k, 0, k, i, texH - l - k, texW, texH);
			tiled(g, sprite, x + i, y + k, z, w - j - i, h - l - k, i, k, texW - j - i, texH - l - k, texW, texH);
			tiled(g, sprite, x + w - j, y + k, z, i, h - l - k, texW - j, k, j, texH - l - k, texW, texH);
		}
		RenderSystem.disableBlend();
	}

	private static void piece(GuiGraphics g, ResourceLocation sprite, int texW, int texH,
							  int u, int v, int x, int y, int z, int w, int h) {
		if (w != 0 && h != 0) g.blit(texture(sprite), x, y, z, (float) u, (float) v, w, h, texW, texH);
	}

	private static void tiled(GuiGraphics g, ResourceLocation sprite, int x, int y, int z,
							  int w, int h, int u, int v, int tileW, int tileH, int texW, int texH) {
		if (w <= 0 || h <= 0 || tileW <= 0 || tileH <= 0) return;
		for (int px = 0; px < w; px += tileW) {
			int pw = Math.min(tileW, w - px);
			for (int py = 0; py < h; py += tileH) {
				int ph = Math.min(tileH, h - py);
				piece(g, sprite, texW, texH, u, v, x + px, y + py, z, pw, ph);
			}
		}
	}

}
