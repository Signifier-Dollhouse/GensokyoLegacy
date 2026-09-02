package dev.xkmc.gensokyolegacy.content.ui.dialog;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.ui.quest.QuestInfo;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2itemselector.overlay.TextBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DialogScreen<T extends DialogMenu> extends AbstractContainerScreen<T> {

	private static final ResourceLocation FRAME = GensokyoLegacy.loc("community/community");
	private static final ResourceLocation OPTION = GensokyoLegacy.loc("community/options");
	private static final ResourceLocation AVATAR = GensokyoLegacy.loc("community/avatar");

	// nine-slice borders, must match the .png.mcmeta next to the textures
	private static final int BORDER = 13;
	private static final int OPT_BORDER = 4;

	// avatar.png native size, its transparent window inset and bottom name plate
	private static final int AVATAR_W = 65;
	private static final int AVATAR_H = 75;
	private static final int AVATAR_WIN = 3;
	private static final int NAME_PLATE = 13;

	private static final int PAD = 3;
	private static final int OPT_GAP = 3;
	private static final int OPT_FLOAT = 8;
	private static final int MIN_OPT_W = 60;

	private static final int TEXT_COLOR = 0xFFFFFF;
	private static final int HOVER_COLOR = 0xFFE9A8;
	private static final int HOVER_FILL = 0x30FFFFFF;
	private static final int NAME_COLOR = 0x6B2038;

	protected int sel = -1;

	public DialogScreen(T menu, Inventory inv, Component title) {
		super(menu, inv, title);
	}

	@Override
	public void renderTransparentBackground(GuiGraphics g) {

	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
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
	public boolean mouseClicked(double mx, double my, int btn) {
		if (sel >= 0) {
			return click(sel);
		}
		return super.mouseClicked(mx, my, btn);
	}

	@Override
	protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
		int sw = g.guiWidth();
		int sh = g.guiHeight();
		var body = menu.getBodyText();
		boolean framed = body.isPresent();

		// dialog frame at bottom center, avatar unit pinned to its top-left corner
		int boxX = 0, boxY = 0, boxW = 0;
		if (framed) {
			boolean avatar = menu.character != null;
			boxW = (int) (sw * 0.7f);
			int padL = avatar ? AVATAR_W + 9 : BORDER + PAD;
			int padR = BORDER + PAD;
			var lines = font.split(body.get(), boxW - padL - padR);
			int textH = lines.size() * font.lineHeight;
			int boxH = Math.max(textH + 2 * (BORDER + PAD),
					Math.max(avatar ? AVATAR_H + BORDER + PAD : 0, (int) (sh * 0.2f)));
			boxX = (sw - boxW) / 2;
			boxY = sh - 10 - boxH;
			blitBlend(g, FRAME, boxX, boxY, 0, boxW, boxH);
			drawLines(g, lines, boxX + padL, boxY + BORDER + PAD, TEXT_COLOR);
			if (avatar) renderAvatar(g, boxX + 1, boxY + 1, mx, my);
		}

		// option boxes, stacked above the frame, right edge flush with the frame
		sel = -1;
		var options = menu.getOptions();
		int n = options.size();
		if (n == 0) return;
		int maxW = (framed ? boxW : sw / 2) - 2 * (OPT_BORDER + 2);
		List<List<FormattedCharSequence>> optLines = new ArrayList<>(n);
		int[] ws = new int[n];
		int[] hs = new int[n];
		int totalH = 0;
		for (int i = 0; i < n; i++) {
			var lines = font.split(options.get(i), maxW);
			optLines.add(lines);
			int w = MIN_OPT_W;
			for (var line : lines) w = Math.max(w, font.width(line) + 2 * (OPT_BORDER + 2));
			ws[i] = w;
			hs[i] = lines.size() * font.lineHeight + 2 * OPT_BORDER;
			if (i > 0) totalH += OPT_GAP;
			totalH += hs[i];
		}
		int y = framed ? boxY - OPT_FLOAT - totalH : (sh - totalH) / 2;
		for (int i = 0; i < n; i++) {
			int x = framed ? boxX + boxW - ws[i] : (sw - ws[i]) / 2;
			blitBlend(g, OPTION, x, y, 0, ws[i], hs[i]);
			boolean hover = mx >= x && mx < x + ws[i] && my >= y && my < y + hs[i];
			if (hover) {
				sel = i;
				g.fill(x, y, x + ws[i], y + hs[i], 350, HOVER_FILL);
			}
			drawLinesCentered(g, optLines.get(i), x + ws[i] / 2, y + OPT_BORDER, hover ? HOVER_COLOR : TEXT_COLOR);
			y += hs[i] + OPT_GAP;
		}
	}

	/**
	 * blitSprite goes through the colorless innerBlit, which does not manage GL
	 * blend state; translucent sprite pixels turn opaque unless blend is enabled.
	 */
	private static void blitBlend(GuiGraphics g, ResourceLocation sprite, int x, int y, int z, int w, int h) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		g.blitSprite(sprite, x, y, z, w, h);
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
	}

	private void renderAvatar(GuiGraphics g, int x, int y, int mx, int my) {
		var ch = menu.character;
		if (ch == null) return;
		// entity bust view, clipped to the transparent window of the frame
		int wx1 = x + AVATAR_WIN;
		int wy1 = y + AVATAR_WIN;
		int wx2 = x + AVATAR_W - AVATAR_WIN;
		int wy2 = y + AVATAR_H - NAME_PLATE - 1;
		float bh = ch.getBbHeight();
		// zoom so the upper ~60% of the body fills the window; 0.35*bh pivots chest to center
		int scale = Mth.clamp(Math.round((wy2 - wy1) / (bh * 0.6f)), 24, 192);
		InventoryScreen.renderEntityInInventoryFollowsMouse(g, wx1, wy1, wx2, wy2, scale, 0.35f * bh, mx, my, ch);
		blitBlend(g, AVATAR, x, y, 100, AVATAR_W, AVATAR_H);
		var name = menu.getSpeakerName();
		if (name.isPresent()) {
			var seq = font.split(name.get(), AVATAR_W - 2 * AVATAR_WIN - 2);
			if (!seq.isEmpty()) {
				var text = seq.get(0);
				g.drawString(font, text, x + (AVATAR_W - font.width(text)) / 2,
						y + AVATAR_H - NAME_PLATE + 2, NAME_COLOR, false);
			}
		}
	}

	private void drawLines(GuiGraphics g, List<FormattedCharSequence> lines, int x, int y, int color) {
		g.pose().pushPose();
		g.pose().translate(0, 0, 400);
		for (var line : lines) {
			g.drawString(font, line, x, y, color, true);
			y += font.lineHeight;
		}
		g.pose().popPose();
	}

	private void drawLinesCentered(GuiGraphics g, List<FormattedCharSequence> lines, int cx, int y, int color) {
		g.pose().pushPose();
		g.pose().translate(0, 0, 400);
		for (var line : lines) {
			g.drawString(font, line, cx - font.width(line) / 2, y, color, true);
			y += font.lineHeight;
		}
		g.pose().popPose();
	}

	protected void renderQuestInfo(GuiGraphics g, Optional<Holder<Quest>> quest) {
		if (quest.isEmpty()) return;
		var key = quest.get().unwrapKey().orElseThrow().location();
		var data = GLMeta.QUEST.type().getOrCreate(menu.player);
		QuestInfo info;
		List<Component> text;
		if (data.hasStarted(key)) {
			info = new QuestInfo(quest.get().value(), data.getData(key));
			text = info.getSideBarText(menu.player);
		} else {
			info = new QuestInfo(quest.get().value(), null);
			text = info.getPreviewText();
		}
		new TextBox(g, 0, 1, 10, g.guiHeight() / 2, (int) (g.guiWidth() * 0.4f - 20))
				.renderLongText(font, text);
	}

}
