package dev.xkmc.gensokyolegacy.content.item.dagger.client;

import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveItem;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveMode;
import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * One mode on the {@link DaggerGloveModeWheel}, drawn as a glove stack carrying that mode's wheel
 * icon. The four held textures are identical copies of one glove, so the wheel tells the modes
 * apart by these icons rather than by the held texture (dagger_glove.md §6).
 */
public record DaggerGloveModeEntry(DaggerGloveMode mode) implements WheelAdaptor.Entry {

	@Override
	public void render(GuiGraphics g, float x0, float y0, float ai, float r0, float r, float da, boolean sel) {
		float s = (sel ? 1.1f : 1) * Math.min(r * 0.015f, da * r0 / 16f);
		float dx = x0 + Mth.cos(ai) * r0;
		float dy = y0 + Mth.sin(ai) * r0;
		g.pose().pushPose();
		g.pose().translate(dx, dy, 0);
		g.pose().scale(s, s, s);
		ItemStack icon = DaggerGloveItem.iconStack(mode);
		g.renderItem(icon, -8, -8);
		g.renderItemDecorations(Minecraft.getInstance().font, icon, -8, -8);
		g.pose().popPose();
	}

}