package dev.xkmc.gensokyolegacy.content.entity.foundation;

import net.minecraft.world.entity.Entity;

/**
 * An entity welded to the level it currently stands in: no portal, and no other
 * move helper, may carry it across dimensions. Marker only — the overrides live
 * on the concrete bases, where a Java interface default would be shadowed by
 * {@link Entity}'s concrete methods.
 *
 * @see doc/design/doll/entity.md
 * @see doc/design/character_visit.md
 */
public interface IDimensionBoundEntity {

}