package dev.xkmc.gensokyolegacy.content.item.common;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ClientInvTooltip(InvTooltip inv) implements ClientTooltipComponent {

	// 1.20.1 has no sprite atlas; this is the slot cell of the vanilla bundle texture.
	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/bundle.png");

	@Override
	public int getHeight() {
		return inv.h() * 18 + 2;
	}

	@Override
	public int getWidth(Font font) {
		return 18 * inv.w();
	}

	@Override
	public void renderImage(Font font, int mx, int my, GuiGraphics g) {
		for (int i = 0; i < Math.min(inv.w() * inv.h(), inv.list().size()); i++) {
			renderSlot(font, mx + i % inv.w() * 18, my + i / inv.w() * 18, g, inv.list().get(i));
		}
	}

	private static void renderSlot(Font font, int x, int y, GuiGraphics g, ItemStack stack) {
		g.blit(TEXTURE, x, y, 0, 0, 18, 20, 128, 128);
		if (stack.isEmpty()) return;
		g.renderItem(stack, x + 1, y + 1, 0);
		g.renderItemDecorations(font, stack, x + 1, y + 1);
	}

}