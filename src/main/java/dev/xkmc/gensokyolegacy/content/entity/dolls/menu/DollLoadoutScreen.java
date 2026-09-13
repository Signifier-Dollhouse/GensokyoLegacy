package dev.xkmc.gensokyolegacy.content.entity.dolls.menu;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.l2core.base.menu.base.BaseContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DollLoadoutScreen extends BaseContainerScreen<DollLoadoutMenu> {

	public DollLoadoutScreen(DollLoadoutMenu cont, Inventory plInv, Component title) {
		super(cont, plInv, title);
	}

	@Override
	protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
		var sr = getRenderer();
		sr.start(g);
		sr.draw(g, "main", "slot", -1, -1);
		sr.draw(g, "off", "slot", -1, -1);
		sr.draw(g, "core", "slot", -1, -1);
		sr.draw(g, "cloth", "slot", -1, -1);
		if (menu.getLoadoutSlot("main").getItem().isEmpty())
			sr.draw(g, "main", "slotbg_main", 0, 0);
		if (menu.getLoadoutSlot("off").getItem().isEmpty())
			sr.draw(g, "off", "slotbg_off", 0, 0);
		if (menu.getLoadoutSlot("core").getItem().isEmpty())
			sr.draw(g, "core", "slotbg_core", 0, 0);
		if (menu.getLoadoutSlot("cloth").getItem().isEmpty())
			sr.draw(g, "cloth", "slotbg_cloth", 0, 0);
		renderPreview(g, mx, my);
	}

	private void renderPreview(GuiGraphics g, int mx, int my) {
		DollEntity doll = menu.getDoll();
		if (doll == null) return;
		int x = leftPos + 30;
		int y = topPos + 39;
		double lx = x - mx;
		double ly = y - my;
		float ax = (float) Math.atan(lx / 50.0);
		float ay = (float) Math.atan(ly / 50.0);
		InventoryScreen.renderEntityInInventoryFollowsAngle(g,
				leftPos + 3, topPos + 16, leftPos + 58, topPos + 62,
				40, 0.0625f, ax, ay, doll);
	}

}
