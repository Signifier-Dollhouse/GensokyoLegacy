package dev.xkmc.gensokyolegacy.content.item.doll;

/**
 * Doll loadout slots, as the ledger and the client mirror index them.
 *
 * <p>Both hands take items a doll can act with; the core takes a folded talisman the doll wears
 * ({@code DollCoreTalisman}); cloth is synced and laid out but takes nothing yet.
 *
 * <p>The declaration order is load-bearing: {@code MutableDollInventory} indexes its array by
 * {@link #ordinal()}, and {@code DOLL_LOADOUT} saves that array.
 */
public enum DollSlot {

	MAIN_HAND,
	OFF_HAND,
	CLOTH,
	CORE

}
