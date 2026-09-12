package dev.xkmc.gensokyolegacy.content.ui.dialog;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.ui.quest.QuestInfo;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2itemselector.overlay.TextBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
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

	private static final ResourceLocation FRAME = GensokyoLegacy.loc("dialogue/frame");
	private static final ResourceLocation BG = GensokyoLegacy.loc("dialogue/bg");
	private static final ResourceLocation OPTION = GensokyoLegacy.loc("dialogue/option");
	private static final ResourceLocation AVATAR = GensokyoLegacy.loc("dialogue/avatar");
	private static final ResourceLocation AVATAR_BG = GensokyoLegacy.loc("dialogue/avatar_bg");

	private static final int SCREEN_H = 384;

	private static final int BOX_H = 160;

	private static final int BG_SIZE = 128;
	private static final int BG_SPLIT = 32;

	private static final int[] BG_LEFT = {-1, -1, -1, -1, -1, 30, 23, 22, 21, 20, 20, 18, 18, 17, 17,
			16, 16, 16, 16, 16, 15, 15, 14, 14, 14, 13, 13, 12, 13, 13, 13, 13, 13, 13, 13, 13, 12, 12, 11,
			10, 9, 8, 8, 8, 8, 8, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 13, 10, 8, 7, 8, -1, -1, -1, -1, -1, -1};
	private static final int[] BG_RIGHT = {-1, -1, -1, -1, -1, 30, 23, 22, 21, 20, 20, 18, 18, 17, 17,
			16, 16, 16, 16, 16, 15, 15, 14, 14, 14, 13, 13, 12, 13, 13, 13, 13, 13, 13, 13, 13, 12, 12, 11,
			10, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 17, 14, -1, -1, -1, -1, -1, -1};

	private static final int FRAME_BORDER_TOP = 47;
	private static final int FRAME_BORDER_BOTTOM = 25;

	private static final int[] BG_CLIP = new int[BOX_H];

	static {
		for (int i = 0; i < BOX_H; i++) {
			if (i < FRAME_BORDER_TOP)
				BG_CLIP[i] = i;
			else if (i < BOX_H - FRAME_BORDER_BOTTOM)
				BG_CLIP[i] = FRAME_BORDER_TOP + (i - FRAME_BORDER_TOP) *
						(BG_SIZE - FRAME_BORDER_TOP - FRAME_BORDER_BOTTOM) /
						(BOX_H - FRAME_BORDER_TOP - FRAME_BORDER_BOTTOM);
			else
				BG_CLIP[i] = BG_SIZE - FRAME_BORDER_BOTTOM + (i - (BOX_H - FRAME_BORDER_BOTTOM));
		}
	}

	private static final int BOX_PAD_X = 48;

	private static final float TEXT_SCALE = 1.5F;
	private static final int TEXT_PAD_TOP = 40;
	private static final int TEXT_PAD_X = 60;

	private static final float AVATAR_SCALE = 0.8F;
	private static final int AVATAR_X = 0;
	private static final int AVATAR_ABOVE = 150;
	private static final int AVATAR_NUDGE_Y = 1;

	private static final int OPT_BOTTOM_GAP = 6;
	private static final int OPT_GAP = 4;
	private static final int OPT_PAD_X = 14;
	private static final float OPT_PAD_Y = 9.0F;
	private static final int MIN_OPT_W = 50;
	private static final int OPT_RIGHT_PAD = 4;

	private static final int AVATAR_W = 195;
	private static final int AVATAR_H = 189;

	private static final int AVATAR_WIN_X = 18;
	private static final int AVATAR_WIN_Y = 42;
	private static final int AVATAR_WIN_W = 121;
	private static final int AVATAR_WIN_H = 102;

	private static final int OPT_BORDER = 24;

	private static final int TEXT_COLOR = 0xFFFFFF;
	private static final int HOVER_COLOR = 0xFFE9A8;
	private static final int HOVER_FILL = 0x30FFFFFF;

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
		float s = this.height / (float) SCREEN_H;
		if (s <= 0.0F) return;
		float dw = this.width / s;

		var body = menu.getBodyText();
		boolean framed = body.isPresent();
		boolean avatar = framed && menu.character != null;

		float padX = Math.min(BOX_PAD_X, dw * 0.2F);
		float boxX = padX;
		float boxW = dw - 2 * padX;
		float boxY = SCREEN_H - BOX_H;
		float avatarY = boxY - AVATAR_ABOVE * AVATAR_SCALE + AVATAR_NUDGE_Y;
		float textTop = boxY + TEXT_PAD_TOP;

		var options = menu.getOptions();
		int n = options.size();
		sel = -1;

		List<List<FormattedCharSequence>> optLines = new ArrayList<>(n);
		int[] ows = new int[n];
		int[] ohs = new int[n];
		float textH = font.lineHeight * TEXT_SCALE;
		float optPadY = OPT_PAD_Y;
		float artScale = 1.0F;
		int sumH = 0;
		if (n > 0) {
			float avail = framed ? boxY - OPT_BOTTOM_GAP : SCREEN_H / 2.0F;
			float needed = n * (textH + 2 * optPadY) + (n - 1) * OPT_GAP;
			if (needed > avail)
				optPadY = Math.max(1.0F, (avail - n * textH - (n - 1) * OPT_GAP) / (2.0F * n));
			artScale = Mth.clamp(optPadY / OPT_BORDER, 0.25F, 1.0F);
			int maxTextW = Math.max(1, Math.round((boxW - 2 * OPT_PAD_X - OPT_RIGHT_PAD) / TEXT_SCALE));
			for (int i = 0; i < n; i++) {
				var lines = font.split(options.get(i), maxTextW);
				optLines.add(lines);
				int tw = 0;
				for (var line : lines) tw = Math.max(tw, font.width(line));
				ows[i] = Math.max(MIN_OPT_W, Math.round(tw * TEXT_SCALE) + 2 * OPT_PAD_X);
				ohs[i] = Math.round(lines.size() * textH + 2 * optPadY);
				sumH += ohs[i];
			}
		}
		float totalH = sumH + Math.max(0, n - 1) * OPT_GAP;

		// drawn unscaled so its quad edges can be pixel-snapped below
		if (framed)
			drawBoxBackground(g, s, Math.round(boxX), Math.round(boxY), Math.round(boxW), BOX_H);

		g.pose().pushPose();
		g.pose().scale(s, s, 1.0F);

		if (framed) {
			blitBlend(g, FRAME, Math.round(boxX), Math.round(boxY), 0, Math.round(boxW), BOX_H);
			var lines = font.split(body.get(), Math.max(1, Math.round((boxW - 2 * TEXT_PAD_X) / TEXT_SCALE)));
			drawLines(g, lines, boxX + TEXT_PAD_X, textTop, TEXT_COLOR);
		}

		if (n > 0) {
			float stackTop = framed ? boxY - OPT_BOTTOM_GAP - totalH : (SCREEN_H - totalH) / 2.0F;
			float boxRight = boxX + boxW;
			float dmx = mx / s;
			float dmy = my / s;
			for (int i = 0; i < n; i++) {
				int x = Math.round(boxRight - ows[i] - OPT_RIGHT_PAD);
				int y = Math.round(stackTop);
				g.pose().pushPose();
				g.pose().translate(x, y, 0);
				g.pose().scale(artScale, artScale, 1.0F);
				blitBlend(g, OPTION, 0, 0, 0, Math.round(ows[i] / artScale), Math.round(ohs[i] / artScale));
				g.pose().popPose();
				boolean hover = dmx >= x && dmx < x + ows[i] && dmy >= y && dmy < y + ohs[i];
				if (hover) {
					sel = i;
					g.fill(x, y, x + ows[i], y + ohs[i], 350, HOVER_FILL);
				}
				drawLinesCentered(g, optLines.get(i), x + ows[i] / 2.0F, y + optPadY,
						hover ? HOVER_COLOR : TEXT_COLOR);
				stackTop += ohs[i] + OPT_GAP;
			}
		}

		g.pose().popPose();

		if (avatar) renderAvatar(g, s, boxX, avatarY, mx, my);
	}

	private static void drawBoxBackground(GuiGraphics g, float scale, int x, int y, int w, int h) {
		int patternTop = y + h - (BG_SIZE - BG_SPLIT);
		drawZone(g, scale, x, y, w, y, patternTop, 0, BG_SPLIT);
		drawZone(g, scale, x, y, w, patternTop, y + h, BG_SPLIT, BG_SIZE - BG_SPLIT);
	}

	private static void drawZone(GuiGraphics g, float scale, int x, int y, int w, int dFrom, int dTo, int vBase, int vPeriod) {
		for (int d = dFrom, next; d < dTo; d = next) {
			int ins = bgInset(d - y, true);
			next = bgRunEnd(d, dTo, y);
			if (ins < 0) continue;
			int x0 = x + ins;
			int x1 = x + w - bgInset(d - y, false);
			for (int py = d; py < next; ) {
				int vOff = (py - dFrom) % vPeriod;
				int vh = Math.min(vPeriod - vOff, next - py);
				for (int px = x0; px < x1; ) {
					int u = (px - x) % BG_SIZE;
					int uw = Math.min(BG_SIZE - u, x1 - px);
					blitBg(g, scale, px, py, uw, vh, u, vBase + vOff);
					px += uw;
				}
				py += vh;
			}
		}
	}

	private static int bgInset(int i, boolean left) {
		int row = BG_CLIP[i];
		return left ? BG_LEFT[row] : BG_RIGHT[row];
	}

	private static int bgRunEnd(int from, int to, int base) {
		int left = bgInset(from - base, true);
		int right = bgInset(from - base, false);
		int d = from + 1;
		while (d < to && bgInset(d - base, true) == left && bgInset(d - base, false) == right)
			d++;
		return d;
	}

	private static void blitBg(GuiGraphics g, float scale, int px, int py, int uw, int vh, int u, int v) {
		// expand every quad to whole screen pixels: adjacent tiles overlap by up
		// to 1px (harmless, the bg draws opaque) instead of abutting on fractional
		// screen positions, which opens 1px world-colored seams at odd gui scales;
		// blitSprite only draws sprites 1:1, so restate its innerBlit here
		int x0 = Mth.floor(px * scale);
		int y0 = Mth.floor(py * scale);
		int x1 = Mth.ceil((px + uw) * scale);
		int y1 = Mth.ceil((py + vh) * scale);
		if (x1 <= x0 || y1 <= y0) return;
		var sprite = Minecraft.getInstance().getGuiSprites().getSprite(BG);
		RenderSystem.setShaderTexture(0, sprite.atlasLocation());
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.disableBlend();
		var matrix = g.pose().last().pose();
		var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.addVertex(matrix, x0, y0, 0).setUv(sprite.getU(u / (float) BG_SIZE), sprite.getV(v / (float) BG_SIZE));
		buffer.addVertex(matrix, x0, y1, 0).setUv(sprite.getU(u / (float) BG_SIZE), sprite.getV((v + vh) / (float) BG_SIZE));
		buffer.addVertex(matrix, x1, y1, 0).setUv(sprite.getU((u + uw) / (float) BG_SIZE), sprite.getV((v + vh) / (float) BG_SIZE));
		buffer.addVertex(matrix, x1, y0, 0).setUv(sprite.getU((u + uw) / (float) BG_SIZE), sprite.getV(v / (float) BG_SIZE));
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void blitBlend(GuiGraphics g, ResourceLocation sprite, int x, int y, int z, int w, int h) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		g.blitSprite(sprite, x, y, z, w, h);
		RenderSystem.disableBlend();
	}

	private void renderAvatar(GuiGraphics g, float s, float boxX, float avatarY, int mx, int my) {
		var ch = menu.character;
		if (ch == null) return;
		float as = AVATAR_SCALE * s;
		int fx = Math.round((boxX + AVATAR_X) * s);
		int fy = Math.round(avatarY * s);
		int fw = Math.round(AVATAR_W * as);
		int fh = Math.round(AVATAR_H * as);
		int wx = fx + Math.round(AVATAR_WIN_X * as);
		int wy = fy + Math.round(AVATAR_WIN_Y * as);
		int ww = Math.round(AVATAR_WIN_W * as);
		int wh = Math.round(AVATAR_WIN_H * as);
		blitBlend(g, AVATAR_BG, wx, wy, 0, ww, wh);
		float bh = ch.getBbHeight();
		int scale = Mth.clamp(Math.round(AVATAR_WIN_H / (bh * 0.6f) * as), 16, 256);
		InventoryScreen.renderEntityInInventoryFollowsMouse(g, wx, wy, wx + ww, wy + wh, scale, 0.35f * bh, mx, my, ch);
		blitBlend(g, AVATAR, fx, fy, 100, fw, fh);
	}

	private void drawLines(GuiGraphics g, List<FormattedCharSequence> lines, float x, float y, int color) {
		g.pose().pushPose();
		g.pose().translate(x, y, 400);
		g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
		int ly = 0;
		for (var line : lines) {
			g.drawString(font, line, 0, ly, color, true);
			ly += font.lineHeight;
		}
		g.pose().popPose();
	}

	private void drawLinesCentered(GuiGraphics g, List<FormattedCharSequence> lines, float cx, float y, int color) {
		g.pose().pushPose();
		g.pose().translate(cx, y, 400);
		g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
		int ly = 0;
		for (var line : lines) {
			g.drawString(font, line, -font.width(line) / 2, ly, color, true);
			ly += font.lineHeight;
		}
		g.pose().popPose();
	}

	protected void renderQuestInfo(GuiGraphics g, Optional<Holder<Quest>> quest) {
		if (quest.isEmpty()) return;
		var key = quest.get().unwrapKey().map(k -> k.location()).orElseThrow();
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
		new TextBox(g, 0, 1, 10, this.height / 2, (int) (this.width * 0.4f - 20))
				.renderLongText(font, text);
	}

}
