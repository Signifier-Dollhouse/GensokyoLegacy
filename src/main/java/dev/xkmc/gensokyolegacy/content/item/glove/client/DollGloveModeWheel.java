package dev.xkmc.gensokyolegacy.content.item.glove.client;

import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.glove.network.DollGloveSelectPacket;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.wheel.PersistentWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class DollGloveModeWheel implements PersistentWheel<DollGloveModeEntry> {

	private final ItemStack stack;

	public DollGloveModeWheel(ItemStack stack) {
		this.stack = stack;
	}

	@Override
	public boolean isValid(Player player) {
		var held = DollGloveSelectionListener.getHeldGlove(player);
		return held != null && held == stack || (held != null && held.getItem() instanceof DollGloveItem);
	}

	@Override
	public List<DollGloveModeEntry> getWheelContent() {
		var mc = Minecraft.getInstance();
		if (mc.player == null) return List.of();
		return DollGloveClientModes.available(mc.player, DollGloveItem.getMode(stack))
				.stream().map(DollGloveModeEntry::new).toList();
	}

	@Override
	public int getIndex(Player player) {
		ItemStack held = DollGloveSelectionListener.getHeldGlove(player);
		if (held == null) return -1;
		var mode = DollGloveItem.getMode(held);
		return DollGloveClientModes.available(player, mode).indexOf(mode);
	}

	@Override
	public void select(int index) {
		var mc = Minecraft.getInstance();
		if (mc.player == null) return;
		var avail = DollGloveClientModes.available(mc.player, DollGloveItem.getMode(stack));
		if (index < 0 || index >= avail.size()) return;
		int ordinal = avail.get(index).ordinal();
		GensokyoLegacy.HANDLER.toServer(new DollGloveSelectPacket(0, ordinal));
		// optimistic update
		stack.set(GLItems.DOLL_GLOVE_MODE.get(), ordinal);
	}

	@Override
	public void renderIcon(GuiGraphics g, int x0, int y0, boolean left, float sideWidth, boolean hover) {
		// wheel icon for main wheel: glove item
		ItemStack icon = new ItemStack(GLItems.DOLL_GLOVE.get());
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
			var text = DollGloveItem.getMode(stack).displayName();
			for (var line : font.split(text, (int) (sideWidth - 4))) {
				g.drawString(font, line, cx2 - font.width(line) / 2, ty, 0xffffff, true);
				ty += font.lineHeight + 1;
			}
		}
	}

}
