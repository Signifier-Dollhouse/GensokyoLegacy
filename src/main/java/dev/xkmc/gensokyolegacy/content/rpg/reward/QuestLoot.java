package dev.xkmc.gensokyolegacy.content.rpg.reward;

import java.util.ArrayList;

/**
 * A quest's item rewards, read off its loot tables on the server and sampled before sending: the
 * drops themselves, and the one rule that hands all of them out.
 */
public record QuestLoot(RewardRule rule, ArrayList<LootDrop> drops) {
}
