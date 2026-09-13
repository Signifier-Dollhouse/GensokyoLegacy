package dev.xkmc.gensokyolegacy.content.entity.dolls.action;

/**
 * Client-visible execution status of a doll, synced via entity data for the
 * attack-glove sidebar. {@code INVALID} (no valid weapon for the held attack)
 * is computed client-side from the loadout mirror and is not synced.
 */
public enum DollActionStatus {

	IDLE,
	PREPARING,
	ATTACKING,
	DONE;

	/**
	 * Sidebar frame color (ARGB): white for idle, yellow for preparing, red for
	 * attacking, green for done.
	 */
	public int frameColor() {
		return switch (this) {
			case IDLE -> 0xFFFFFFFF;
			case PREPARING -> 0xFFFFFF00;
			case ATTACKING -> 0xFFFF0000;
			case DONE -> 0xFF00FF00;
		};
	}

}
