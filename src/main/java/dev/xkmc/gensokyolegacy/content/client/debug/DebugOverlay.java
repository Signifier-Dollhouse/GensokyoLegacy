package dev.xkmc.gensokyolegacy.content.client.debug;

import dev.xkmc.l2library.base.overlay.OverlayUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

public class DebugOverlay implements IGuiOverlay {

	@Override
	public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
		if (Minecraft.getInstance().screen != null) return;
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		var level = player.level();
		List<Component> lines = new ArrayList<>();
		long time = level.getGameTime();
		for (ItemStack stack : player.getHandSlots()) {
			if (stack.getItem() instanceof IDebugOverlayWand wand) {
				wand.addTooltip(player, stack, lines, time);
			}
		}
		for (ItemStack stack : player.getArmorSlots()) {
			if (stack.getItem() instanceof IDebugOverlayWand wand) {
				wand.addTooltip(player, stack, lines, time);
			}
		}
		if (lines.isEmpty()) return;
		int sw = g.guiWidth();
		int sh = g.guiHeight();
		new OverlayUtil(g, (int) (sw * 0.6), (int) (sh * 0.5), (int) (sw * 0.3))
				.renderLongText(Minecraft.getInstance().font, lines);
	}

}
