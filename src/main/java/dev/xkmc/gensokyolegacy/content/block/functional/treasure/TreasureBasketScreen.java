package dev.xkmc.gensokyolegacy.content.block.functional.treasure;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Large-chest layout for the treasure basket: the vanilla 6-row texture, no extra widgets.
 *
 * @see "doc/design/treasure_basket.md"
 */
public class TreasureBasketScreen extends AbstractContainerScreen<TreasureBasketMenu> {

	private static final ResourceLocation CONTAINER_BACKGROUND =
			ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

	public TreasureBasketScreen(TreasureBasketMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		this.imageHeight = 114 + TreasureBasketMenu.ROWS * 18;
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int left = (this.width - this.imageWidth) / 2;
		int top = (this.height - this.imageHeight) / 2;
		g.blit(CONTAINER_BACKGROUND, left, top, 0, 0, this.imageWidth, TreasureBasketMenu.ROWS * 18 + 17);
		g.blit(CONTAINER_BACKGROUND, left, top + TreasureBasketMenu.ROWS * 18 + 17, 0, 126, this.imageWidth, 96);
	}

}
