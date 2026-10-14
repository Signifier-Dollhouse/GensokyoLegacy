package dev.xkmc.gensokyolegacy.content.item.hakkero;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server-side carrier for the finished Mini Hakkero's tooltip image: its contents plus the smelting
 * progress of each channel. Contents are already simulated when this is built, so the progress
 * belongs to that same state. {@code progress} is {@link HakkeroData#CHANNELS} permille, matching
 * the menu's data slots.
 */
public record HakkeroTooltip(List<ItemStack> slots, int[] progress)
		implements TooltipComponent {

	/** Progress is carried in permille, matching the menu's data slots. */
	public static final int SCALE = 1000;
	/** Rows of eight: the inputs, the results, then the fuel. */
	public static final int ROWS = 3;

	/** Progress of one channel, as a 0..1 fraction. */
	public float progress(int channel) {
		if (channel < 0 || channel >= progress.length) return 0.0F;
		return progress[channel] / (float) SCALE;
	}

}
