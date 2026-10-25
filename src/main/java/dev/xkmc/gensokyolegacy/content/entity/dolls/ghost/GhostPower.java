package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

/**
 * The shared power scale: a ghost is fully opaque at {@link #MAX} and fades as it spends power.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public final class GhostPower {

	/**
	 * Full power of a living ghost, and the point the fan tops a ghost up to.
	 */
	public static final int MAX = 1000;

	/**
	 * A wild ghost rolls its starting power in this inclusive range.
	 */
	public static final int WILD_MIN = 500;
	public static final int WILD_MAX = 1000;

	/**
	 * Power one summon costs, and the power a freshly summoned ghost carries.
	 */
	public static final int SUMMON_COST = 1000;

	/**
	 * Below this an owned ghost leaves on its own, and the bone flute refuses to bind a stray.
	 */
	public static final int LEAVE_THRESHOLD = 200;

	/**
	 * Never let the model vanish completely: a spent ghost is still a faint outline.
	 */
	public static final float MIN_ALPHA = 0.05F;

	private GhostPower() {
	}

}
