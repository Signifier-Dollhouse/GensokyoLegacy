package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

/**
 * Transient record of the last slot a doll spent an item from, so it stays in hand for
 * {@link #HOLD} ticks: the spend also fires the one-shot and completes that tick, so re-arming
 * next tick would pull the item out mid-swing (control.md §10). Server-only, never persisted.
 */
public final class DollHandLock {

	/**
	 * Ticks a spent item stays in hand, however ready its host is to re-arm.
	 */
	public static final int HOLD = 10;

	/**
	 * Far enough in the past that the first {@link #isLocked} subtraction cannot
	 * overflow — same sentinel and subtraction shape as {@code DollActionHandler.ready}.
	 */
	private long spentAt = -1000;

	/**
	 * Called by a behavior the moment it acts with the item, before it completes.
	 */
	public void stamp(long gameTime) {
		spentAt = gameTime;
	}

	/**
	 * Whether an item was spent within the last {@link #HOLD} ticks.
	 */
	public boolean isLocked(long gameTime) {
		return gameTime - spentAt < HOLD;
	}

}