package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.attachment.doll.MutableDollInventory;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanCurioItem;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanHolder;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * The core slot's folded talisman: a talisman a doll <b>wears</b> rather than an order it follows,
 * reaching Curios' passive tick and damage hooks through {@link TalismanCurioItem}'s discovery and
 * acting only on the doll itself — the opposite of a heal talisman in a hand (loadout.md §6).
 */
public interface DollCoreTalisman extends DollLoadout, TalismanHolder {

	@Override
	default List<ItemStack> talismanStacks() {
		ItemStack core = asDoll().ledgerStack(DollSlot.CORE);
		return core.isEmpty() ? List.of() : List.of(core);
	}

	/**
 * Server tick: one activation sweep, then a mirror refresh if the paper spent a use. The slot is
 * emptied outright on the last use so an idle doll syncs nothing.
 */
	default void tickCoreTalisman() {
		MutableDollInventory inv = asDoll().loadout();
		ItemStack live = inv.get(DollSlot.CORE);
		if (live.isEmpty()) return;
		Integer before = GLTalismans.DC_TALISMAN_DURABILITY.get(live);
		TalismanCurioItem.tickExtraTalismans(asDoll());
		if (live.isEmpty()) inv.set(DollSlot.CORE, ItemStack.EMPTY);
		else if (Objects.equals(before, GLTalismans.DC_TALISMAN_DURABILITY.get(live))) return;
		asDoll().syncLoadoutMirror();
	}

	/**
	 * A damage hook spent a use, so ledger and client mirror have parted company. A slot emptied
	 * by its last use still holds a count-zero stack, which has to go back to being nothing.
	 */
	@Override
	default void onTalismansSpent() {
		MutableDollInventory inv = asDoll().loadout();
		if (inv.get(DollSlot.CORE).isEmpty()) inv.set(DollSlot.CORE, ItemStack.EMPTY);
		asDoll().syncLoadoutMirror();
	}

}
