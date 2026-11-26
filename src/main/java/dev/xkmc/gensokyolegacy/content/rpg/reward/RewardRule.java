package dev.xkmc.gensokyolegacy.content.rpg.reward;

import java.util.List;

/**
 * How a quest's whole reward list is handed out: together, rolled, or picked by the player.
 */
public enum RewardRule {
	/**
	 * every listed item is given
	 */
	ALL,
	/**
	 * a roll picks from the listed items
	 */
	RANDOM,
	/**
	 * the player picks from the listed items
	 */
	CHOICE,
	/**
	 * several rules in one reward, which a single line cannot state
	 */
	MIXED;

	/**
	 * One rule for several sources: one source keeps its own rule, several sources are all given
	 * only if every one of them is.
	 */
	public static RewardRule combine(List<RewardRule> rules) {
		if (rules.isEmpty()) return MIXED;
		if (rules.size() == 1) return rules.get(0);
		for (var rule : rules)
			if (rule != ALL) return MIXED;
		return ALL;
	}

}
