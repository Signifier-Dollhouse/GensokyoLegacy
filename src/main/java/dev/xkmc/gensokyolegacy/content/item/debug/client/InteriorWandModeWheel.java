package dev.xkmc.gensokyolegacy.content.item.debug.client;

import dev.xkmc.gensokyolegacy.content.item.debug.InteriorWandItem;
import dev.xkmc.gensokyolegacy.content.item.debug.InteriorWandMode;
import dev.xkmc.gensokyolegacy.content.item.debug.InteriorWandSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.common.network.SelectorSelectPacket;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2itemselector.wheel.ItemWheelEntry;
import dev.xkmc.l2itemselector.wheel.PersistentWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * The selector wheel for the {@link InteriorWandItem}: one entry per mode, every mode always
 * listed.
 *
 * @see "doc/design/structure/interior_wand.md"
 */
public class InteriorWandModeWheel implements PersistentWheel<ItemWheelEntry> {

	private final ItemStack stack;

	public InteriorWandModeWheel(ItemStack stack) {
		this.stack = stack;
	}

	@Override
	public boolean isValid(Player player) {
		return InteriorWandSelectionListener.getHeldWand(player) != null;
	}

	@Override
	public List<ItemWheelEntry> getWheelContent() {
		return Arrays.stream(InteriorWandMode.values())
				.map(mode -> new ItemWheelEntry(InteriorWandItem.iconStack(mode)))
				.toList();
	}

	@Override
	public int getIndex(Player player) {
		ItemStack held = InteriorWandSelectionListener.getHeldWand(player);
		return held == null ? -1 : InteriorWandItem.getMode(held).ordinal();
	}

	@Override
	public void select(int index) {
		InteriorWandMode[] modes = InteriorWandMode.values();
		if (index < 0 || index >= modes.length) return;
		GensokyoLegacy.HANDLER.toServer(new SelectorSelectPacket(index));
		// optimistic update, same as the other selector wheels
		InteriorWandItem.setMode(stack, modes[index]);
	}

	@Override
	public void renderIcon(GuiGraphics g, int x0, int y0, boolean left, float sideWidth, boolean hover) {
		ItemStack icon = InteriorWandItem.iconStack(InteriorWandItem.getMode(stack));
		float cx = left ? sideWidth / 2f : g.guiWidth() - sideWidth / 2f;
		float r = Math.min((float) x0 / 1.5f, (float) y0) / 1.5f;
		float rs = r * 0.025f;
		float r0 = Math.min(sideWidth / 2f, r * 0.75f) * (hover ? 0.3f : 0.15f);
		if (r0 > 4) {
			g.pose().pushPose();
			g.pose().translate(cx, y0, 0);
			g.pose().scale(rs, rs, rs);
			g.renderItem(icon, -8, -8);
			g.pose().popPose();
		}
		if (hover) {
			int cx2 = left ? (int) (sideWidth / 2) : g.guiWidth() - (int) (sideWidth / 2);
			int ty = y0 + (int) r0 * 2;
			var font = Minecraft.getInstance().font;
			var text = icon.getHoverName();
			for (var line : font.split(text, (int) (sideWidth - 4))) {
				g.drawString(font, line, cx2 - font.width(line) / 2, ty, 0xffffff, true);
				ty += font.lineHeight + 1;
			}
		}
	}

}
