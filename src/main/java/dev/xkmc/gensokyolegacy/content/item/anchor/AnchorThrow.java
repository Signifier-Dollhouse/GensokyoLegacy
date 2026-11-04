package dev.xkmc.gensokyolegacy.content.item.anchor;

/**
 * What one thrown anchor does. The item picks it from the mode and the click; the projectile reads
 * it for the rest of its life.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public enum AnchorThrow {
	DESTROY(16, 0),
	DESTROY_CHARGED(16, 0),
	GRAB(16, 0),
	PULL(16, 0),
	STORM(48, 5),
	STORM_VOLLEY(48, 10);

	private final double reach;
	private final int cost;

	AnchorThrow(double reach, int cost) {
		this.reach = reach;
		this.cost = cost;
	}

	/**
	 * How far from the thrower this throw may travel before it gives up on hitting anything.
	 */
	public double reach() {
		return reach;
	}

	/**
	 * Spiritual power spent before the anchor is spawned.
	 */
	public int cost() {
		return cost;
	}

	public boolean isStorm() {
		return this == STORM || this == STORM_VOLLEY;
	}

}
