package dev.xkmc.gensokyolegacy.content.attachment.doll;

/**
 * A doll's pairing form: {@code SUMMONED} is a live entity; {@code STORED} and {@code TEMP}
 * are parked data carrying an intent — to become an item, or an entity again.
 *
 * @see "doc/design/doll/pairing.md"
 */
public enum DollState {
	SUMMONED,
	STORED,
	TEMP
}