package dev.xkmc.gensokyolegacy.content.item.spiritual;

/**
 * An item that carries a spiritual power pool, whose size is fixed per item rather than stored on the
 * stack.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public interface SpiritualCarrier {

	int maxPower();

}
