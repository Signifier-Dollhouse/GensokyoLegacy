package dev.xkmc.gensokyolegacy.content.ui.trade;

import dev.xkmc.gensokyolegacy.content.item.currency.CoinPurse;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.content.rpg.trade.OfferKind;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.ui.util.SpriteButton;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ItemDecoratorHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Draws the offer board, its price tags and the filter/paging rail.
 *
 * @see "doc/design/trade_ui.md"
 */
public class TradeScreen extends AbstractContainerScreen<TradeMenu> {

	private static final ResourceLocation TEXTURE = GensokyoLegacy.loc("textures/gui/container/trade.png");

	private static final ResourceLocation PRICE_TRADE = GensokyoLegacy.loc("trade/price_trade");
	private static final ResourceLocation PRICE_BUYING = GensokyoLegacy.loc("trade/price_buying");
	private static final ResourceLocation PRICE_PROCESSING = GensokyoLegacy.loc("trade/price_processing");

	private static final ResourceLocation[] FILTER_NORMAL = {
			GensokyoLegacy.loc("trade/tab_trade_normal"),
			GensokyoLegacy.loc("trade/tab_buying_normal"),
			GensokyoLegacy.loc("trade/tab_processing_normal")
	};
	private static final ResourceLocation[] FILTER_HOVER = {
			GensokyoLegacy.loc("trade/tab_trade_hover"),
			GensokyoLegacy.loc("trade/tab_buying_hover"),
			GensokyoLegacy.loc("trade/tab_processing_hover")
	};
	private static final ResourceLocation[] FILTER_DISABLED = {
			GensokyoLegacy.loc("trade/tab_trade_disabled"),
			GensokyoLegacy.loc("trade/tab_buying_disabled"),
			GensokyoLegacy.loc("trade/tab_processing_disabled")
	};
	private static final GLLang.Trade[] FILTER_LANG = {
			GLLang.Trade.FILTER_TRADE,
			GLLang.Trade.FILTER_BUYING,
			GLLang.Trade.FILTER_PROCESSING
	};

	private static final ResourceLocation PREV_NORMAL = GensokyoLegacy.loc("trade/prev_normal");
	private static final ResourceLocation PREV_HOVER = GensokyoLegacy.loc("trade/prev_hover");
	private static final ResourceLocation PREV_DISABLED = GensokyoLegacy.loc("trade/prev_disabled");
	private static final ResourceLocation NEXT_NORMAL = GensokyoLegacy.loc("trade/next_normal");
	private static final ResourceLocation NEXT_HOVER = GensokyoLegacy.loc("trade/next_hover");
	private static final ResourceLocation NEXT_DISABLED = GensokyoLegacy.loc("trade/next_disabled");

	private static final int BUTTON_SIZE = 12;
	private static final int BUTTON_X = 217;
	private static final int FILTER_Y = 61;
	private static final int BUTTON_STEP = 14;
	private static final int NEXT_Y = 103;
	private static final int PREV_Y = 117;

	private static final int MONEY_X = 24;
	private static final int MONEY_Y = 75;
	private static final int MONEY_W = 13;

	private static final OfferKind[] KINDS = OfferKind.values();

	private final SpriteButton[] filterButtons = new SpriteButton[KINDS.length];
	private Button prevButton, nextButton;

	public TradeScreen(TradeMenu cont, Inventory plInv, Component title) {
		super(cont, plInv, title);
		imageWidth = 252;
		imageHeight = 240;
		playChestSound();
	}

	@Override
	protected void init() {
		super.init();
		for (int i = 0; i < KINDS.length; i++) {
			OfferKind kind = KINDS[i];
			var btn = new SpriteButton(FILTER_NORMAL[i], FILTER_HOVER[i], FILTER_DISABLED[i],
					leftPos + BUTTON_X, topPos + FILTER_Y + i * BUTTON_STEP, BUTTON_SIZE, BUTTON_SIZE,
					() -> menu.getFilter() == kind, b -> click(TradeMenu.filterButtonId(kind)));
			btn.setTooltip(Tooltip.create(FILTER_LANG[i].get()));
			filterButtons[i] = addRenderableWidget(btn);
		}
		nextButton = addRenderableWidget(new SpriteButton(NEXT_NORMAL, NEXT_HOVER, NEXT_DISABLED,
				leftPos + BUTTON_X, topPos + NEXT_Y, BUTTON_SIZE, BUTTON_SIZE, b -> click(TradeMenu.PAGE_NEXT)));
		prevButton = addRenderableWidget(new SpriteButton(PREV_NORMAL, PREV_HOVER, PREV_DISABLED,
				leftPos + BUTTON_X, topPos + PREV_Y, BUTTON_SIZE, BUTTON_SIZE, b -> click(TradeMenu.PAGE_PREV)));
	}

