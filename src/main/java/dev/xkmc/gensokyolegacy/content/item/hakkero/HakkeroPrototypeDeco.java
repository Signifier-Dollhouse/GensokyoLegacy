package dev.xkmc.gensokyolegacy.content.item.hakkero;

import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Draws each of the prototype's four cooking neighbours' progress over its slot in the
 * player inventory. Client-only, so nothing on the common side may reference it.
 */
public class HakkeroPrototypeDeco {

	public static void renderSlot(GuiGraphics g, ItemStack stack, Inventory inv, int i, int x, int y) {
		if (i < 9 || i >= 36) return;
		int r = (i - 9) / 9;
		int c = i % 9;
		renderImpl(g, inv, 0, x, y, r, c + 1, r, c);
		renderImpl(g, inv, 1, x, y, r + 1, c, r, c);
		renderImpl(g, inv, 2, x, y, r, c - 1, r, c);
		renderImpl(g, inv, 3, x, y, r - 1, c, r, c);
	}

	private static void renderImpl(GuiGraphics g, Inventory inv, int i, int x, int y, int r0, int c0, int r1, int c1) {
		if (r0 < 0 || r0 >= 3 || c0 < 0 || c0 >= 9) return;
		int s0 = r0 * 9 + c0 + 9;
		var stack = inv.getItem(s0);
		if (!stack.is(GLItems.MINI_HAKKERO_PROTOTYPE)) return;
		var data = stack.getOrDefault(GLItems.DC_HAKKERO_PROTOTYPE, HakkeroPrototype.Data.DEF);
		if (data.state() == HakkeroMode.OFF) return;

		var entry = data.data()[i];
		if (entry == null) return;
		HakkeroProgressBar.draw(g, x, y, 1f * entry.time() / entry.max());
	}

}
