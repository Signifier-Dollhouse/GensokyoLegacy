package dev.xkmc.gensokyolegacy.content.item.anchor;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What the anchor does on a right-click: break, grapple, or call a storm. Stored on the stack as a
 * component; each mode selects one of the throw behaviours in {@link AnchorThrow}.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public enum AnchorMode {
	DESTROY(GLLang.ItemAnchor.MODE_DESTROY),
	VOYAGE(GLLang.ItemAnchor.MODE_VOYAGE),
	STORM(GLLang.ItemAnchor.MODE_STORM);

	private static final AnchorMode[] VALUES = values();

	private final GLLang.ItemAnchor name;

	AnchorMode(GLLang.ItemAnchor name) {
		this.name = name;
	}

	public Component getDisplayName() {
		return name.get();
	}

	/**
	 * A selector icon for this mode, named after it. Every mode is the anchor stack itself; the mode
	 * component picks the flat GUI icon through {@link AnchorItem#displayPredicate}.
	 */
	public ItemStack icon() {
		ItemStack stack = new ItemStack(GLItems.MINAMITSU_ANCHOR.get());
		AnchorItem.setMode(stack, this);
		stack.set(DataComponents.ITEM_NAME, getDisplayName());
		return stack;
	}

	public static AnchorMode byId(int id) {
		return id < 0 || id >= VALUES.length ? DESTROY : VALUES[id];
	}

}
