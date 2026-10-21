package dev.xkmc.gensokyolegacy.content.client.deco;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;

/**
 * Client-only helpers, kept in one class so the server never has to resolve client types.
 */
public final class ClientTooltip {

	private ClientTooltip() {
	}

	public static boolean isShiftDown() {
		return Screen.hasShiftDown();
	}

	/**
	 * Game time plus the frame's partial tick; {@link Double#NaN} when there is no level yet.
	 */
	public static double smoothGameTime(Level level) {
		var client = Minecraft.getInstance();
		if (client.level == null) return Double.NaN;
		return level.getGameTime() + client.getTimer().getGameTimeDeltaPartialTick(true);
	}

}
