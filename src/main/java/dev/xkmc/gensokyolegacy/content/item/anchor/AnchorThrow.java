package dev.xkmc.gensokyolegacy.content.item.anchor;

/**
 * What one anchor click does. The item picks it from the mode and the click; a thrown projectile
 * reads it for the rest of its life, while the rain uses it only for the cost gate.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public enum AnchorThrow {
	DESTROY(16, 0),
	DESTROY_CHARGED(16, 0),
	GRAB(16, 0),
	PULL(16, 0),
	STORM(48, 5),
	STORM_RAIN(32, 20);

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
		return this == STORM;
	}

	/**
	 * The charged right click's rain: anchors are dropped over nearby enemies instead of thrown
	 * from the hand, so it has no flight of its own.
	 */
	public boolean isRain() {
		return this == STORM_RAIN;
	}

	/**
	 * Whether an impact from this throw could hurt anything. Only these hits play the mace-smash
	 * feedback; the Voyage throws move things without damage.
	 */
	public boolean dealsDamage() {
		return this == DESTROY || this == DESTROY_CHARGED;
	}

}
