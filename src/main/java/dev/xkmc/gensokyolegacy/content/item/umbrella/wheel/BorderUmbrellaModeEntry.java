package dev.xkmc.gensokyolegacy.content.item.umbrella.wheel;

import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaMode;

public record BorderUmbrellaModeEntry(BorderUmbrellaMode mode) implements WheelAdaptor.Entry {

	@Override
	public void render(GuiGraphics g, float x0, float y0, float ai, float r0, float r, float da, boolean sel) {
		var s = sel ? 1.1f : 1;
		s *= Math.min(r * 0.015f, da * r0 / 16f);
		float dx = x0 + Mth.cos(ai) * r0;
		float dy = y0 + Mth.sin(ai) * r0;
		g.pose().pushPose();
		g.pose().translate(dx, dy, 0);
		g.pose().scale(s, s, s);
		ItemStack icon = mode.icon();
		g.renderItem(icon, -8, -8);
		g.renderItemDecorations(Minecraft.getInstance().font, icon, -8, -8);
		g.pose().popPose();
	}
}
