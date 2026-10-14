package dev.xkmc.gensokyolegacy.content.attachment.doll;

/**
 * What a ledger entry is allowed to become, where {@link DollState} only says what shape it is in
 * right now. Declared once by the entity kind ({@code BaseDollEntity.nature()}) and stamped onto the
 * entry by {@link DollSpawn#materialize}, so a kind is never declared twice.
 *
 * @see "doc/design/doll/follower.md"
 */
public enum DollNature {
	/** An item-backed doll: it converts to a doll item, the glove recalls it, and it carries a loadout. */
	DOLL(true, true, true),
	/**
	 * A temporary follower, conjured rather than owned. No item form exists, so its death or the end
	 * of its lifespan simply drops the entry, the glove cannot recall it, and it carries no loadout:
	 * the one move it fights with comes from what it is (DollBehaviorRegistry#registerIntrinsic).
	 */
	FOLLOWER(false, false, false);

	private final boolean itemizable, recallable, loadoutCapable;

	DollNature(boolean itemizable, boolean recallable, boolean loadoutCapable) {
		this.itemizable = itemizable;
		this.recallable = recallable;
		this.loadoutCapable = loadoutCapable;
	}

	/** Whether this can ever become a doll item, and with it be parked as {@link DollState#STORED}. */
	public boolean itemizable() {
		return itemizable;
	}

	/** Whether the glove recall sweep may take it at all. */
	public boolean recallable() {
		return recallable;
	}

	/** Whether the loadout editor opens on it. */
	public boolean loadoutCapable() {
		return loadoutCapable;
	}

}