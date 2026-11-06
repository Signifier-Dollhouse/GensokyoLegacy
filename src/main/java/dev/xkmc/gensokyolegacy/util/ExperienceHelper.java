package dev.xkmc.gensokyolegacy.util;

import net.minecraft.world.entity.player.Player;

/**
 * Experience-point arithmetic for costs that consume a whole level plus the current progress.
 */
public final class ExperienceHelper {

	private ExperienceHelper() {
	}

	/**
	 * XP points needed to advance from {@code level} to {@code level + 1}, mirroring
	 * {@link Player#getXpNeededForNextLevel()} for an arbitrary level.
	 */
	public static int xpToNext(int level) {
		if (level >= 30) return 112 + (level - 30) * 9;
		if (level >= 15) return 37 + (level - 15) * 5;
		return 7 + level * 2;
	}

	/**
	 * XP points held in the player's current level together with the progress into it.
	 */
	public static int levelValue(Player player) {
		int level = player.experienceLevel;
		if (level <= 0) return 0;
		return xpToNext(level - 1) + (int) (player.experienceProgress * xpToNext(level));
	}

	/**
	 * Consumes one level and the current progress, returning the XP points actually removed.
	 */
	public static int consumeLevel(Player player) {
		int value = levelValue(player);
		if (value <= 0) return 0;
		player.experienceProgress = 0;
		player.giveExperienceLevels(-1);
		return value;
	}

}
