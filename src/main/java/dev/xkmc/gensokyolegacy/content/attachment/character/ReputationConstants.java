package dev.xkmc.gensokyolegacy.content.attachment.character;

public final class ReputationConstants {

	// --- Absolute bounds ---
	public static final int MIN_REPUTATION = -300;
	public static final int MAX_REPUTATION = 300;

	// --- Starting values ---
	public static final int INITIAL_REPUTATION = 0;
	public static final int INITIAL_CAP = 100;

	// --- ReputationState thresholds ---
	public static final int THRESHOLD_FRIEND = 150;
	public static final int THRESHOLD_STRANGER = -50;
	public static final int THRESHOLD_JERK = -150;

	// --- Daily decay ---
	public static final int DAILY_DECAY_AMOUNT = 1;
	public static final int DAILY_DECAY_THRESHOLD = 150;

	// --- Combat: youkai killed by player ---
	public static final int KILLED_GAIN = 100;
	public static final int KILLED_SOFT_CAP = 50;

	// --- Combat: player killed by youkai ---
	public static final int DEATH_LOSS = 200;

	// --- Combat: player hurt youkai ---
	public static final int HURT_FIRST_SMALL_LOSS = 1;
	public static final int HURT_FIRST_SMALL_REP_THRESHOLD = 100;
	public static final int HURT_FIRST_BIG_LOSS = 5;
	public static final int HURT_FIRST_BIG_FLOOR = -100;
	public static final int HURT_REPEAT_LOW_LOSS = 10;
	public static final int HURT_REPEAT_LOW_FLOOR = -150;
	public static final int HURT_REPEAT_HIGH_LOSS = 20;

	// --- Feeding ---
	public static final int FEED_SOFT_CAP = 150;

	// --- Gifts ---
	public static final int GIFT_SOFT_CAP = 150;

	private ReputationConstants() {}
}
