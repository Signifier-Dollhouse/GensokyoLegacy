package dev.xkmc.gensokyolegacy.content.item.doll;

/**
 * Doll loadout slots, as the ledger and the client mirror index them. Both hands take items a
 * doll can act with, the core takes a folded talisman the doll wears, cloth is synced but takes
 * nothing yet.
 * <p>
 * The declaration order is load-bearing: {@code MutableDollInventory} indexes its array by
 * {@link #ordinal()}, and {@code DOLL_LOADOUT} saves that array.
 *
 * @see doc/design/doll/loadout.md
 */
public enum DollSlot {

	MAIN_HAND,
	OFF_HAND,
	CLOTH,
	CORE

}
