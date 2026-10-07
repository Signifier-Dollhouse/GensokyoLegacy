package dev.xkmc.gensokyolegacy.content.item.hakkero;

import dev.xkmc.l2core.base.menu.base.BaseContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Screen of the finished Mini Hakkero. The grid itself is the generated sprite; this adds
 * the mode name under it, the furnace flame over the fuel slot, and the cooldown style
 * overlay on the input slots, which is how each channel's progress is shown.
 */
public class HakkeroScreen extends BaseContainerScreen<HakkeroMenu> {

	/** Vanilla's furnace flame, blitted from the GUI atlas exactly as the furnace screen does. */
	private static final ResourceLocation FLAME =
			ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");

	public HakkeroScreen(HakkeroMenu menu, Inventory plInv, Component title) {
		super(menu, plInv, title);
	}

	@Override
	protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
		// ease the overlay toward whatever the last data slot update said, so the wipe moves
		// smoothly instead of stepping once per server tick
		menu.advanceInterpolation();
		getRenderer().start(g);
		renderFuel(g);
	}

	/**
	 * The furnace flame on the fuel slot, drawn upward the way vanilla does it: the icon is
	 * 14 tall and the top of it creeps down as the fuel burns down. Nothing is drawn while
	 * no fuel is lit.
	 */
	private void renderFuel(GuiGraphics g) {
		float progress = menu.getBurnProgress();
		if (progress <= 0.0F) return;
		int height = Mth.ceil(progress * 13.0F) + 1;
		var slot = menu.getLayout().getComp("fuel");
		g.blitSprite(FLAME, 14, 14, 0, 14 - height, leftPos + slot.x, topPos + slot.y - height, 14, height);
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
		if (slot instanceof HakkeroMenu.ModeSlot && clickType == ClickType.PICKUP) {
			click(HakkeroMenu.MODE_BUTTON);
			return;
		}
		super.slotClicked(slot, slotId, mouseButton, clickType);
	}

	@Override
	protected void renderSlotContents(GuiGraphics g, ItemStack stack, Slot slot, @Nullable String countString) {
		super.renderSlotContents(g, stack, slot, countString);
		if (!(slot instanceof HakkeroMenu.InputSlot input)) return;
		HakkeroProgressBar.draw(g, slot.x, slot.y, menu.getSmoothProgress(input.channel()));
	}

}
