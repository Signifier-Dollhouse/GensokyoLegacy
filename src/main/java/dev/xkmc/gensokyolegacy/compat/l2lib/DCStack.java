package dev.xkmc.gensokyolegacy.compat.l2lib;

import net.minecraft.world.item.ItemStack;

public final class DCStack {

	private final ItemStack stack;
	private final int hashCode;

	public DCStack(ItemStack stack) {
		this.stack = stack;
		hashCode = stack.hashCode();
	}

	public ItemStack stack() {
		return stack;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DCStack s && hashCode == s.hashCode && stack.equals(s.stack);
	}

}
