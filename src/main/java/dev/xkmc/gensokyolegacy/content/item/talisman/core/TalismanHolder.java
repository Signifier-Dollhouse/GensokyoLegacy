package dev.xkmc.gensokyolegacy.content.item.talisman.core;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A carrier of talismans that is not a Curios charm slot: something with a slot of its own
 * whose talismans belong to the entity carrying them and to nobody else. A doll's core slot is
 * the one such holder ({@code DollCoreTalisman}).
 *
 * <p>Discovery is a type check ({@link TalismanCurioItem#equippedTalismans}), so this package
 * never has to know what implements it — the same inversion the Curios charm slot gets for
 * free from the Curios capability.
 */
public interface TalismanHolder {

	/**
	 * The talisman stacks this holder carries right now. Live, not copies: a context built from
	 * one of these wears it in place, which is the whole point of a worn talisman — there is no
	 * pocket around it to write the change back into.
	 */
	List<ItemStack> talismanStacks();

	/**
	 * A talisman carried by this holder just spent a use outside its own tick — a damage hook
	 * firing {@code onAttacked} / {@code onDamaged}, which wear the stack in place exactly as
	 * {@link TalismanContext#hurtItem} does anywhere else. A Curios charm slot needs nothing
	 * here: the stack it wears belongs to the Curios capability, which syncs itself. A holder
	 * that keeps its talismans in storage of its own has to refresh whatever mirrors them.
	 */
	default void onTalismansSpent() {
	}

}
