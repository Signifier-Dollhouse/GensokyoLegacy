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
 * The core slot's folded talisman: a talisman a doll <b>wears</b> rather than an order it
 * follows. Everything the Curios charm slot does for a player it does here for a doll — the
 * passive tick, and the on-attacked / on-damaged hooks, which reach the doll through
 * {@link TalismanCurioItem}'s discovery rather than through Curios. A Curios wearer has its
 * talisman stacks synced for it by the Curios capability; a doll has to refresh its own mirror
 * after each of those paths, which is what the two methods below are for.
 *
 * <p>Self-only by construction, which is the whole point of putting it in the core rather than a
 * hand: the context's target is the doll, and an effect only ever acts on its own target
 * ({@code TalismanPaperItem.trigger}), so a core paper heals, hastes or shelters the doll and
 * nothing it fights beside. The deliberate opposite of a heal talisman in a hand, which the doll
 * walks over to a wounded ally to spend (DollHealBehavior).
 *
 * <p>No ticket, no target, no approach — nothing an order needs. A hand item has to be
 * physically held and is therefore driven by a goal; this one is a passive holder, so all it
 * needs is a tick.
 */
public interface DollCoreTalisman extends DollLoadout, TalismanHolder {

	@Override
	default List<ItemStack> talismanStacks() {
		ItemStack core = asDoll().ledgerStack(DollSlot.CORE);
		return core.isEmpty() ? List.of() : List.of(core);
	}

	/**
	 * Server tick: one activation sweep, then a mirror refresh if the paper spent a use.
	 *
	 * <p>{@link TalismanContext#hurtItem} wears the live ledger stack in place, so the mirror is
	 * stale the moment anything triggers — and the slot has to be emptied outright once the last
	 * use is gone, or the doll would keep ticking an empty stack. Both are one comparison of the
	 * uses component, so an idle doll syncs nothing.
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
	 * A damage hook spent a use of the core paper, so the ledger and its client mirror have
	 * parted company. There is nothing to diff against this time — the hook has already worn the
	 * stack — but the ledger half still has to happen: a slot emptied by its last use holds a
	 * count-zero stack that has to go back to being nothing. The mirror refresh itself writes
	 * four entity-data values that the synched-data layer drops when they are unchanged, so an
	 * attack the paper did nothing about still costs no packet.
	 */
	@Override
	default void onTalismansSpent() {
		MutableDollInventory inv = asDoll().loadout();
		if (inv.get(DollSlot.CORE).isEmpty()) inv.set(DollSlot.CORE, ItemStack.EMPTY);
		asDoll().syncLoadoutMirror();
	}

}
