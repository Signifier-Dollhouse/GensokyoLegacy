package dev.xkmc.gensokyolegacy.content.item.debug;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;

/**
 * The five ways the interior wand reads a block click. Stored as an ordinal in the wand's data
 * component and picked from the item selector.
 *
 * @see "doc/design/structure/interior_wand.md"
 */
public enum InteriorWandMode {

	BOUND(GLLang.ItemDebug.INTERIOR_MODE_BOUND),
	BOUND_EDIT(GLLang.ItemDebug.INTERIOR_MODE_BOUND_EDIT),
	WAYPOINT(GLLang.ItemDebug.INTERIOR_MODE_WAYPOINT),
	DUAL_WAYPOINT(GLLang.ItemDebug.INTERIOR_MODE_DUAL_WAYPOINT),
	EXPORT(GLLang.ItemDebug.INTERIOR_MODE_EXPORT);

	private final GLLang.ItemDebug name;

	InteriorWandMode(GLLang.ItemDebug name) {
		this.name = name;
	}

	public Component displayName() {
		return name.get();
	}

}
