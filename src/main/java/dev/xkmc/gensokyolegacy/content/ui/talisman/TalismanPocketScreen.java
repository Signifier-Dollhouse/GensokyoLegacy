package dev.xkmc.gensokyolegacy.content.ui.talisman;

import dev.xkmc.l2core.base.menu.base.BaseContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TalismanPocketScreen extends BaseContainerScreen<TalismanPocketMenu> {

	public TalismanPocketScreen(TalismanPocketMenu cont, Inventory plInv, Component title) {
		super(cont, plInv, title);
	}

	@Override
	protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
		getRenderer().start(g);
	}

}