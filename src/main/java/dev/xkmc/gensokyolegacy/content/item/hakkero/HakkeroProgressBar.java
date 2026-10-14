package dev.xkmc.gensokyolegacy.content.item.hakkero;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * The cooldown-style wipe showing smelting progress over a 16x16 slot, shared by every hakkero
 * readout — the finished one's input slots, the prototype's four neighbours. Client-only.
 */
public final class HakkeroProgressBar {

	private HakkeroProgressBar() {
	}

	/**
	 * Fills the slot at {@code (x, y)} from the bottom up to {@code progress}, counted as a
	 * 0..1 fraction. Nothing is drawn at or below zero.
	 */
	public static void draw(GuiGraphics g, int x, int y, float progress) {
		if (progress <= 0.0F) return;
		int y0 = y + Mth.floor(16.0F * (1.0F - progress));
		int y1 = y0 + Mth.ceil(16.0F * progress);
		g.fill(RenderType.guiOverlay(), x, y0, x + 16, y1, Integer.MAX_VALUE);
	}

}
