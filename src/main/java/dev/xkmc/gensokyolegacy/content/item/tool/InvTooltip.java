package dev.xkmc.gensokyolegacy.content.item.tool;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record InvTooltip(List<ItemStack> list, int w, int h) implements TooltipComponent {
}