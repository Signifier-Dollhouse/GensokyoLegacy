package dev.xkmc.gensokyolegacy.content.item.umbrella.wheel;

import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

	public ItemStack icon() {
		return switch (distance) {
			case 10 -> new ItemStack(Items.PAPER);
			case 100 -> new ItemStack(Items.MAP);
			case 1000 -> new ItemStack(Items.FILLED_MAP);
			case 10000 -> new ItemStack(Items.NETHER_STAR);
			default -> new ItemStack(Items.ENDER_PEARL);
		};
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
		ItemStack icon = icon();
		g.renderItem(icon, -8, -8);
		g.renderItemDecorations(Minecraft.getInstance().font, icon, -8, -8);
		g.pose().popPose();
	}
}
