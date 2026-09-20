package dev.xkmc.gensokyolegacy.content.item.glove.client;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.l2itemselector.overlay.OverlayUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Hover readout (glove.md §2b): while holding the glove in any mode with no
 * screen open, hovering a doll shows its name plus the loadout in the menu's
 * cross arrangement, the same presentation as ModularGolems' equipment overlay
 * ({@code GolemStatusOverlay} + {@code GolemEquipmentTooltip}). Loadout stacks
 * come from the doll's synced client mirror, so this is display-only. The
 * hovered doll comes from the vanilla crosshair via {@code GloveDollHover},
 * never the target cache.
 */
public class DollGloveOverlay implements LayeredDraw.Layer {

	@Override
	public void render(GuiGraphics g, DeltaTracker delta) {
		var mc = Minecraft.getInstance();
		if (mc.screen != null || mc.player == null) return;
		DollEntity doll = GloveDollHover.hoveredDoll();
		if (doll == null) return;
		List<ClientTooltipComponent> tip = new ArrayList<>();
		tip.add(ClientTooltipComponent.create(doll.getDisplayName().getVisualOrderText()));
		tip.add(new DollClientLoadoutTooltip(new DollLoadoutTooltip(
				doll.getLoadoutItem(DollSlot.MAIN_HAND), doll.getLoadoutItem(DollSlot.OFF_HAND),
				doll.getLoadoutItem(DollSlot.CORE), doll.getLoadoutItem(DollSlot.CLOTH))));
		// right half: left edge at the right half's midpoint; y -1 keeps the
		// default vertical centering, maxW -1 the default quarter-screen wrap.
		new OverlayUtil(g, Math.round(g.guiWidth() * 0.6f), -1, Math.round(g.guiWidth() * 0.25f)).renderTooltipInternal(mc.font, tip);
	}

}
