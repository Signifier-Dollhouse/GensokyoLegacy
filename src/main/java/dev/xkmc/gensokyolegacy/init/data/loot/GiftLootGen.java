package dev.xkmc.gensokyolegacy.init.data.loot;

import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.AppealConsumedCondition;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyGift;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLDecoBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;
import java.util.Map;

/**
 * The gift each party guest leaves behind, as a loot table per character.
 * <p>
 * One table per character at {@code gensokyolegacy:party_gift/<entity type path>}, read by
 * {@link PartyGift}. A datapack replaces one by shipping its own file at the same path; a
 * character with no file of her own falls back to {@link #DEFAULT}.
 * <p>
 * Every table has the same shape: **one pool per generosity tier**, and the tiers are
 * disjoint bands of {@link AppealConsumedCondition}, so exactly one pool can ever fire
 * and the table yields exactly one stack however many pools it has. That is why the
 * generosity is a ladder of pools rather than a count on the entry: a count cannot be
 * conditional, and {@link PartyGift} deliberately keeps only the first stack, so a ladder
 * written as several unconditional pools would bury the table instead.
 * <p>
 * The tier boundaries reproduce the ladder {@code PartyGift} used to compute in Java —
 * one more of something per {@link #APPEAL_PER_TIER} of appeal the guest consumed, from
 * one at the bottom to five at the top. What a tier may hand out is wider than one item:
 * the good things arrive with the generosity rather than multiplying with it.
 */
public class GiftLootGen {

	/** Appeal that buys the guest one more of something. */
	public static final int APPEAL_PER_TIER = 8;

	/**
	 * {@code advancement_reward} because that is the stock param set carrying both halves
	 * of the context a gift table needs: the guest herself, for
	 * {@link AppealConsumedCondition}, and an origin.
	 */
	private static final LootContextParamSet SET = LootContextParamSets.ADVANCEMENT_REWARD;

