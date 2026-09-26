package dev.xkmc.gensokyolegacy.content.item.umbrella.wheel;

import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public record BorderUmbrellaDistanceEntry(int distance) implements WheelAdaptor.Entry {

	public static final int[] DISTANCES = {10, 100, 1000, 10000};

	public static int indexOf(int distance) {
		for (int i = 0; i < DISTANCES.length; i++) {
			if (DISTANCES[i] == distance) return i;
		}
		return 2; // default 1000
	}

	public static int distanceOf(int index) {
		int i = Math.floorMod(index, DISTANCES.length);
		return DISTANCES[i];
	}

	public Component displayName() {
		return Component.literal(distance + " blocks");
	}

	public ResourceLocation icon() {
		return UmbrellaWheelIcons.distanceIcon(distance);
	}

	@Override
	public void render(GuiGraphics g, float x0, float y0, float ai, float r0, float r, float da, boolean sel) {
		var s = sel ? 1.1f : 1;
		s *= Math.min(r * 0.015f, da * r0 / 16f);
		float dx = x0 + Mth.cos(ai) * r0;
		float dy = y0 + Mth.sin(ai) * r0;
		g.pose().pushPose();
		g.pose().translate(dx, dy, 0);
		g.pose().scale(s, s, s);
		UmbrellaWheelIcons.render(g, icon());
		g.pose().popPose();
	}
}
