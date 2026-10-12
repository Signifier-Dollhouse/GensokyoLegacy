package dev.xkmc.gensokyolegacy.init.data.loot;

import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.AppealConsumedCondition;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyGift;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
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
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Arrays;
import java.util.List;

/**
 * The gift each party guest leaves behind, as a loot table per character.
 * <p>
 * One table per character at {@code gensokyolegacy:party_gift/<entity type path>}, read by
 * {@link PartyGift}. A datapack replaces one by shipping its own file at the same path; a
 * character with no file of her own falls back to {@link #DEFAULT}.
 * <p>
 * Every table is **one pool of one roll**, and what makes generosity visible is not how
 * many she gives — a gift is always exactly one item — but **how much is on the table**.
 * An entry is excluded from the weighted draw entirely until she has been served
 * {@link #APPEAL_PER_TIER} more appeal than the step below it, gated by
 * {@link AppealConsumedCondition}, so a lavish banquet does not hand over five fairy
 * cakes but lets her reach for the magic book and then for something of her own.
 * <p>
 * On top of the generic gift items each character has an **own pool**: what is actually
 * hers, or at least what only she has — Reimu her talismans, Marisa her hexbrew, Cirno the
 * ice and frogs she makes, Sakuya her dagger, Alice her dolls' two weapons, Morichika
 * whatever happens to be on his shelf. It opens at the last step, because that is the one
 * gift that costs her something, and each of its entries is worth {@link #OWN_WEIGHT}
 * against the generic gifts' 10..20: the step decided she may, this only decides how often.
 * <p>
 * A pool is written out rather than read from a tag, even where the mod already has one
 * that means exactly this — `#gensokyolegacy:talisman`, `#gensokyolegacy:morichika_offers`.
 * A loot entry can only name one item, so a tag has to be expanded at datagen either way,
 * and the datagen holder lookup hands back tags that are still being constructed and throw
 * when they are dereferenced. The table is what ships, so the list is what has to be right;
 * the javadoc on each pool names the tag it mirrors.
 */
public class GiftLootGen {

	/** Appeal that buys the guest one more step of generosity. */
	public static final int APPEAL_PER_TIER = 20;

	/** Generosity steps, from the plain gift up to the one that costs her something. */
	public static final int TIERS = 5;

	/**
	 * How readily she parts with something out of her own pool. Low against the generic
	 * gifts' 10..20, because the step already decided she may; this only decides how often.
	 */
	public static final int OWN_WEIGHT = 8;

	/** Appeal at which her own pool opens: the last step, and the only one with no ceiling. */
	private static final int OWN_APPEAL = (TIERS - 1) * APPEAL_PER_TIER;

	/**
	 * {@code advancement_reward} because that is the stock param set carrying both halves
	 * of the context a gift table needs: the guest herself, for
	 * {@link AppealConsumedCondition}, and an origin.
	 */
	private static final LootContextParamSet SET = LootContextParamSets.ADVANCEMENT_REWARD;

	/** The even spread a guest with no table of her own falls back to, and no own pool. */
	private static final List<Entry> DEFAULT = List.of(
			gift(GLItems.FAIRY_CAKE, 10),
			gift(GLItems.TENGU_SAKE, 10),
			gift(GLItems.GHOST_SAKE, 10),
			gift(GLDecoBlocks.STURDY_TEDDY_BEAR, 10),
			from(1, GLNaturalBlocks.STAR_FLOWER, 10),
			from(2, GLItems.MAGIC_BOOK, 10));

	public static void genLoot(RegistrateLootTableProvider pvd) {
		pvd.addLootAction(SET, c -> c.accept(PartyGift.DEFAULT_TABLE, table(DEFAULT, List.of())));
		gift(pvd, "hakurei_reimu", reimu(), talismans());
		gift(pvd, "cirno", cirno(), own(GLItems.FAIRY_ICE_CRYSTAL, GLItems.FROZEN_FROG_COLD,
				GLItems.FROZEN_FROG_WARM, GLItems.FROZEN_FROG_TEMPERATE));
		gift(pvd, "kirisame_marisa", marisa(), hexbrew());
		gift(pvd, "izayoi_sakuya", sakuya(), own(GLItems.IRON_DAGGER));
		gift(pvd, "alice_margatroid", alice(), own(GLItems.DOLL_LANCE, GLItems.STAR_WAND));
		gift(pvd, "morichika_rinnosuke", morichika(), shelf());
	}

	/** Reimu's own pool is {@code #gensokyolegacy:talisman}, the tag her shop trades from. */
	private static List<Item> talismans() {
		return own(GLTalismans.HEAL_TALISMAN, GLTalismans.SPEED_TALISMAN, GLTalismans.HYDROPHOBIC_TALISMAN,
				GLTalismans.LAVA_TALISMAN, GLTalismans.SHELTER_TALISMAN, GLTalismans.FOLDED_PAPER_TALISMAN);
	}

	/**
	 * Morichika's own pool is {@code #gensokyolegacy:morichika_offers}, the tag
	 * {@code YoukaiRestockShelfTask} refills his shelf from — he has nothing of his own,
	 * only stock, and what he parts with is what he would have sold.
	 * <p>
	 * Written out rather than read from the tag: a loot entry can only name one item, so
	 * the tag has to be expanded at datagen either way, and the datagen holder lookup hands
	 * back tags that are still being constructed and cannot be dereferenced. The table is
	 * what ships, so the list is what has to be right.
	 */
	private static List<Item> shelf() {
		return own(GLItems.MINI_HAKKERO_PROTOTYPE, GLItems.CENTIPICKAXE, GLItems.DOWSER,
				GLItems.MERMAID_PEARL, GLItems.CAT_BELL, GLItems.STRAW_HAT, GLItems.STRANGE_GLASSES);
	}

