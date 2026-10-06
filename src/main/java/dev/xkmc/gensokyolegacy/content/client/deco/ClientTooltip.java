package dev.xkmc.gensokyolegacy.content.client.deco;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only helpers for code that has to stay loadable on a dedicated server.
 *
 * <p>A method that merely <i>returns</i> a client type still costs the class verifier
 * enough to resolve it, which is what makes naming {@code Minecraft.level} (a
 * {@code ClientLevel}) from an item class break server startup. Going through here keeps
 * every client type behind one class that the server never loads.
 */
public final class ClientTooltip {

	private ClientTooltip() {
	}

	@OnlyIn(Dist.CLIENT)
	public static boolean isShiftDown() {
		return Screen.hasShiftDown();
	}

	/**
	 * The game time as a smooth, continuously moving value.
	 *
	 * <p>{@code Level.getGameTime()} only steps once per tick, so anything reading it to
	 * advance a display moves twenty times a second and looks frozen in between — which is
	 * what a smelting progress overlay must not do. Adding the frame's partial tick makes
	 * the value move every frame instead, so a client that replays elapsed ticks from it
	 * produces a continuously changing picture.
	 *
	 * @return the fractional game time, or {@link Double#NaN} when there is no level yet
	 */
	@OnlyIn(Dist.CLIENT)
	public static double smoothGameTime(Level level) {
		var client = Minecraft.getInstance();
		if (client.level == null) return Double.NaN;
		return level.getGameTime() + client.getTimer().getGameTimeDeltaPartialTick(true);
	}

}
