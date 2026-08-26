package dev.xkmc.gensokyolegacy.content.item.umbrella.data;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record BorderSlot(
		BlockPos pos,
		ResourceLocation dim,
		String name,
		ItemStack icon,
		boolean isEmpty
) {

	public BorderSlot() {
		this(BlockPos.ZERO, ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"), "", ItemStack.EMPTY, true);
	}

	public BorderSlot(BlockPos pos, ResourceLocation dim, String name, ItemStack icon) {
		this(pos, dim, name, icon, false);
	}

	public static BorderSlot empty() {
		return new BorderSlot();
	}

	public boolean isEmptySlot() {
		return isEmpty;
	}

	public ItemStack displayIcon() {
		if (isEmpty) return ItemStack.EMPTY;
		return icon.isEmpty() ? new ItemStack(Items.BARRIER) : icon;
	}

	public Component displayName() {
		if (isEmpty) return GLLang.UMBRELLA$SLOT_EMPTY.get();
		if (name == null || name.isEmpty()) {
			return Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " [" + dim + "]");
		}
		return Component.literal(name);
	}
}