package dev.xkmc.gensokyolegacy.content.attachment.doll;

/**
 * A doll's pairing form. {@code SUMMONED} is an entity right now; {@code STORED} and
 * {@code TEMP} are parked data carrying an intent — to become an item, or to become an
 * entity again.
 *
 * @see doc/design/doll/pairing.md
 */
public enum DollState {
	SUMMONED,
	STORED,
	TEMP
}