package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.overlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record TileClientTooltip(List<ItemStack> items, List<FluidStack> fluids, int width,
                                int height) implements ClientTooltipComponent {

	public TileClientTooltip(TileTooltip inv) {
		this(inv.items(), inv.fluids(), inv.w(), inv.h());
	}

	@Override
	public int getHeight() {
		return height * 18 + 2;
	}

	@Override
	public int getWidth(Font font) {
		return width * 18 + 2;
	}

	@Override
	public void renderImage(Font font, int mx, int my, GuiGraphics g) {
		int w = width;
		int n = 0;
		for (ItemStack stack : items) {
			if (stack.isEmpty()) continue;
			int y = my + n / w * 18 + 1;
			int x = mx + n % w * 18 + 1;
			renderSlot(font, x, y, g, stack);
			n++;
		}
		for (FluidStack stack : fluids) {
			if (stack.isEmpty()) continue;
			int y = my + n / w * 18 + 1;
			int x = mx + n % w * 18 + 1;
			// fluid rendering in tooltip: fallback to bucket item if needed
			// For alchemy hints we don't use fluids, so skip detailed fluid render
			// If needed, could render fluid icon via FluidStack's fluid bucket
			// Keep empty for now
			//n++;
		}
	}

	private void renderSlot(Font font, int x, int y, GuiGraphics g, ItemStack stack) {
		if (!stack.isEmpty()) {
			g.renderItem(stack, x + 1, y + 1, 0);
			g.renderItemDecorations(font, stack, x + 1, y + 1);
		}
	}
}
