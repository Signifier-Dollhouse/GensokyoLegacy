package dev.xkmc.gensokyolegacy.content.item.doll;

/**
 * Doll loadout slots: both hands take items a doll can act with, the core takes a folded talisman the doll
 * wears, cloth is synced but takes nothing yet. Declaration order is load-bearing — {@code MutableDollInventory}
 * indexes its array by {@link #ordinal()}, and {@code DOLL_LOADOUT} saves that array.
 *
 * @see "doc/design/doll/loadout.md"
 */
public enum DollSlot {

	MAIN_HAND,
	OFF_HAND,
	CLOTH,
	CORE

}
