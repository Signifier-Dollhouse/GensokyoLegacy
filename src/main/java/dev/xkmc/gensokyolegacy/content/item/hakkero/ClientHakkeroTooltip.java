package dev.xkmc.gensokyolegacy.content.item.hakkero;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The finished Mini Hakkero's tooltip image, drawn in the menu's layout: input slots with a progress wipe,
 * then the results, then the fuel — one simulated state, current between the server's {@code IDLE_BATCH} refreshes.
 */
public final class ClientHakkeroTooltip implements ClientTooltipComponent {

	private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/bundle/slot");

	private final HakkeroTooltip inv;

	public ClientHakkeroTooltip(HakkeroTooltip inv) {
		this.inv = inv;
	}

	@Override
	public int getHeight() {
		return HakkeroTooltip.ROWS * 18 + 2;
	}

	@Override
	public int getWidth(Font font) {
		return 18 * HakkeroData.CHANNELS;
	}

	@Override
	public void renderImage(Font font, int mx, int my, GuiGraphics g) {
		List<ItemStack> list = inv.slots();
		for (int i = 0; i < Math.min(HakkeroData.CHANNELS * HakkeroTooltip.ROWS, list.size()); i++) {
			int x = mx + i % HakkeroData.CHANNELS * 18;
			int y = my + i / HakkeroData.CHANNELS * 18;
			renderSlot(font, x, y, g, list.get(i));
			// the input row is the first one, so the wipe says how far each channel has cooked. No
			// easing: rebuilt against a smooth client clock, so this value moves every frame.
			if (i < HakkeroData.CHANNELS) HakkeroProgressBar.draw(g, x + 1, y + 1, inv.progress(i));
		}
	}

	private static void renderSlot(Font font, int x, int y, GuiGraphics g, ItemStack stack) {
		g.blitSprite(SLOT, x, y, 18, 20);
		if (stack.isEmpty()) return;
		g.renderItem(stack, x + 1, y + 1, 0);
		g.renderItemDecorations(font, stack, x + 1, y + 1);
	}

}
