package dev.xkmc.gensokyolegacy.content.item.glove.client;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Doll loadout readout in menu order (main, off, core, cloth). Display-only
 * carrier, same role as {@code InvTooltip}; rendering lives in
 * {@link DollClientLoadoutTooltip}, which arranges the four slots as the
 * cross from the loadout menu instead of a row.
 */
public record DollLoadoutTooltip(ItemStack main, ItemStack off, ItemStack core, ItemStack cloth) implements TooltipComponent {
}
