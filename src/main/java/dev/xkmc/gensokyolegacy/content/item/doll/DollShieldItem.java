package dev.xkmc.gensokyolegacy.content.item.doll;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * An item a doll can raise as a shield. The vanilla shield qualifies by being a
 * {@link ShieldItem}; this marker is the other way round — a mini shield is not a vanilla shield,
 * so a player cannot block with it (item.md §10).
 */
public interface DollShieldItem {

	/**
	 * Whether a stack is something a doll can block with: the vanilla shield, or an item marked
	 * {@link DollShieldItem}. The one place the two kinds are told apart from.
	 */
	static boolean isShield(ItemStack stack) {
		return !stack.isEmpty() && (stack.getItem() instanceof ShieldItem || stack.getItem() instanceof DollShieldItem);
	}

}
