package dev.xkmc.gensokyolegacy.content.item.anchor;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What the anchor does: break, grapple, or call a storm. The mode picks what both clicks do and
 * which {@link AnchorThrow} the item spawns; it is stored on the stack as a component.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public enum AnchorMode {
	VOYAGE(GLLang.ItemAnchor.MODE_VOYAGE),
	DESTROY(GLLang.ItemAnchor.MODE_DESTROY),
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
	 * A wheel icon stack for this mode: named after it and marked so {@link AnchorItem#displayPredicate}
	 * picks the wheel icon range.
	 */
	public ItemStack icon() {
		return AnchorItem.iconStack(this);
	}

	public static AnchorMode byId(int id) {
		return id < 0 || id >= VALUES.length ? DESTROY : VALUES[id];
	}

}
