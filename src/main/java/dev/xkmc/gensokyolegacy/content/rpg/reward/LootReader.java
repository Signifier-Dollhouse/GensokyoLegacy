package dev.xkmc.gensokyolegacy.content.rpg.reward;

import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestReward;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootDataResolver;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * What a quest's loot tables can drop, read off the tables rather than rolled, so a constant count is exact, not
 * merely observed. Conditions go entirely: every branch counts, the point of a page describing what <i>can</i> come
 * out. The pools also say whether the items come together or a roll picks one, which is the quest's reward rule.
 *
 * @see "doc/design/quest/rewards.md"
 */
public class LootReader {

	private LootReader() {
	}

	/**
	 * Every item a quest hands out through loot, read. Only those: this is all a client is told,
	 * and a page for a quest is the only thing that needs them.
	 */
	public static LinkedHashMap<ResourceLocation, QuestLoot> questRewards(MinecraftServer server) {
		var registries = server.getLootData();
		LinkedHashMap<ResourceLocation, QuestLoot> ans = new LinkedHashMap<>();
		for (var quest : CodecRegistry.QUEST.getAll(server.registryAccess()).toList()) {
			var key = quest.unwrapKey().orElse(null);
			if (key == null) continue;
			List<RewardRule> rules = new ArrayList<>();
			ArrayList<LootDrop> drops = new ArrayList<>();
			for (QuestReward<?> reward : quest.value().rewards())
				if (reward instanceof LootTableReward loot) {
					var table = of(loot.table(), registries);
					rules.add(table.rule());
					drops.addAll(table.drops());
				}
			if (!rules.isEmpty())
				ans.put(key.location(), new QuestLoot(RewardRule.combine(rules), drops));
		}
		return ans;
	}

	/**
	 * Everything the table {@code id} names can drop, one entry per item, its count folded in
	 * from however many pools, rolls and functions ask for it.
	 */
	private static TableLoot of(ResourceLocation id, LootDataResolver registries) {
		var table = registries.getLootTable(id);
		if (table == LootTable.EMPTY) return new TableLoot(RewardRule.MIXED, new ArrayList<>());
		ArrayList<LootDrop> drops = new ArrayList<>();
		for (LootPool pool : table.pools) readPool(pool, drops);
		return new TableLoot(rule(table), drops);
	}

	/**
	 * Several pools of one entry each hand out all of their items; one pool of several entries
	 * rolls for one of them. Anything else mixes the two.
	 */
	private static RewardRule rule(LootTable table) {
		if (table.pools.isEmpty()) return RewardRule.MIXED;
		if (table.pools.size() > 1) {
			for (LootPool pool : table.pools)
				if (pool.entries.length > 1) return RewardRule.MIXED;
			return RewardRule.ALL;
		}
		return table.pools.get(0).entries.length > 1 ? RewardRule.RANDOM : RewardRule.ALL;
	}

	private static void readPool(LootPool pool, List<LootDrop> drops) {
		// how many times the pool rolls is how much of it there is, but a bonus roll makes that
		// turn on the luck of whoever is rolling, which a table does not state
		Range rolls = range(pool.rolls);
		Range bonus = range(pool.bonusRolls);
		Range times = rolls == null || (bonus != null && bonus.max() > 0) ? null
				: new Range(rolls.min(), rolls.max());
		if (times != null && times.max() < 1) return;
		for (LootPoolEntryContainer entry : pool.entries) readEntry(entry, times, drops);
	}

	private static void readEntry(LootPoolEntryContainer entry, @Nullable Range times, List<LootDrop> drops) {
		if (entry instanceof LootItem item) {
			// no entry type but this one names an item outright; the rest are left to speak for
			// themselves rather than guessed at
			add(drops, new ItemStack(item.item), times, readCount(item.functions));
		} else if (entry instanceof CompositeEntryBase composite) {
			// conditions being ignored makes a group, a sequence and a set of alternatives all
			// the same thing: everything any of them could hold
			for (LootPoolEntryContainer child : composite.children) readEntry(child, times, drops);
		}
	}

	/**
	 * The count an entry's {@code set_count} functions ask for, or null if they do not say one. Anything else
	 * they do either leaves the count alone or changes the item in ways a page cannot show.
	 */
	private static @Nullable Range readCount(LootItemFunction[] functions) {
		Range ans = new Range(1, 1);
		for (LootItemFunction function : functions) {
			if (!(function instanceof SetItemCountFunction set)) continue;
			Range value = range(set.value);
			if (value == null) return null;
			ans = set.add ? new Range(ans.min() + value.min(), ans.max() + value.max())
					: new Range(value.min(), value.max());
		}
		return ans;
	}

	private static void add(List<LootDrop> drops, ItemStack stack, @Nullable Range times, @Nullable Range count) {
		Integer min = times == null || count == null ? null : times.min() * count.min();
		Integer max = times == null || count == null ? null : times.max() * count.max();
		int at = indexOf(drops, stack);
		if (at < 0) {
			drops.add(new LootDrop(stack.copyWithCount(1), min, max));
		} else {
			LootDrop drop = drops.get(at);
			drops.set(at, new LootDrop(drop.stack(), lo(drop.min(), min), hi(drop.max(), max)));
		}
	}

	/**
	 * the lower of two bounds, either being unsayable making the whole thing unsayable
	 */
	private static @Nullable Integer lo(@Nullable Integer a, @Nullable Integer b) {
		return a == null || b == null ? null : Math.min(a, b);
	}

	/**
	 * the upper of two bounds, on the same terms as {@link #lo}
	 */
	private static @Nullable Integer hi(@Nullable Integer a, @Nullable Integer b) {
		return a == null || b == null ? null : Math.max(a, b);
	}

	private static int indexOf(List<LootDrop> list, ItemStack stack) {
		for (int i = 0; i < list.size(); i++)
			if (ItemStack.isSameItemSameTags(list.get(i).stack(), stack)) return i;
		return -1;
	}

	/**
	 * The span of values a number provider can produce, or null where it turns on something only the roll knows.
	 * A constant is one value; a uniform range is as wide as its two ends say, if each end is itself sayable.
	 */
	private static @Nullable Range range(NumberProvider provider) {
		if (provider instanceof ConstantValue constant) {
			int value = Mth.floor(constant.value);
			return new Range(value, value);
		}
		if (provider instanceof UniformGenerator uniform) {
			Range min = range(uniform.min);
			Range max = range(uniform.max);
			if (min == null || max == null) return null;
			return new Range(min.min(), max.max());
		}
		return null;
	}

	/**
	 * a span of whole numbers, inclusive
	 */
	private record Range(int min, int max) {
	}

	/**
	 * One table's drops and the rule its pools imply, before the quest's tables are combined.
	 */
	private record TableLoot(RewardRule rule, ArrayList<LootDrop> drops) {
	}

}
