package dev.xkmc.gensokyolegacy.content.entity.youkai;

public enum YoukaiFlags {
	/**
	 * Append-only: the enum ordinal is the bit index in the synced flag
	 * bitfield, so new flags go last.
	 */
	CHARGING, FAINTED, POWERED, NONE, FED, GIFTED, FLYING, VISITING,
	/**
	 * Panicking. Synced rather than kept server-side because it is what she moves
	 * on, and the client has to agree with the server about how fast that is.
	 */
	FLEEING
}
