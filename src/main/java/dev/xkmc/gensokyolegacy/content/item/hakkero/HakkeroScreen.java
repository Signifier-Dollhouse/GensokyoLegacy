package dev.xkmc.gensokyolegacy.content.item.hakkero;

import dev.xkmc.l2library.base.menu.base.BaseContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/**
 * Screen of the finished Mini Hakkero. The grid itself is the generated sprite; this adds the mode
 * name under it, the furnace flame over the fuel slot, and the cooldown overlay on the input slots.
 */
public class HakkeroScreen extends BaseContainerScreen<HakkeroMenu> {

	/**
	 * Vanilla's furnace texture, which holds the lit-progress flame at its top-right.
	 */
	private static final ResourceLocation FLAME = new ResourceLocation("textures/gui/container/furnace.png");

	public HakkeroScreen(HakkeroMenu menu, Inventory plInv, Component title) {
		super(menu, plInv, title);
	}

	@Override
	protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
		// ease the overlay toward whatever the last data slot update said, so the wipe moves
		// smoothly instead of stepping once per server tick
		menu.advanceInterpolation();
		menu.sprite.get().getRenderer(this).start(g);
		renderFuel(g);
	}

	/**
	 * The furnace flame on the fuel slot, drawn upward the way vanilla does it: the top of it
	 * creeps down as the fuel burns down, and nothing is drawn while no fuel is lit.
	 */
	private void renderFuel(GuiGraphics g) {
		float progress = menu.getBurnProgress();
		if (progress <= 0.0F) return;
		int height = Mth.ceil(progress * 13.0F) + 1;
		var slot = menu.sprite.get().getComp("fuel");
		g.blit(FLAME, leftPos + slot.x, topPos + slot.y - height, 176, 14 - height, 14, height, 256, 256);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		var mode = Component.translatable(menu.getMode().block().getDescriptionId());
		// just under the grid, whose lowest row of slots is the outer rim's "out5"
		int y = menu.sprite.get().getComp("out5").y + 20;
		g.drawString(font, mode, (imageWidth - font.width(mode)) / 2, y, 0x404040, false);
		// the slot pose is still translated to the GUI origin here, so slot.x/slot.y are the
		// right coordinates for the wipe that 1.20.1 has no renderSlotContents hook for
		for (Slot slot : menu.slots) {
			if (slot instanceof HakkeroMenu.InputSlot input)
				HakkeroProgressBar.draw(g, slot.x, slot.y, menu.getSmoothProgress(input.channel()));
		}
	}

	@Override
	protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
		if (slot instanceof HakkeroMenu.ModeSlot && clickType == ClickType.PICKUP) {
			click(HakkeroMenu.MODE_BUTTON);
			return;
		}
		super.slotClicked(slot, slotId, mouseButton, clickType);
	}

}
