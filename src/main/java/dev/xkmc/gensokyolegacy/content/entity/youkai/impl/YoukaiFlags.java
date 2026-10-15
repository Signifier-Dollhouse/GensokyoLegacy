package dev.xkmc.gensokyolegacy.content.entity.youkai.impl;

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
	FLEEING,
	/**
	 * At a party. Synced for the same reason as {@link #FLEEING} - it is what she
	 * moves on, and it decides where she is standing.
	 */
	PARTYING,
	/**
	 * Lying down in {@code GLBrains.NAP}. Synced because the pose is a client-side decision and the client
	 * cannot ask the brain, the same reason {@code isSleeping()} and {@code isPassenger()} are authoritative.
	 */
	NAPPING
}
