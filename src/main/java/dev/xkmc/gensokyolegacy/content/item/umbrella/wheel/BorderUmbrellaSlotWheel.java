package dev.xkmc.gensokyolegacy.content.item.umbrella.wheel;

import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderSlot;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaSlots;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaSelectPacket;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
import dev.xkmc.l2itemselector.wheel.WheelContext;
import dev.xkmc.l2itemselector.wheel.WheelKeyHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class BorderUmbrellaSlotWheel implements WheelAdaptor<BorderSlotEntry> {

	private final ItemStack stack;

	public BorderUmbrellaSlotWheel(ItemStack stack) {
		this.stack = stack;
	}

	@Override
	public WheelKeyHandler getInputHandler() {
		return new UmbrellaWheelKeyHandler();
	}

	@Override
	public List<BorderSlotEntry> getWheelContent() {
		var slots = stack.getOrDefault(GLItems.UMBRELLA_SLOTS.get(), BorderUmbrellaSlots.defaultSlots());
		List<BorderSlotEntry> list = new ArrayList<>();
		for (int i = 0; i < BorderUmbrellaSlots.MAX_SLOTS; i++) {
			var slot = slots.slots()[i];
			if (slot == null) slot = BorderSlot.empty();
			list.add(new BorderSlotEntry(i, slot));
		}
		return list;
	}

	@Override
	public int getIndex(Player player) {
		ItemStack held = BorderUmbrellaSelectionListener.getHeldUmbrella(player);
		if (held == null) return -1;
		return held.getOrDefault(GLItems.UMBRELLA_SLOT_SELECTED.get(), 0);
	}

	@Override
	public void select(int index) {
		GensokyoLegacy.HANDLER.toServer(new BorderUmbrellaSelectPacket(1, index));
		stack.set(GLItems.UMBRELLA_SLOT_SELECTED.get(), Math.floorMod(index, BorderUmbrellaSlots.MAX_SLOTS));
	}

	@Override
	public void renderIcon(GuiGraphics g, int x0, int y0, boolean left, float sideWidth, boolean hover) {
		// wheel icon for position wheel: 2x map + 1x selected position icon
		float cx = left ? sideWidth / 2f : g.guiWidth() - sideWidth / 2f;
		float r = Math.min((float) x0 / 1.5f, (float) y0) / 1.5f;
		float s = r * 0.025f;
		float r0 = Math.min(sideWidth / 2f, r * 0.75f) * (hover ? 0.3f : 0.15f);
		if (r0 > 4) {
			g.pose().pushPose();
			g.pose().translate(cx, y0, 0);
			g.pose().scale(s, s, s);
			var slot = BorderUmbrellaItem.getSelectedSlotData(stack);
			ItemStack icon = slot.isEmptySlot() ? new ItemStack(Items.COMPASS) : slot.displayIcon();
			if (icon.isEmpty()) icon = new ItemStack(Items.COMPASS);
			g.renderItem(icon, -8, -8);
			g.pose().popPose();
		}
		if (hover) {
			int cx2 = left ? (int) (sideWidth / 2) : g.guiWidth() - (int) (sideWidth / 2);
			int ty = y0 + (int) r0 * 2;
			var font = Minecraft.getInstance().font;
			for (var line : font.split(GLLang.ItemUmbrella.WHEEL_TARGET.get(), (int) (sideWidth - 4))) {
				g.drawString(font, line, cx2 - font.width(line) / 2, ty, 0xFFFF55, true);
				ty += font.lineHeight + 1;
			}
			var slot = BorderUmbrellaItem.getSelectedSlotData(stack);
			var text = slot.isEmptySlot() ? GLLang.ItemUmbrella.SLOT_EMPTY_ITEM.get() : slot.displayName();
			for (var line : font.split(text, (int) (sideWidth - 4))) {
				g.drawString(font, line, cx2 - font.width(line) / 2, ty, 0xffffff, true);
				ty += font.lineHeight + 1;
			}
		}
	}

	@Override
	public void renderImpl(GuiGraphics g, Player player, List<BorderSlotEntry> list, WheelContext ctx) {
		WheelAdaptor.super.renderImpl(g, player, list, ctx);
		int index = ctx.hover() >= 0 ? ctx.hover() : ctx.sel();
		if (index < 0) index = getIndex(player);
		if (index < 0 || index >= list.size()) return;
		var entry = list.get(index);
		int x0 = g.guiWidth() / 2, y0 = g.guiHeight() / 2;
		float r = Math.min(x0 / 1.5f, y0) / 1.5f;
		float s = r * 0.02f;
		// selected position icon in the middle
		g.pose().pushPose();
		g.pose().translate(x0, y0, 0);
		g.pose().scale(s, s, s);
		ItemStack icon = entry.slot().isEmptySlot() ? ItemStack.EMPTY : entry.slot().displayIcon();
		g.renderItem(icon, -8, -8);
		g.pose().popPose();
		var text = entry.slot().displayName();
		var font = Minecraft.getInstance().font;
		int y = (int) (y0 + s * 8);
		for (var line : font.split(text, (int) r)) {
			g.drawString(font, line, x0 - font.width(line) / 2, y, 0xffffff, true);
			y += font.lineHeight + 1;
		}
		// show coords if not empty
		if (!entry.slot().isEmptySlot()) {
			var coord = Component.literal(entry.slot().pos().getX() + ", " + entry.slot().pos().getY() + ", " + entry.slot().pos().getZ());
			for (var line : font.split(coord, (int) r)) {
				g.drawString(font, line, x0 - font.width(line) / 2, y, 0xAAAAAA, true);
				y += font.lineHeight + 1;
			}
		}
	}
}