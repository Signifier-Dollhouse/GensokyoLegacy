package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

/**
 * Transient per-doll record of the last loadout slot a doll spent an item from, so
 * that item stays in its hand for {@link #HOLD} ticks afterwards.
 * <p>
 * The one-shot animations are triggered by the very action that spends the item, and
 * a ticket does not outlive that action: the heal, throw and laser behaviors all
 * complete on the tick they fire. So an arming pass on the next tick sees an idle
 * doll whose hand is empty and refills it — and the client, still playing the swing
 * that item was just swung with, watches the wand blink out of the fist mid-swing.
 * Locking the hands for a beat after the spend is what keeps the picture and the
 * bookkeeping in the same frame.
 * <p>
 * Server-only and never persisted, like {@link dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionHandler}:
 * the client is told what to draw by the synced loadout mirror, it keeps no timing
 * of its own.
 */
public final class DollHandLock {

	/** Ticks a spent item stays in hand, however ready its host is to re-arm. */
	public static final int HOLD = 10;

	/**
	 * Far enough in the past that the first {@link #isLocked} subtraction cannot
	 * overflow — same sentinel and same subtraction shape as
	 * {@code DollActionHandler.ready}, for the same reason.
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