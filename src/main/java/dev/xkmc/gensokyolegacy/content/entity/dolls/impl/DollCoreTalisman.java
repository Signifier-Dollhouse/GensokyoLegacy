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
 * The core slot's folded talisman: a talisman a doll <b>wears</b> rather than an order
 * it follows. Everything the Curios charm slot does for a player it does here for a
 * doll — the passive tick and the on-attacked / on-damaged hooks, reached through
 * {@link TalismanCurioItem}'s discovery rather than through Curios. A doll has to
 * refresh its own mirror after each of those paths; Curios syncs its wearer's talisman
 * stacks for it, a doll does not.
 * <p>
 * Self-only by construction, which is the point of the slot: the context's target is
 * the doll and an effect only acts on its own target, so a core paper heals, hastes
 * or shelters the doll and nothing beside it — the deliberate opposite of a heal
 * talisman in a hand (loadout.md §6). No ticket, no target, no approach: all it needs
 * is a tick.
 */
public interface DollCoreTalisman extends DollLoadout, TalismanHolder {

	@Override
	default List<ItemStack> talismanStacks() {
		ItemStack core = asDoll().ledgerStack(DollSlot.CORE);
		return core.isEmpty() ? List.of() : List.of(core);
	}

	/**
	 * Server tick: one activation sweep, then a mirror refresh if the paper spent a use.
	 * The slot has to be emptied outright once the last use is gone, or the doll would
	 * keep ticking an empty stack; both are one comparison of the uses component, so an
	 * idle doll syncs nothing.
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
	 * A damage hook spent a use of the core paper, so the ledger and its client mirror
	 * have parted company. The ledger half still has to happen: a slot emptied by its
	 * last use holds a count-zero stack that has to go back to being nothing.
	 */
	@Override
	default void onTalismansSpent() {
		MutableDollInventory inv = asDoll().loadout();
		if (inv.get(DollSlot.CORE).isEmpty()) inv.set(DollSlot.CORE, ItemStack.EMPTY);
		asDoll().syncLoadoutMirror();
	}

}
