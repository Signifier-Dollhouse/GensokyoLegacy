package dev.xkmc.gensokyolegacy.content.ui.trade;

import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.trade.IClientOffer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.Optional;

public class TradeScreen extends AbstractContainerScreen<TradeMenu> {

	private static final ResourceLocation TEXTURE = GensokyoLegacy.loc("textures/gui/container/trade.png");

	public TradeScreen(TradeMenu cont, Inventory plInv, Component title) {
		super(cont, plInv, title);
		imageWidth = 252;
		imageHeight = 220;
	}

	private Button prevButton, nextButton;

	@Override
	protected void init() {
		super.init();
		int y = topPos + 6;
		prevButton = addRenderableWidget(new Button.Builder(Component.literal("<"), b -> click(-1))
				.pos(leftPos + 95, y).size(16, 16).build());
		nextButton = addRenderableWidget(new Button.Builder(Component.literal(">"), b -> click(-2))
				.pos(leftPos + 139, y).size(16, 16).build());
	}

	@Override
	protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
		boolean showPages = menu.getMaxPage() > 1;
		if (prevButton != null) prevButton.visible = showPages;
		if (nextButton != null) nextButton.visible = showPages;
		g.blit(TEXTURE, leftPos, topPos, 0, 0, 256, 220);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		super.render(g, mx, my, pt);
		this.renderTooltip(g, mx, my);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		if (menu.getMaxPage() > 1) {
			var text = Component.literal((menu.getPage() + 1) + "/" + menu.getMaxPage());
			int x = 124 - font.width(text) / 2;
			int y = 9;
			g.drawString(font, text, x, y, 0x404040, false);
		}
		for (var ts : menu.getTradeSlots()) {
			if (!ts.hasItem()) continue;
			var offerId = GLItems.DC_OFFER.get(ts.getItem());
			if (offerId == null) continue;
			var offer = CodecRegistry.TRADE.get(menu.player.level().registryAccess(), offerId);
			if (offer == null) continue;
			var currency = IClientOffer.resolve(offer.value()).currency();
			if (currency.isEmpty()) continue;
			var price = Component.literal("¥" + currency.getCount());
			int x = ts.x + 9 - font.width(price) / 2;
			int y = ts.y + 19;
			g.drawString(font, price, x, y, 0xFFFFFF, true);
		}
	}

	@Override
	protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
		if (slot instanceof TradeSlot ts) {
			click(ts.getContainerSlot());
			return;
		}
		super.slotClicked(slot, slotId, mouseButton, clickType);
	}

	protected boolean click(int btn) {
		if (menu.clickMenuButton(menu.player, btn) && Minecraft.getInstance().gameMode != null) {
			Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, btn);
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void renderTooltip(GuiGraphics g, int x, int y) {
		if (menu.getCarried().isEmpty() && hoveredSlot instanceof TradeSlot ts && ts.hasItem()) {
			var stack = ts.getItem();
			var offerId = GLItems.DC_OFFER.get(stack);
			if (offerId != null) {
				var offer = CodecRegistry.TRADE.get(menu.player.level().registryAccess(), offerId);
				if (offer != null) {
					var data = GLMeta.TRADE.type().getOrCreate(menu.player);
					var list = new ArrayList<Component>();
					list.add(GLLang.TRADE$STOCK.get(data.getRemainingTrades(menu.player, offer), data.getMaxTrades(offer)));
					if (offer.value().ingredients().size() > 1) {
						list.add(GLLang.TRADE$INGREDIENTS.get());
						for (var entry : offer.value().ingredients())
							list.add(entry.getDesc(menu.player));
					}
					g.renderTooltip(font, list, Optional.empty(), x, y);
					return;
				}
			}
		}
		super.renderTooltip(g, x, y);
	}

}
