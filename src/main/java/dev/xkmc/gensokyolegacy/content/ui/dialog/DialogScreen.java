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

	private static final ResourceLocation FRAME = GensokyoLegacy.loc("dialogue/box");
	private static final ResourceLocation BOX_BG = GensokyoLegacy.loc("dialogue/box_bg");
	private static final ResourceLocation OPTION = GensokyoLegacy.loc("dialogue/option");
	private static final ResourceLocation AVATAR = GensokyoLegacy.loc("dialogue/avatar");
	private static final ResourceLocation AVATAR_BG = GensokyoLegacy.loc("dialogue/avatar_bg");

	private static final int SCREEN_H = 384;

	private static final int BOX_H = 160;

	// horizontal breathing room between the screen edges and the box; everything
	// pinned to the box (text, options, avatar) moves with it
	private static final int BOX_PAD_X = 48;

	private static final float TEXT_SCALE = 1.5F;
	private static final int TEXT_PAD_TOP = 40;
	private static final int TEXT_PAD_X = 60;

	// AVATAR: overall size relative to the texture, left edge offset from the
	// box's left edge, how far the frame's top edge sits ABOVE the box top at
	// scale 1.0 (the distance itself scales with AVATAR_SCALE: the unit shrinks
	// toward the box top, so the dip into the box stays proportional), and a
	// vertical nudge
	private static final float AVATAR_SCALE = 0.8F;
	private static final int AVATAR_X = 0;
	private static final int AVATAR_ABOVE = 150;
	private static final int AVATAR_NUDGE_Y = 1;

	// OPTIONS: gap between the bottom option's bottom edge and the box top,
	// gap between options, horizontal/vertical padding around the label,
	// minimum box width, and the right margin from the screen right edge
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
		// guards the two divisions below if the screen was never sized
		if (s <= 0.0F) return;
		float dw = this.width / s;

		var body = menu.getBodyText();
		boolean framed = body.isPresent();
		boolean avatar = framed && menu.character != null;

		// never let the margin squeeze the box out on extremely narrow windows
		float padX = Math.min(BOX_PAD_X, dw * 0.2F);
		float boxX = padX;
		float boxW = dw - 2 * padX;
		float boxY = SCREEN_H - BOX_H;
		// distance above the box scales with the avatar, so the unit shrinks
		// toward the box top instead of sinking into it by the full height loss
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
			// the option is drawn in a pose scaled by artScale while its target size
			// is divided by it, so the nine-slice border lands at OPT_BORDER * artScale
			// = optPadY pixels: the frame tracks the text padding instead of
			// overflowing it, and stays unclamped since ohs >= 2 * optPadY
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

		g.pose().pushPose();
		g.pose().scale(s, s, 1.0F);

		if (framed) {
			blitBlend(g, BOX_BG, Math.round(boxX), Math.round(boxY), 0, Math.round(boxW), BOX_H);
			blitBlend(g, FRAME, Math.round(boxX), Math.round(boxY), 0, Math.round(boxW), BOX_H);
			var lines = font.split(body.get(), Math.max(1, Math.round((boxW - 2 * TEXT_PAD_X) / TEXT_SCALE)));
			drawLines(g, lines, boxX + TEXT_PAD_X, textTop, TEXT_COLOR);
		}

		if (n > 0) {
			// laid out from a top anchor: when framed the lowest option ends up
			// OPT_BOTTOM_GAP above the box, otherwise the stack is centred
			float stackTop = framed ? boxY - OPT_BOTTOM_GAP - totalH : (SCREEN_H - totalH) / 2.0F;
			// options hug the box's right edge, and the pose is scaled by s, so
			// hit testing works in design space as well
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

	/**
	 * blitSprite goes through the colorless innerBlit, which does not manage GL
	 * blend state; translucent sprite pixels turn opaque unless blend is enabled.
	 */
	private static void blitBlend(GuiGraphics g, ResourceLocation sprite, int x, int y, int z, int w, int h) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		g.blitSprite(sprite, x, y, z, w, h);
		RenderSystem.disableBlend();
	}

	private void renderAvatar(GuiGraphics g, float s, float boxX, float avatarY, int mx, int my) {
		var ch = menu.character;
		if (ch == null) return;
		// all avatar geometry carries AVATAR_SCALE so frame, window and entity
		// shrink together around the same top-left corner
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
