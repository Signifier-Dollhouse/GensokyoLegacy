package dev.xkmc.gensokyolegacy.content.item.glove.client;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Doll loadout readout in menu order (main, off, core, cloth), a display-only carrier like {@code InvTooltip};
 * {@link DollClientLoadoutTooltip} renders it as the menu's cross instead of a row.
 *
 * @see "doc/design/doll/loadout.md"
 */
public record DollLoadoutTooltip(ItemStack main, ItemStack off, ItemStack core, ItemStack cloth) implements TooltipComponent {
}