	private static void playChestSound() {
		var mc = Minecraft.getInstance();
		if (mc.player != null)
			mc.player.playSound(SoundEvents.CHEST_OPEN, 0.5F, 0.9F + mc.player.getRandom().nextFloat() * 0.1F);
	}

	@Override
	public void onClose() {
		var mc = Minecraft.getInstance();
		if (mc.player != null)
			mc.player.playSound(SoundEvents.CHEST_CLOSE, 0.5F, 0.9F + mc.player.getRandom().nextFloat() * 0.1F);
		super.onClose();
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		super.render(g, mx, my, pt);
		renderTooltip(g, mx, my);
	}

	@Override
	protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
		for (int i = 0; i < KINDS.length; i++) {
			if (filterButtons[i] != null) filterButtons[i].active = menu.isAvailable(KINDS[i]);
		}
		if (prevButton != null) prevButton.active = menu.getPage() > 0;
		if (nextButton != null) nextButton.active = menu.getPage() < menu.getMaxPage() - 1;
		g.blit(TEXTURE, leftPos, topPos, 0, 0, 256, 240);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		int money = CoinPurse.money(menu.player);
		drawMoney(g, money);
		if (menu.getMaxPage() > 1) {
			var text = Component.literal((menu.getPage() + 1) + "/" + menu.getMaxPage());
			int x = imageWidth / 2 - font.width(text) / 2;
			g.drawString(font, text, x, 29, 0xFFFFFF, true);
		}
		for (var ts : menu.getTradeSlots()) {
			if (!ts.hasItem()) continue;
			var offerId = GLItems.DC_OFFER.get(ts.getItem());
			if (offerId == null) continue;
			var offer = CodecRegistry.TRADE.get(menu.player.level().registryAccess(), offerId);
			if (offer == null) continue;
			var kind = offer.value().kind();
			int x = ts.x - 6;
			int y = ts.y + 17;
			g.blitSprite(tag(kind), x, y, 27, 12);
			var price = price(offer.value());
			if (price.isEmpty()) continue;
			var text = Component.literal("¥" + price.getCount());
			int color = kind == OfferKind.TRADE && price.getCount() > money ? 0xFF5555 : 0xFFFFFF;
			g.drawString(font, text, x + (27 - font.width(text)) / 2, y + 2, color, true);
		}
	}

	private void drawMoney(GuiGraphics g, int money) {
		var text = Component.literal(String.valueOf(money));
		int width = font.width(text);
		float scale = Math.min(1F, (float) MONEY_W / width);
		g.pose().pushPose();
		g.pose().translate(Math.round(MONEY_X + (MONEY_W - width * scale) / 2F), MONEY_Y, 0);
		g.pose().scale(scale, scale, 1);
		g.drawString(font, text, 0, 0, 0xFFFFFF, true);
		g.pose().popPose();
	}

	private static ResourceLocation tag(OfferKind kind) {
		return switch (kind) {
			case TRADE -> PRICE_TRADE;
			case BUYING -> PRICE_BUYING;
			case PROCESSING -> PRICE_PROCESSING;
		};
	}

	private static ItemStack price(TradeOffer offer) {
		return switch (offer.kind()) {
			case TRADE -> {
				ItemStack ans = ItemStack.EMPTY;
				for (var entry : offer.ingredients()) {
					for (var stack : entry.ingredient().getItems()) {
						if (stack.is(GLTagGen.CURRENCY)) {
							ans = stack.copyWithCount(entry.count());
							break;
						}
					}
					if (!ans.isEmpty()) break;
				}
				yield ans;
			}
			case BUYING -> offer.result();
			case PROCESSING -> ItemStack.EMPTY;
		};
	}

	@Override
	protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
		if (slot instanceof TradeSlot ts) {
			click(ts.getContainerSlot());
			return;
		}
		super.slotClicked(slot, slotId, mouseButton, clickType);
	}

	@Override
	protected void renderSlotContents(GuiGraphics g, ItemStack stack, Slot slot, @Nullable String countString) {
		if (slot instanceof TradeSlot ts && ts.hasItem()) {
			var offer = getBuyingOffer(ts);
			if (offer != null) {
				int seed = slot.x + slot.y * this.imageWidth;
				if (slot.isFake()) {
					g.renderFakeItem(stack, slot.x, slot.y, seed);
				} else {
					g.renderItem(stack, slot.x, slot.y, seed);
				}
				renderBuyingDecorations(g, stack, slot, offer.value().canTrade(menu.player));
				return;
			}
		}
		super.renderSlotContents(g, stack, slot, countString);
	}

	@Nullable
	private Holder<TradeOffer> getBuyingOffer(TradeSlot ts) {
		var offerId = GLItems.DC_OFFER.get(ts.getItem());
		if (offerId == null) return null;
		var offer = CodecRegistry.TRADE.get(menu.player.level().registryAccess(), offerId);
		if (offer == null || offer.value().kind() != OfferKind.BUYING) return null;
		return offer;
	}

	private void renderBuyingDecorations(GuiGraphics g, ItemStack stack, Slot slot, boolean enough) {
		int x = slot.x, y = slot.y;
		g.pose().pushPose();
		if (stack.getCount() != 1) {
			String s = String.valueOf(stack.getCount());
			g.pose().translate(0.0F, 0.0F, 200.0F);
			g.drawString(font, s, x + 19 - 2 - font.width(s), y + 6 + 3,
					enough ? 0x55FF55 : 0xFF5555, true);
		}
		if (stack.isBarVisible()) {
			int l = stack.getBarWidth();
			int i = stack.getBarColor();
			int j = x + 2;
			int k = y + 13;
			g.fill(RenderType.guiOverlay(), j, k, j + 13, k + 2, -16777216);
			g.fill(RenderType.guiOverlay(), j, k, j + l, k + 1, i | 0xFF000000);
		}
		LocalPlayer localplayer = this.minecraft.player;
		float f = localplayer == null ? 0.0F :
				localplayer.getCooldowns().getCooldownPercent(stack.getItem(),
						this.minecraft.getTimer().getGameTimeDeltaPartialTick(true));
		if (f > 0.0F) {
			int i1 = y + Mth.floor(16.0F * (1.0F - f));
			int j1 = i1 + Mth.ceil(16.0F * f);
			g.fill(RenderType.guiOverlay(), x, i1, x + 16, j1, Integer.MAX_VALUE);
		}
		g.pose().popPose();
		ItemDecoratorHandler.of(stack).render(g, font, stack, x, y);
	}

	protected boolean click(int btn) {
		if (menu.clickMenuButton(menu.player, btn) && Minecraft.getInstance().gameMode != null) {
			Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, btn);
			return true;
		} else {
			return false;
		}
	}

	public static Component actionText(TradeOffer offer) {
		var character = bracket(offer.character().getDescription(), ChatFormatting.AQUA);
		return switch (offer.kind()) {
			case TRADE -> GLLang.Trade.BUY.get(bracket(offer.result().getHoverName(), ChatFormatting.YELLOW), character);
			case BUYING -> GLLang.Trade.SELL.get(bracket(ingredientName(offer.ingredients().getFirst()), ChatFormatting.YELLOW), character);
			case PROCESSING -> GLLang.Trade.CRAFT.get(character, bracket(offer.result().getHoverName(), ChatFormatting.YELLOW));
		};
	}

	/** Priced offers already show their cost - buying in the action line, trade as a {@code ¥N} tag - so only
	 * processing or multi-ingredient offers need the list. See {@link #renderLabels}. */
	private static boolean showIngredients(TradeOffer offer) {
		return offer.kind() == OfferKind.PROCESSING || offer.ingredients().size() > 1;
	}

	private static Component bracket(Component inner, ChatFormatting color) {
		return Component.literal("[").append(inner.copy().withStyle(color)).append("]");
	}

	private static Component ingredientName(IngredientEntry entry) {
		if (entry.text().isPresent()) return Component.literal(entry.text().get());
		var items = entry.ingredient().getItems();
		if (items.length == 0) return Component.literal("?");
		return items[0].getHoverName().copy();
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
					list.add(actionText(offer.value()));
					list.add(GLLang.Trade.STOCK.get(data.getRemainingTrades(menu.player, offer), data.getMaxTrades(offer)));
					if (showIngredients(offer.value())) {
						list.add(GLLang.Trade.INGREDIENTS.get());
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