	/** Generic gifts, weighted by her {@code gift_preference}: drink 2.0, book 1.5. */
	private static List<Entry> reimu() {
		return List.of(
				gift(GLItems.GHOST_SAKE, 20),
				gift(GLItems.TENGU_SAKE, 20),
				gift(GLItems.FAIRY_CAKE, 10),
				from(1, GLDecoBlocks.STURDY_TEDDY_BEAR, 10),
				from(2, GLNaturalBlocks.STAR_FLOWER, 12),
				from(2, GLItems.MAGIC_BOOK, 15));
	}

	/** And hers: toy 2.0, food 1.5, and a book she does not care for at 0.5. */
	private static List<Entry> cirno() {
		return List.of(
				gift(GLDecoBlocks.STURDY_TEDDY_BEAR, 20),
				gift(GLItems.FAIRY_CAKE, 15),
				gift(GLItems.TENGU_SAKE, 10),
				from(1, GLItems.GHOST_SAKE, 10),
				from(2, GLNaturalBlocks.STAR_FLOWER, 10),
				from(2, GLItems.MAGIC_BOOK, 5));
	}

	private static List<Entry> marisa() {
		return List.of(
				gift(GLItems.TENGU_SAKE, 12),
				gift(GLItems.GHOST_SAKE, 12),
				gift(GLItems.FAIRY_CAKE, 12),
				gift(GLDecoBlocks.STURDY_TEDDY_BEAR, 12),
				from(1, GLNaturalBlocks.STAR_FLOWER, 12),
				from(2, GLItems.MAGIC_BOOK, 14));
	}

	private static List<Entry> sakuya() {
		return List.of(
				gift(GLItems.GHOST_SAKE, 14),
				gift(GLItems.FAIRY_CAKE, 12),
				gift(GLItems.TENGU_SAKE, 12),
				from(1, GLDecoBlocks.STURDY_TEDDY_BEAR, 12),
				from(2, GLNaturalBlocks.STAR_FLOWER, 12),
				from(2, GLItems.MAGIC_BOOK, 14));
	}

	private static List<Entry> alice() {
		return List.of(
				gift(GLDecoBlocks.STURDY_TEDDY_BEAR, 14),
				gift(GLItems.GHOST_SAKE, 12),
				gift(GLItems.FAIRY_CAKE, 12),
				from(1, GLItems.TENGU_SAKE, 12),
				from(2, GLNaturalBlocks.STAR_FLOWER, 12),
				from(2, GLItems.MAGIC_BOOK, 12));
	}

	private static List<Entry> morichika() {
		return List.of(
				gift(GLItems.FAIRY_CAKE, 14),
				gift(GLItems.TENGU_SAKE, 14),
				gift(GLItems.GHOST_SAKE, 14),
				gift(GLDecoBlocks.STURDY_TEDDY_BEAR, 12),
				from(1, GLNaturalBlocks.STAR_FLOWER, 10),
				from(2, GLItems.MAGIC_BOOK, 10));
	}

	private static void gift(RegistrateLootTableProvider pvd, String character, List<Entry> gifts, List<Item> own) {
		pvd.addLootAction(SET, c -> c.accept(key(character), table(gifts, own)));
	}

	private static ResourceKey<LootTable> key(String character) {
		return ResourceKey.create(Registries.LOOT_TABLE,
				ResourceLocation.fromNamespaceAndPath(GensokyoLegacy.MODID, PartyGift.TABLE_DIR + "/" + character));
	}

	/**
	 * One pool, one roll: a gift is always exactly one item, and how good an item she can
	 * reach is what the appeal buys. An entry that fails its {@link AppealConsumedCondition}
	 * never enters the weighted draw at all, so gating is subtraction from the pool rather
	 * than a second roll — which is what keeps the table to one item however it is written.
	 */
	private static LootTable.Builder table(List<Entry> gifts, List<Item> own) {
		var pool = LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1f))
				.setBonusRolls(ConstantValue.exactly(0f));
		for (var gift : gifts) {
			pool.add(entry(gift));
		}
		for (var item : own) {
			pool.add(entry(new Entry(item, OWN_WEIGHT, OWN_APPEAL)));
		}
		return LootTable.lootTable().withPool(pool);
	}

	private static LootPoolSingletonContainer.Builder<?> entry(Entry gift) {
		var entry = LootItem.lootTableItem(gift.item()).setWeight(gift.weight());
		if (gift.minAppeal() > 0) entry.when(() -> new AppealConsumedCondition(gift.minAppeal()));
		return entry;
	}

	/** One gift: what it is, how likely she is to pick it, and the appeal it needs. */
	private record Entry(Item item, int weight, int minAppeal) {
	}

	/** On the table whatever she was served. */
	private static Entry gift(ItemLike item, int weight) {
		return new Entry(item.asItem(), weight, 0);
	}

	/**
	 * On the table once she has been served {@code band} generosity steps.
	 * <p>
	 * One step is {@link #APPEAL_PER_TIER}, so band 1 is 20, band 2 is 40. A band is only
	 * ever a <b>floor</b>: she keeps everything the lower bands gave her and this joins it.
	 */
	private static Entry from(int band, ItemLike item, int weight) {
		return new Entry(item.asItem(), weight, band * APPEAL_PER_TIER);
	}

	@SafeVarargs
	private static List<Item> own(ItemLike... items) {
		return Arrays.stream(items).map(ItemLike::asItem).toList();
	}

	/** Everything she brews. The enum is already the whole of her stock. */
	private static List<Item> hexbrew() {
		return Arrays.stream(HexBrew.values()).map(e -> e.bottle.asItem()).toList();
	}

}