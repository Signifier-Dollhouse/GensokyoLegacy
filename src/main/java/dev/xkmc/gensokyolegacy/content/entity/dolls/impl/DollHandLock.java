package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

/**
 * Transient per-doll record of the last loadout slot a doll spent an item from, so
 * that item stays in its hand for {@link #HOLD} ticks afterwards.
 * <p>
 * The one-shot animations are triggered by the very action that spends the item, and
 * heal, throw and laser complete on the tick they fire — so an arming pass on the next
 * tick would take the item back out of the hand while the client is still playing the
 * swing. Locking both hands for a beat past the spend keeps the picture and the
 * bookkeeping in the same frame (control.md §10).
 * <p>
 * Server-only and never persisted, like {@code DollActionHandler}: the client is told
 * what to draw by the synced loadout mirror.
 */
public final class DollHandLock {

	/** Ticks a spent item stays in hand, however ready its host is to re-arm. */
	public static final int HOLD = 10;

	/**
	 * Far enough in the past that the first {@link #isLocked} subtraction cannot
	 * overflow — same sentinel and subtraction shape as {@code DollActionHandler.ready}.
	 */
	private long spentAt = -1000;

	/** Called by a behavior the moment it acts with the item, before it completes. */
	public void stamp(long gameTime) {
		spentAt = gameTime;
	}

	/** Whether an item was spent within the last {@link #HOLD} ticks. */
	public boolean isLocked(long gameTime) {
		return gameTime - spentAt < HOLD;
	}

}