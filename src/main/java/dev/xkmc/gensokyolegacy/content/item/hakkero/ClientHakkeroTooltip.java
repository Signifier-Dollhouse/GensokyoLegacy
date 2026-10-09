package dev.xkmc.gensokyolegacy.content.item.hakkero;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Client rendering of the finished Mini Hakkero's tooltip image.
 *
 * <p>Same layout as the menu: a row of eight input slots with a cooldown wipe over each one
 * showing how far that channel has cooked, then the eight results, then the fuel. The
 * contents and progress come from the same simulated state, so the image shows where the
 * hakkero has got to even though the server only refreshes it every {@code IDLE_BATCH} ticks.
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
			// the input row is the first one; the wipe says how far each channel has cooked.
			// No easing here: the record was rebuilt against a smoothly-moving client clock
			// (see Hakkero.getTooltipImage), so this value already moves every frame.
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
