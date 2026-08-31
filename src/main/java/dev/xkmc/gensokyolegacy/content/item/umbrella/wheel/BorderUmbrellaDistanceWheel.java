package dev.xkmc.gensokyolegacy.content.item.umbrella.wheel;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
import dev.xkmc.l2itemselector.wheel.WheelContext;
import dev.xkmc.l2itemselector.wheel.WheelKeyHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaSelectPacket;

public class BorderUmbrellaDistanceWheel implements WheelAdaptor<BorderUmbrellaDistanceEntry> {

	private final ItemStack stack;

	public BorderUmbrellaDistanceWheel(ItemStack stack) {
		this.stack = stack;
	}

	@Override
	public WheelKeyHandler getInputHandler() {
		return new UmbrellaWheelKeyHandler();
	}

	@Override
	public List<BorderUmbrellaDistanceEntry> getWheelContent() {
		List<BorderUmbrellaDistanceEntry> list = new ArrayList<>();
		for (int d : BorderUmbrellaDistanceEntry.DISTANCES) {
			list.add(new BorderUmbrellaDistanceEntry(d));
		}
		return list;
	}

	@Override
	public int getIndex(Player player) {
		ItemStack held = BorderUmbrellaSelectionListener.getHeldUmbrella(player);
		if (held == null) return -1;
		int dist = held.getOrDefault(GLItems.UMBRELLA_DISTANCE.get(), 1000);
		return BorderUmbrellaDistanceEntry.indexOf(dist);
	}

	@Override
	public void select(int index) {
		GensokyoLegacy.HANDLER.toServer(new BorderUmbrellaSelectPacket(2, index));
		int dist = BorderUmbrellaDistanceEntry.distanceOf(index);
		stack.set(GLItems.UMBRELLA_DISTANCE.get(), dist);
	}

	@Override
	public void renderIcon(GuiGraphics g, int x0, int y0, boolean left, float sideWidth, boolean hover) {
		// side icon shows current distance
		int dist = stack.getOrDefault(GLItems.UMBRELLA_DISTANCE.get(), 1000);
		var entry = new BorderUmbrellaDistanceEntry(dist);
		ItemStack icon = entry.icon();
		float cx = left ? sideWidth / 2f : g.guiWidth() - sideWidth / 2f;
		float r = Math.min((float) x0 / 1.5f, (float) y0) / 1.5f;
		float s = r * 0.025f;
		float r0 = Math.min(sideWidth / 2f, r * 0.75f) * (hover ? 0.3f : 0.15f);
		if (r0 > 4) {
			g.pose().pushPose();
			g.pose().translate(cx, y0, 0);
			g.pose().scale(s, s, s);
			g.renderItem(icon, -8, -8);
			g.pose().popPose();
		}
		if (hover) {
			int cx2 = left ? (int) (sideWidth / 2) : g.guiWidth() - (int) (sideWidth / 2);
			int ty = y0 + (int) r0 * 2;
			var font = Minecraft.getInstance().font;
			for (var line : font.split(GLLang.ItemUmbrella.WHEEL_DISTANCE.get(), (int) (sideWidth - 4))) {
				g.drawString(font, line, cx2 - font.width(line) / 2, ty, 0xFFFF55, true);
				ty += font.lineHeight + 1;
			}
			var text = entry.displayName();
			for (var line : font.split(text, (int) (sideWidth - 4))) {
				g.drawString(font, line, cx2 - font.width(line) / 2, ty, 0xffffff, true);
				ty += font.lineHeight + 1;
			}
		}
	}

	@Override
	public void renderImpl(GuiGraphics g, Player player, List<BorderUmbrellaDistanceEntry> list, WheelContext ctx) {
		WheelAdaptor.super.renderImpl(g, player, list, ctx);
		int index = ctx.hover() >= 0 ? ctx.hover() : ctx.sel();
		if (index < 0) index = getIndex(player);
		if (index < 0 || index >= list.size()) return;
		int x0 = g.guiWidth() / 2, y0 = g.guiHeight() / 2;
		float r = Math.min(x0 / 1.5f, y0) / 1.5f;
		float s = r * 0.02f;
		g.pose().pushPose();
		g.pose().translate(x0, y0, 0);
		g.pose().scale(s, s, s);
		g.pose().translate(0, -8, 0);
		var entry = list.get(index);
		ItemStack icon = entry.icon();
		g.renderItem(icon, -8, -8);
		g.pose().popPose();
		var text = entry.displayName();
		var font = Minecraft.getInstance().font;
		int y = (int) (y0 + s * 3);
		for (var line : font.split(text, (int) r)) {
			g.drawString(font, line, x0 - font.width(line) / 2, y, 0xffffff, true);
			y += font.lineHeight + 1;
		}
	}
}
