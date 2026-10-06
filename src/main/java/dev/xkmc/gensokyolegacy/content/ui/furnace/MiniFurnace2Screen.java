package dev.xkmc.gensokyolegacy.content.ui.furnace;

import dev.xkmc.l2core.base.menu.base.BaseContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Screen of the finished Mini Hakkero. The grid itself is the generated sprite; this only
 * adds the mode name under it and the cooldown style overlay on the input slots, which is
 * how the smelting progress of each channel is shown.
 */
public class MiniFurnace2Screen extends BaseContainerScreen<MiniFurnace2Menu> {

	public MiniFurnace2Screen(MiniFurnace2Menu menu, Inventory plInv, Component title) {
		super(menu, plInv, title);
	}

	@Override
	protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
		getRenderer().start(g);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		var mode = Component.translatable(menu.getMode().block().getDescriptionId());
		// just under the grid, whose lowest row of slots is the outer rim's "out5"
		int y = menu.getLayout().getComp("out5").y + 20;
		g.drawString(font, mode, (imageWidth - font.width(mode)) / 2, y, 0x404040, false);
	}

	@Override
	protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
		if (slot instanceof MiniFurnace2Menu.ModeSlot && clickType == ClickType.PICKUP) {
			click(MiniFurnace2Menu.MODE_BUTTON);
			return;
		}
		super.slotClicked(slot, slotId, mouseButton, clickType);
	}

	@Override
	protected void renderSlotContents(GuiGraphics g, ItemStack stack, Slot slot, @Nullable String countString) {
		super.renderSlotContents(g, stack, slot, countString);
		if (!(slot instanceof MiniFurnace2Menu.InputSlot input)) return;
		float f = menu.getProgress(input.getSlotIndex());
		if (f <= 0.0F) return;
		int y0 = slot.y + Mth.floor(16.0F * (1.0F - f));
		int y1 = y0 + Mth.ceil(16.0F * f);
		g.fill(RenderType.guiOverlay(), slot.x, y0, slot.x + 16, y1, Integer.MAX_VALUE);
	}

}
