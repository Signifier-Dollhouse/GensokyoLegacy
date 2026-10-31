package dev.xkmc.gensokyolegacy.content.item.ghost;

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
 * The selector wheel for the {@link YuyukoFanItem}: one entry per fan mode, every mode always
 * listed.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public class YuyukoFanModeWheel implements PersistentWheel<ItemWheelEntry> {

	private final ItemStack stack;

	public YuyukoFanModeWheel(ItemStack stack) {
		this.stack = stack;
	}

	@Override
	public boolean isValid(Player player) {
		ItemStack held = YuyukoFanSelectionListener.getHeldFan(player);
		return held != null && held.getItem() instanceof YuyukoFanItem;
	}

	@Override
	public List<ItemWheelEntry> getWheelContent() {
		return Arrays.stream(YuyukoFanMode.values())
				.map(mode -> new ItemWheelEntry(YuyukoFanItem.iconStack(mode)))
				.toList();
	}

	@Override
	public int getIndex(Player player) {
		ItemStack held = YuyukoFanSelectionListener.getHeldFan(player);
		return held == null ? -1 : YuyukoFanItem.getMode(held).ordinal();
	}

	@Override
	public void select(int index) {
		YuyukoFanMode[] modes = YuyukoFanMode.values();
		if (index < 0 || index >= modes.length) return;
		GensokyoLegacy.HANDLER.toServer(new SelectorSelectPacket(index));
		// optimistic update, same as the other selector wheels
		YuyukoFanItem.setMode(stack, modes[index]);
	}

	@Override
	public void renderIcon(GuiGraphics g, int x0, int y0, boolean left, float sideWidth, boolean hover) {
		ItemStack icon = YuyukoFanItem.iconStack(YuyukoFanItem.getMode(stack));
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