	/** The even spread a guest with no table of her own falls back to. */
	private static final List<Tier> DEFAULT = List.of(
			tier(1, g(GLItems.FAIRY_CAKE, 10), g(GLItems.TENGU_SAKE, 10),
					g(GLItems.GHOST_SAKE, 10), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 10)),
			tier(2, g(GLItems.FAIRY_CAKE, 10), g(GLItems.TENGU_SAKE, 10),
					g(GLItems.GHOST_SAKE, 10), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 10),
					g(GLNaturalBlocks.STAR_FLOWER, 10)),
			tier(3, g(GLItems.FAIRY_CAKE, 10), g(GLItems.TENGU_SAKE, 10),
					g(GLItems.GHOST_SAKE, 10), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 10),
					g(GLNaturalBlocks.STAR_FLOWER, 10), g(GLItems.MAGIC_BOOK, 10)),
			tier(4, g(GLItems.MAGIC_BOOK, 12), g(GLNaturalBlocks.STAR_FLOWER, 12),
					g(GLItems.TENGU_SAKE, 10), g(GLItems.GHOST_SAKE, 10),
					g(GLItems.FAIRY_CAKE, 8), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 8)),
			tier(5, g(GLItems.MAGIC_BOOK, 16), g(GLNaturalBlocks.STAR_FLOWER, 14),
					g(GLItems.TENGU_SAKE, 12), g(GLItems.GHOST_SAKE, 12),
					g(GLItems.FAIRY_CAKE, 8), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 8)));

	public static void genLoot(RegistrateLootTableProvider pvd) {
		pvd.addLootAction(SET, c -> c.accept(PartyGift.DEFAULT_TABLE, table(DEFAULT)));
		// Reimu and Cirno keep the spread their `gift_preference` data maps already give
		// them — drink-led and toy-led respectively — scaled by ten so a 1.5 survives the
		// trip into integer loot weights.
		gift(pvd, "hakurei_reimu", reimu());
		gift(pvd, "cirno", cirno());
		// The other four have no authored preference, so these are a reading of who they
		// are rather than a conversion of anything: Marisa her library, Sakuya her
		// standards, Alice her dolls, Morichika his counter.
		gift(pvd, "kirisame_marisa", marisa());
		gift(pvd, "izayoi_sakuya", sakuya());
		gift(pvd, "alice_margatroid", alice());
		gift(pvd, "morichika_rinnosuke", morichika());
	}

	private static List<Tier> reimu() {
		return List.of(
				tier(1, g(GLItems.GHOST_SAKE, 20), g(GLItems.TENGU_SAKE, 20), g(GLItems.FAIRY_CAKE, 10)),
				tier(2, g(GLItems.GHOST_SAKE, 20), g(GLItems.TENGU_SAKE, 20), g(GLItems.FAIRY_CAKE, 10),
						g(GLDecoBlocks.STURDY_TEDDY_BEAR, 10)),
				tier(3, g(GLItems.GHOST_SAKE, 20), g(GLItems.TENGU_SAKE, 20), g(GLItems.FAIRY_CAKE, 10),
						g(GLDecoBlocks.STURDY_TEDDY_BEAR, 10), g(GLNaturalBlocks.STAR_FLOWER, 10),
						g(GLItems.MAGIC_BOOK, 15)),
				tier(4, g(GLItems.GHOST_SAKE, 22), g(GLItems.TENGU_SAKE, 22), g(GLItems.MAGIC_BOOK, 18),
						g(GLNaturalBlocks.STAR_FLOWER, 12), g(GLItems.FAIRY_CAKE, 10),
						g(GLDecoBlocks.STURDY_TEDDY_BEAR, 8)),
				tier(5, g(GLItems.GHOST_SAKE, 24), g(GLItems.TENGU_SAKE, 24), g(GLItems.MAGIC_BOOK, 22),
						g(GLNaturalBlocks.STAR_FLOWER, 14), g(GLItems.FAIRY_CAKE, 10),
						g(GLDecoBlocks.STURDY_TEDDY_BEAR, 6)));
	}

	private static List<Tier> cirno() {
		return List.of(
				tier(1, g(GLDecoBlocks.STURDY_TEDDY_BEAR, 20), g(GLItems.FAIRY_CAKE, 15),
						g(GLItems.TENGU_SAKE, 10)),
				tier(2, g(GLDecoBlocks.STURDY_TEDDY_BEAR, 20), g(GLItems.FAIRY_CAKE, 15),
						g(GLItems.TENGU_SAKE, 10), g(GLItems.GHOST_SAKE, 10)),
				tier(3, g(GLDecoBlocks.STURDY_TEDDY_BEAR, 22), g(GLItems.FAIRY_CAKE, 16),
						g(GLItems.TENGU_SAKE, 10), g(GLItems.GHOST_SAKE, 10),
						g(GLNaturalBlocks.STAR_FLOWER, 10), g(GLItems.MAGIC_BOOK, 5)),
				tier(4, g(GLDecoBlocks.STURDY_TEDDY_BEAR, 22), g(GLItems.FAIRY_CAKE, 16),
						g(GLNaturalBlocks.STAR_FLOWER, 12), g(GLItems.TENGU_SAKE, 10),
						g(GLItems.GHOST_SAKE, 10), g(GLItems.MAGIC_BOOK, 5)),
				tier(5, g(GLDecoBlocks.STURDY_TEDDY_BEAR, 20), g(GLItems.FAIRY_CAKE, 16),
						g(GLNaturalBlocks.STAR_FLOWER, 14), g(GLItems.TENGU_SAKE, 10),
						g(GLItems.GHOST_SAKE, 10), g(GLItems.MAGIC_BOOK, 8)));
	}

	private static List<Tier> marisa() {
		return List.of(
				tier(1, g(GLItems.TENGU_SAKE, 12), g(GLItems.GHOST_SAKE, 12), g(GLItems.FAIRY_CAKE, 12),
						g(GLDecoBlocks.STURDY_TEDDY_BEAR, 12)),
				tier(2, g(GLItems.TENGU_SAKE, 12), g(GLItems.GHOST_SAKE, 12), g(GLItems.FAIRY_CAKE, 12),
						g(GLDecoBlocks.STURDY_TEDDY_BEAR, 12), g(GLNaturalBlocks.STAR_FLOWER, 10)),
				tier(3, g(GLItems.TENGU_SAKE, 12), g(GLItems.GHOST_SAKE, 12), g(GLItems.FAIRY_CAKE, 12),
						g(GLDecoBlocks.STURDY_TEDDY_BEAR, 12), g(GLNaturalBlocks.STAR_FLOWER, 12),
						g(GLItems.MAGIC_BOOK, 12)),
				tier(4, g(GLItems.MAGIC_BOOK, 18), g(GLNaturalBlocks.STAR_FLOWER, 14),
						g(GLItems.TENGU_SAKE, 12), g(GLItems.GHOST_SAKE, 12),
						g(GLItems.FAIRY_CAKE, 8), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 6)),
				tier(5, g(GLItems.MAGIC_BOOK, 24), g(GLNaturalBlocks.STAR_FLOWER, 16),
						g(GLItems.TENGU_SAKE, 12), g(GLItems.GHOST_SAKE, 12),
						g(GLItems.FAIRY_CAKE, 8), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 4)));
	}

	private static List<Tier> sakuya() {
		return List.of(
				tier(1, g(GLItems.GHOST_SAKE, 14), g(GLItems.FAIRY_CAKE, 12), g(GLItems.TENGU_SAKE, 12)),
				tier(2, g(GLItems.GHOST_SAKE, 14), g(GLItems.FAIRY_CAKE, 12), g(GLItems.TENGU_SAKE, 12),
						g(GLDecoBlocks.STURDY_TEDDY_BEAR, 12)),
				tier(3, g(GLItems.GHOST_SAKE, 16), g(GLItems.FAIRY_CAKE, 12), g(GLItems.TENGU_SAKE, 12),
						g(GLDecoBlocks.STURDY_TEDDY_BEAR, 12), g(GLNaturalBlocks.STAR_FLOWER, 12),
						g(GLItems.MAGIC_BOOK, 14)),
				tier(4, g(GLItems.MAGIC_BOOK, 18), g(GLItems.GHOST_SAKE, 16),
						g(GLNaturalBlocks.STAR_FLOWER, 14), g(GLItems.TENGU_SAKE, 12),
						g(GLItems.FAIRY_CAKE, 10), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 8)),
				tier(5, g(GLItems.MAGIC_BOOK, 22), g(GLItems.GHOST_SAKE, 18),
						g(GLNaturalBlocks.STAR_FLOWER, 16), g(GLItems.TENGU_SAKE, 12),
						g(GLItems.FAIRY_CAKE, 10), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 6)));
	}

	private static List<Tier> alice() {
		return List.of(
				tier(1, g(GLDecoBlocks.STURDY_TEDDY_BEAR, 14), g(GLItems.GHOST_SAKE, 12),
						g(GLItems.FAIRY_CAKE, 12)),
				tier(2, g(GLDecoBlocks.STURDY_TEDDY_BEAR, 14), g(GLItems.GHOST_SAKE, 12),
						g(GLItems.FAIRY_CAKE, 12), g(GLItems.TENGU_SAKE, 12)),
				tier(3, g(GLDecoBlocks.STURDY_TEDDY_BEAR, 16), g(GLItems.GHOST_SAKE, 12),
						g(GLItems.FAIRY_CAKE, 12), g(GLItems.TENGU_SAKE, 12),
						g(GLNaturalBlocks.STAR_FLOWER, 12), g(GLItems.MAGIC_BOOK, 12)),
				tier(4, g(GLItems.MAGIC_BOOK, 18), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 18),
						g(GLNaturalBlocks.STAR_FLOWER, 14), g(GLItems.GHOST_SAKE, 12),
						g(GLItems.FAIRY_CAKE, 10), g(GLItems.TENGU_SAKE, 8)),
				tier(5, g(GLItems.MAGIC_BOOK, 24), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 20),
						g(GLNaturalBlocks.STAR_FLOWER, 16), g(GLItems.GHOST_SAKE, 12),
						g(GLItems.FAIRY_CAKE, 10), g(GLItems.TENGU_SAKE, 6)));
	}

	private static List<Tier> morichika() {
		return List.of(
				tier(1, g(GLItems.FAIRY_CAKE, 14), g(GLItems.TENGU_SAKE, 14),
						g(GLItems.GHOST_SAKE, 14), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 12)),
				tier(2, g(GLItems.FAIRY_CAKE, 14), g(GLItems.TENGU_SAKE, 14),
						g(GLItems.GHOST_SAKE, 14), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 12),
						g(GLNaturalBlocks.STAR_FLOWER, 10)),
				tier(3, g(GLItems.FAIRY_CAKE, 14), g(GLItems.TENGU_SAKE, 14),
						g(GLItems.GHOST_SAKE, 14), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 12),
						g(GLNaturalBlocks.STAR_FLOWER, 12), g(GLItems.MAGIC_BOOK, 10)),
				tier(4, g(GLItems.FAIRY_CAKE, 12), g(GLItems.TENGU_SAKE, 14),
						g(GLItems.GHOST_SAKE, 14), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 12),
						g(GLNaturalBlocks.STAR_FLOWER, 14), g(GLItems.MAGIC_BOOK, 12)),
				tier(5, g(GLItems.FAIRY_CAKE, 12), g(GLItems.TENGU_SAKE, 14),
						g(GLItems.GHOST_SAKE, 14), g(GLDecoBlocks.STURDY_TEDDY_BEAR, 12),
						g(GLNaturalBlocks.STAR_FLOWER, 16), g(GLItems.MAGIC_BOOK, 16)));
	}

	private static void gift(RegistrateLootTableProvider pvd, String character, List<Tier> tiers) {
		pvd.addLootAction(SET, c -> c.accept(key(character), table(tiers)));
	}

	private static ResourceKey<LootTable> key(String character) {
		return ResourceKey.create(Registries.LOOT_TABLE,
				ResourceLocation.fromNamespaceAndPath(GensokyoLegacy.MODID, PartyGift.TABLE_DIR + "/" + character));
	}

	/**
	 * One pool per tier, each gated on its own disjoint band of appeal.
	 * <p>
	 * The top tier is left unbounded rather than capped at some appeal ceiling: a banquet
	 * that good should still get the biggest gift the table has, not fall off the end of
	 * the ladder.
	 */
	private static LootTable.Builder table(List<Tier> tiers) {
		var table = LootTable.lootTable();
		for (int i = 0; i < tiers.size(); i++) {
			int index = i;
			var tier = tiers.get(index);
			var pool = LootPool.lootPool()
					.setRolls(ConstantValue.exactly(1f))
					.setBonusRolls(ConstantValue.exactly(0f))
					.when(() -> band(index, tiers.size()));
			for (var gift : tier.gifts()) {
				pool.add(LootItem.lootTableItem(gift.getKey())
						.setWeight(gift.getValue())
						.apply(SetItemCountFunction.setCount(ConstantValue.exactly(tier.count()))));
			}
			table.withPool(pool);
		}
		return table;
	}

	/** The appeal band tier {@code index} answers to, out of {@code total}. */
	private static AppealConsumedCondition band(int index, int total) {
		int min = index * APPEAL_PER_TIER;
		return index == total - 1 ? new AppealConsumedCondition(min)
				: new AppealConsumedCondition(min, min + APPEAL_PER_TIER - 1);
	}

	/** How many of her gift a tier gives, and what she may hand out at it. */
	private record Tier(int count, List<Map.Entry<Item, Integer>> gifts) {
	}

	@SafeVarargs
	private static Tier tier(int count, Map.Entry<Item, Integer>... gifts) {
		return new Tier(count, List.of(gifts));
	}

	private static Map.Entry<Item, Integer> g(ItemLike item, int weight) {
		return Map.entry(item.asItem(), weight);
	}

}