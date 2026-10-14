package dev.xkmc.gensokyolegacy.content.item.talisman.core;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A carrier of talismans that is not a Curios charm slot: its own slot, whose talismans belong
 * to the entity carrying them alone — a doll's core slot is the one such holder, found by a
 * type check in {@link TalismanCurioItem#equippedTalismans}.
 */
public interface TalismanHolder {

	/** The talisman stacks this holder carries right now. Live, not copies: a context built from
	 * one wears it in place, since there is no pocket around it to write the change back into.
	 */
	List<ItemStack> talismanStacks();

	/**
	 * A talisman here just spent a use outside its own tick, so a holder keeping its talismans in
	 * storage of its own must refresh whatever mirrors them; a Curios charm slot needs nothing here.
	 */
	default void onTalismansSpent() {
	}

}
