package dev.xkmc.gensokyolegacy.content.item.glove.client;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.l2library.base.overlay.OverlayUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Hover readout (glove.md §2b): while holding the glove with no screen open, hovering a doll shows its name
 * plus the loadout in the menu's cross arrangement, as ModularGolems' equipment overlay does. Stacks come
 * from the doll's synced client mirror and the doll from {@code GloveDollHover}, never the target cache.
 *
 * @see "doc/design/doll/glove.md"
 */
public class DollGloveOverlay implements IGuiOverlay {

	@Override
	public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
		var mc = Minecraft.getInstance();
		if (mc.screen != null || mc.player == null) return;
		DollEntity doll = GloveDollHover.hoveredDoll();
		if (doll == null) return;
		List<ClientTooltipComponent> tip = new ArrayList<>();
		tip.add(ClientTooltipComponent.create(doll.getDisplayName().getVisualOrderText()));
		tip.add(new DollClientLoadoutTooltip(new DollLoadoutTooltip(
				doll.getLoadoutItem(DollSlot.MAIN_HAND), doll.getLoadoutItem(DollSlot.OFF_HAND),
				doll.getLoadoutItem(DollSlot.CORE), doll.getLoadoutItem(DollSlot.CLOTH))));
		// right half: left edge at 60% of the width, quarter-screen wrap; -1 keeps the
		// default vertical centering.
		new OverlayUtil(g, Math.round(g.guiWidth() * 0.6f), -1, Math.round(g.guiWidth() * 0.25f)).renderTooltipInternal(mc.font, tip);
	}

}
