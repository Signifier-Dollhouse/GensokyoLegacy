package dev.xkmc.gensokyolegacy.content.ui.trade;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.content.rpg.trade.IClientOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.ui.util.SpriteButton;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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

public class TradeScreen extends AbstractContainerScreen<TradeMenu> {

	/**
	 * Open/close animation frames, trade_1.png (fully open) to trade_5.png (lid shut).
	 * Opening plays 5 -> 1, closing plays 1 -> 5; each frame lasts {@link #FRAME_MS}.
	 */
	private static final ResourceLocation[] FRAMES = new ResourceLocation[5];
	private static final int FRAME_MS = 20; // 2 ticks per frame

	private static final ResourceLocation PRICE_SELL = GensokyoLegacy.loc("trade/price_sell");
	private static final ResourceLocation PRICE_WANT = GensokyoLegacy.loc("trade/price_want");
	private static final ResourceLocation PREV_NORMAL = GensokyoLegacy.loc("trade/prev_normal");
	private static final ResourceLocation PREV_HOVER = GensokyoLegacy.loc("trade/prev_hover");
	private static final ResourceLocation PREV_PRESSED = GensokyoLegacy.loc("trade/prev_pressed");
	private static final ResourceLocation NEXT_NORMAL = GensokyoLegacy.loc("trade/next_normal");
	private static final ResourceLocation NEXT_HOVER = GensokyoLegacy.loc("trade/next_hover");
	private static final ResourceLocation NEXT_PRESSED = GensokyoLegacy.loc("trade/next_pressed");

	static {
		for (int i = 0; i < FRAMES.length; i++)
			FRAMES[i] = GensokyoLegacy.loc("textures/gui/container/trade_" + (i + 1) + ".png");
	}

	private Button prevButton, nextButton;

	private long timer = Util.getMillis();
	private boolean closing;
	private int closeStart;

	public TradeScreen(TradeMenu cont, Inventory plInv, Component title) {
		super(cont, plInv, title);
		imageWidth = 252;
		imageHeight = 220;
		playChestSound(SoundEvents.CHEST_OPEN);
	}

	@Override
	protected void init() {
		super.init();
		int y = topPos + 6;
		prevButton = addRenderableWidget(new SpriteButton(PREV_NORMAL, PREV_HOVER, PREV_PRESSED,
				leftPos + 95, y, 16, 16, b -> click(-1)));
		nextButton = addRenderableWidget(new SpriteButton(NEXT_NORMAL, NEXT_HOVER, NEXT_PRESSED,
				leftPos + 139, y, 16, 16, b -> click(-2)));
	}

	private int currentFrame() {
		long steps = (Util.getMillis() - timer) / FRAME_MS;
		if (closing) return Math.min(FRAMES.length, closeStart + (int) steps);
		return Math.max(1, FRAMES.length - (int) steps);
	}

	private boolean isOpen() {
		return !closing && currentFrame() == 1;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		if (closing && Util.getMillis() - timer >= (FRAMES.length - closeStart + 1) * (long) FRAME_MS) {
			closeNow();
			return;
		}
		super.render(g, mx, my, pt);
		this.renderTooltip(g, mx, my);
	}

	@Override
	public void onClose() {
		if (closing) { // repeated ESC while closing: skip remaining animation
			closeNow();
			return;
		}
		closeStart = currentFrame();
		timer = Util.getMillis();
		closing = true;
		playChestSound(SoundEvents.CHEST_CLOSE);
	}

	private static void playChestSound(SoundEvent sound) {
		var mc = Minecraft.getInstance();
		if (mc.player != null)
			mc.player.playSound(sound, 0.5F, 0.9F + mc.player.getRandom().nextFloat() * 0.1F);
	}

	private void closeNow() {
		if (minecraft != null) {
			if (this.minecraft.player != null)
				this.minecraft.player.closeContainer();
			this.minecraft.setScreen(null);
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (!isOpen()) return false;
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!isOpen() && keyCode != InputConstants.KEY_ESCAPE) return false;
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
		boolean ready = currentFrame() == 1;
		boolean showPages = ready && menu.getMaxPage() > 1;
		if (prevButton != null) prevButton.visible = showPages;
		if (nextButton != null) nextButton.visible = showPages;
		for (var ts : menu.getTradeSlots()) ts.active = ready;
		g.blit(FRAMES[currentFrame() - 1], leftPos, topPos, 0, 0, 256, 220);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		if (currentFrame() != 1) return;
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
			boolean sell = offer.value().isSellOffer();
			var tag = sell ? PRICE_SELL : PRICE_WANT;
			int tw = sell ? 31 : 30;
			int x = ts.x + 9 - tw / 2;
			int y = ts.y + 17 - (12 - font.lineHeight) / 2;
			g.blitSprite(tag, x, y, tw, 12);
			g.drawString(font, price, ts.x + 9 - font.width(price) / 2, ts.y + 19, 0xFFFFFF, true);
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

	@Override
	protected void renderSlotContents(GuiGraphics g, ItemStack stack, Slot slot, @Nullable String countString) {
		if (countString == null && slot instanceof TradeSlot ts && ts.hasItem()) {
			var offer = getPlayerSellOffer(ts);
			if (offer != null) {
				int seed = slot.x + slot.y * this.imageWidth;
				if (slot.isFake()) {
					g.renderFakeItem(stack, slot.x, slot.y, seed);
				} else {
					g.renderItem(stack, slot.x, slot.y, seed);
				}
				renderSellDecorations(g, stack, slot, offer.value().canTrade(menu.player));
				return;
			}
		}
		super.renderSlotContents(g, stack, slot, countString);
	}

	@Nullable
	private Holder<TradeOffer> getPlayerSellOffer(TradeSlot ts) {
		var offerId = GLItems.DC_OFFER.get(ts.getItem());
		if (offerId == null) return null;
		var offer = CodecRegistry.TRADE.get(menu.player.level().registryAccess(), offerId);
		if (offer == null || offer.value().isSellOffer()) return null;
		return offer;
	}

	private void renderSellDecorations(GuiGraphics g, ItemStack stack, Slot slot, boolean enough) {
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

	private static Component actionText(TradeOffer offer) {
		var character = bracket(offer.character().getDescription(), ChatFormatting.AQUA);
		if (!offer.isSellOffer()) {
			var entry = offer.ingredients().getFirst();
			return GLLang.Trade.SELL.get(bracket(ingredientName(entry), ChatFormatting.YELLOW), character);
		}
		if (IClientOffer.resolve(offer).currency().isEmpty()) {
			return GLLang.Trade.CRAFT.get(character, bracket(offer.result().getHoverName(), ChatFormatting.YELLOW));
		}
		return GLLang.Trade.BUY.get(bracket(offer.result().getHoverName(), ChatFormatting.YELLOW), character);
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
		if (!isOpen()) return;
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
					if (offer.value().ingredients().size() > 1) {
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
