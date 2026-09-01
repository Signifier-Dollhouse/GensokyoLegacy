package dev.xkmc.gensokyolegacy.content.attachment.character;

/**
 * Centralized constants for the reputation system. All magic numbers that control how
 * reputation is gained, lost, capped and decayed live here so they can be tuned in one
 * place instead of being scattered across callers.
 *
 * <p>Reputation spans the hard band [{@link #MIN_REPUTATION}, {@link #MAX_REPUTATION}],
 * but a character's current reachable maximum is bounded by the per-character
 * {@code reputationCap}, which starts at {@link #INITIAL_CAP} and grows through quests.
 * Gains apply an absolute soft cap: once reputation has crossed the soft cap value, further
 * gains from that action are halved.
 */
public final class ReputationConstants {

	// --- Absolute bounds ---

	/**
	 * Hard lower bound every reputation value is clamped to. No character relationship can
	 * ever be worse than this, regardless of losses. Acts as the implicit floor for the
	 * single-argument {@code loseReputation(int)} until an explicit floor is supplied.
	 */
	public static final int MIN_REPUTATION = -300;

	/**
	 * Hard upper bound of the reputation scale. No character relationship can ever exceed
	 * this value. Individual characters cap lower via {@code reputationCap}, so this is the
	 * theoretical ceiling quests can eventually unlock toward.
	 */
	public static final int MAX_REPUTATION = 300;

	// --- Starting values ---

	/**
	 * Starting reputation for a fresh, unknown character relationship. A brand new player
	 * begins as a stranger (neutral) with this value.
	 */
	public static final int INITIAL_REPUTATION = 0;

	/**
	 * Starting reputation cap for a new character relationship. Players may earn reputation
	 * up to this value immediately through simple actions (feeding, gifts, kills), but must
	 * complete quests to raise the cap further toward {@link #MAX_REPUTATION}.
	 */
	public static final int INITIAL_CAP = 100;

	// --- ReputationState thresholds ---

	/**
	 * Minimum reputation to be considered a FRIEND (green). Being at or above this value
	 * makes the character non-hostile and treated as worthy rather than a combat target.
	 */
	public static final int THRESHOLD_FRIEND = 150;

	/**
	 * Minimum reputation to be a STRANGER (white); any value below this but above
	 * {@link #THRESHOLD_JERK} is neutral. Strangers are neither friendly nor hostile.
	 */
	public static final int THRESHOLD_STRANGER = -50;

	/**
	 * Minimum reputation to be a JERK (yellow); values below this fall to ENEMY. JERK and
	 * below are hostile targets.
	 */
	public static final int THRESHOLD_JERK = -150;

	// --- Daily decay ---

	/**
	 * How many points of reputation decay per in-game day. Positive reputation drifts down
	 * and negative reputation drifts back toward zero by this amount each day.
	 */
	public static final int DAILY_DECAY_AMOUNT = 1;

	// --- Combat: youkai killed player (forgiveness) ---

	/**
	 * Reputation gained toward a character when one of that character's youkai kills the
	 * player. This is forgiveness: a death is not meant to drive the relationship all the
	 * way to {@link #MIN_REPUTATION} on its own, so the loss of {@link #DEATH_LOSS} is
	 * partially offset by this gain. Never raises the reputation cap.
	 */
	public static final int KILLED_GAIN = 100;

	/**
	 * Negative forgiveness ceiling for {@link #KILLED_GAIN}. The gain moves reputation toward
	 * zero but can never push it above this negative value, so forgiveness only applies while
	 * the relationship is hostile. Once reputation has recovered past {@code -50}, being
	 * killed by this character's youkai no longer helps.
	 */
	public static final int KILLED_SOFT_CAP = -50;

	// --- Combat: youkai killed the player ---

	/**
	 * Reputation lost when the player is killed by a character's youkai. A serious setback
	 * that drops the relationship, floored at {@link #MIN_REPUTATION}.
	 */
	public static final int DEATH_LOSS = 200;

	// --- Combat: player hurt a youkai ---

	/**
	 * Minor reputation loss for a single small (non-danmaku, damage &lt;= 4) first hit dealt
	 * to a previously-unaware youkai while reputation is already high (at or above
	 * {@link #HURT_FIRST_SMALL_REP_THRESHOLD}). Cushions accidents with friends.
	 */
	public static final int HURT_FIRST_SMALL_LOSS = 1;

	/**
	 * Reputation level at which a first hit is treated leniently ({@link #HURT_FIRST_SMALL_LOSS})
	 * rather than an escalating penalty. Values at or above this get the smallest loss.
	 */
	public static final int HURT_FIRST_SMALL_REP_THRESHOLD = 100;

	/**
	 * Standard reputation loss for a larger or repeated hit, and the loss applied to a first
	 * hit when reputation is low. Sits between the small leniency loss and the severe
	 * repeat losses.
	 */
	public static final int HURT_FIRST_BIG_LOSS = 5;

	/**
	 * Floor for reputation when taking a bigger first hit at low reputation. Prevents a couple
	 * of stray hits from instantly tanking a relationship into outright hostility.
	 */
	public static final int HURT_FIRST_BIG_FLOOR = -100;

	/**
	 * Reputation loss for a repeated (or large) hit against a STRANGER or above without a
	 * friendship cushion. Escalates damage to the relationship as the fight continues.
	 */
	public static final int HURT_REPEAT_LOW_LOSS = 10;

	/**
	 * Floor for reputation when punishing repeat hits while the player is not yet friendly.
	 * Keeps repeated low-tier harassment from pushing all the way to {@link #MIN_REPUTATION}.
	 */
	public static final int HURT_REPEAT_LOW_FLOOR = -150;

	/**
	 * Reputation loss for repeated (or large) hits directed at an already-hostile
	 * (JERK or ENEMY) character. Represents the heaviest penalty for provoking a fight.
	 */
	public static final int HURT_REPEAT_HIGH_LOSS = 20;

	// --- Feeding ---

	/**
	 * Absolute soft cap for reputation gained by feeding a character. Feeding grants full
	 * value up to this point and half value beyond it, so grinding food alone cannot max out
	 * a relationship. Feeding never raises the reputation cap.
	 */
	public static final int FEED_SOFT_CAP = 150;

	// --- Gifts ---

	/**
	 * Absolute soft cap for reputation gained by gifting a character. Like feeding, gifting
	 * is full-value up to this point and halved beyond, and never raises the reputation cap.
	 */
	public static final int GIFT_SOFT_CAP = 150;

	private ReputationConstants() {}
}
