package dev.xkmc.gensokyolegacy.content.entity.foundation;

import net.minecraft.world.entity.Entity;

/**
 * An entity welded to the level it currently stands in: no portal, and no other
 * move helper, may carry it across dimensions.
 *
 * <p>Both dolls and characters are bound this way, and for the same reason —
 * neither is a thing that exists in the world at large. Each is a projection of
 * a record that lives in one specific level: a doll of its ledger entry
 * (doc/design/doll), a character of the home or visit site its index entry names
 * (doc/design/character_visit.md). Letting one walk into a portal would strand
 * it where no ledger can describe it, and the existing self-healing passes would
 * only notice by discarding it and building a replacement back home — a churn
 * that the dimension lock simply makes unnecessary.
 *
 * <p>The policy itself cannot live here: {@link Entity#canUsePortal} and
 * {@link Entity#canChangeDimensions} are concrete superclass methods, and a Java
 * interface default is silently shadowed by them. So the two concrete bases —
 * {@code BaseDollEntity} and {@code YoukaiEntity} — carry the overrides, and
 * this type is the single marker those overrides and the call sites that move
 * other entities around (umbrella capture) agree on.
 */
public interface IDimensionBoundEntity {

}