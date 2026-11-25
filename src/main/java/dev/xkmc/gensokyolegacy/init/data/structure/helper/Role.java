package dev.xkmc.gensokyolegacy.init.data.structure.helper;

/**
 * Tier of a structure in its set, which decides the index space its {@code set_index}/{@code set_count} belong to and
 * whether it is a home or a decorative filler.
 *
 * @see "doc/design/structure/secondary.md"
 */
public enum Role {

	PRIMARY,
	SECONDARY,
	TINY

}
