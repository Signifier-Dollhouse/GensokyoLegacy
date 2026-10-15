package dev.xkmc.gensokyolegacy.content.attachment.character;

/**
 * Every tuning number of reputation: a relationship's value ({@link CharacterData#reputation}) and reachable cap.
 *
 * @see "doc/design/reputation.md"
 */
public final class ReputationConstants {

	// --- Absolute bounds ---

	/** Hard lower bound of the scale, and the default floor of {@link CharacterData#loseReputation(int)}. */
	public static final int MIN_REPUTATION = -300;

	/** Hard upper bound of the scale. A character usually stops lower, at its own reputation cap. */
	public static final int MAX_REPUTATION = 300;

	// --- Starting values ---

	/** Reputation of an untouched relationship, which reads as STRANGER. */
	public static final int INITIAL_REPUTATION = 0;

	/** Ceiling of a fresh relationship. Simple actions fill it; only quest rewards raise it. */
	public static final int INITIAL_CAP = 100;

	// --- ReputationState thresholds ---

	/** Minimum reputation for FRIEND, which also makes the character a worthy rather than a combat target. */
	public static final int THRESHOLD_FRIEND = 150;

	/** Minimum reputation for STRANGER; below it the character is at least a JERK. */
	public static final int THRESHOLD_STRANGER = -50;

	/** Minimum reputation for JERK; below it the character is an ENEMY. */
	public static final int THRESHOLD_JERK = -150;

	// --- Daily decay ---

	/** Points moved per in-game day, toward the decay floor when positive and back toward zero when hostile. */
	public static final int DAILY_DECAY_AMOUNT = 1;

	// --- Combat: youkai killed the player (forgiveness) ---

	/** Value a dead player is forgiven up to: a hostile character drops straight to it instead of climbing. */
	public static final int KILLED_RESTORE = 0;

	// --- Combat targeting ---

	/**
	 * Reputation above which a youkai leaves the player alone; at or below it the player is a valid
	 * target. {@link #INITIAL_REPUTATION} sits exactly on it, so a fresh player is hostile by default.
	 */
	public static final int COMBAT_SAFE_THRESHOLD = 0;

	/** Reputation below which the character leaves the world when no non-hostile player is around. */
	public static final int DISCARD_REP_THRESHOLD = 0;

	// --- Combat: player killed a youkai ---

	/** Reputation lost when the player kills one of the character's youkai. */
	public static final int DEATH_LOSS = 200;

	// --- Combat: player hurt a youkai ---

	/** Floor for every loss from hurting a youkai; only killing can push below it. Equals {@link #THRESHOLD_JERK}. */
	public static final int HURT_LOSS_FLOOR = -150;

	/** Loss for a single small first hit against an already friendly character. */
	public static final int HURT_FIRST_SMALL_LOSS = 1;

	/** Reputation from which a first hit is met with leniency instead of an escalating penalty. */
	public static final int HURT_FIRST_SMALL_REP_THRESHOLD = 100;

	/** Loss for a first hit of any size against a character that is not yet a friend. */
	public static final int HURT_FIRST_BIG_LOSS = 5;

	/** Loss for a repeated hit against a character that is still a STRANGER or better. */
	public static final int HURT_REPEAT_LOW_LOSS = 10;

	/** Loss for a repeated hit against a character that is already hostile. */
	public static final int HURT_REPEAT_HIGH_LOSS = 20;

	// --- Feeding ---

	/** Reputation above which feeding gains at half rate. Never raises the reputation cap. */
	public static final int FEED_SOFT_CAP = 150;

	// --- Gifts ---

	/** Reputation above which gifting gains at half rate. Never raises the reputation cap. */
	public static final int GIFT_SOFT_CAP = 150;

	private ReputationConstants() {
	}
}
