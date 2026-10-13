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
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * The gift each party guest leaves behind, as a loot table per character.
 * <p>
 * One table per character at {@code gensokyolegacy:party_gift/<entity type path>}, read by
 * {@link PartyGift}. A datapack replaces one by shipping its own file at the same path; a
 * character with no file of her own falls back to {@link #DEFAULT}.
 * <p>
 * A table is a **ladder of pools, one per generosity step**, each one roll, and the appeal
 * the guest personally consumed picks her rung. Every pool is gated by an
 * {@link AppealConsumedCondition} over a **band** of appeal — {@code min} inclusive,
 * {@code max} exclusive, {@code max <= 0} meaning no ceiling — and the bands tile the range
 * without gap or overlap, so exactly one pool ever answers a roll. What makes generosity
 * visible is therefore not how many she gives (a gift is always exactly one item) but how
 * far up the ladder she can reach: an under-served guest reaches for a star flower, a
 * lavish banquet lets her reach for the magic book and then for something of her own.
 * <p>
 * A rung is written {@code tier(min, max, …)} and means whatever it is given — the bands
 * are **not** derived from {@link #APPEAL_PER_TIER} or {@link #TIERS}, which survive only
 * as the numbers they were first measured against. How many rungs a character needs, and how
 * far apart, says how much she eats and how steeply her tastes run out; Cirno climbs a
 * three-rung ladder to 40, Sakuya a four-rung one to 400.
 * <p>
 * Bands rather than floors, so the rungs do not overlap: at a banquet the ladder does not
 * widen into every gift the host could afford, it moves up to what she actually wants. That
 * also makes the gate a pool condition rather than an entry condition, which is cheaper —
 * a pool that fails costs one condition test instead of re-testing every entry in it.
 * <p>
 * A rung is written out rather than read from a tag, even where the mod already has one
 * that means exactly this — {@code #gensokyolegacy:talisman},
 * {@code #gensokyolegacy:morichika_offers}. A loot entry can only name one item, so a tag
 * has to be expanded at datagen either way, and the datagen holder lookup hands back tags
 * that are still being constructed and throw when they are dereferenced. The table is what
 * ships, so the list is what has to be right.
 *
 * @see doc/design/party/send.md
 */
public class GiftLootGen {

	/**
	 * Appeal that buys the guest one more step of generosity.
	 */
	public static final int APPEAL_PER_TIER = 20;

	/**
	 * Generosity steps, from the plain gift up to the one that costs her something.
	 */
	public static final int TIERS = 5;

	/**
	 * {@code advancement_reward} because that is the stock param set carrying both halves
	 * of the context a gift table needs: the guest herself, for
	 * {@link AppealConsumedCondition}, and an origin.
	 */
	private static final LootContextParamSet SET = LootContextParamSets.ADVANCEMENT_REWARD;

	/**
	 * The even spread a guest with no table of her own falls back to, and no own stock —
	 * so her ladder is three generic rungs and stops, rather than reaching a top one.
	 */
	private static final List<Tier> DEFAULT = List.of(
			tier(0, 50, gift(GLNaturalBlocks.STAR_FLOWER, 10), gift(GLItems.FAIRY_CAKE, 10)),
			tier(50, 100, gift(GLItems.TENGU_SAKE, 10), gift(GLItems.GHOST_SAKE, 10),
					gift(GLItems.FAIRY_CAKE, 10), gift(GLDecoBlocks.STURDY_TEDDY_BEAR, 10)),
			tier(100, 150, gift(GLItems.TENGU_SAKE, 20), gift(GLItems.GHOST_SAKE, 20),
					gift(GLItems.MAGIC_BOOK, 20), gift(GLDecoBlocks.STURDY_TEDDY_BEAR, 20)));

	public static void genLoot(RegistrateLootTableProvider pvd) {
		pvd.addLootAction(SET, c -> c.accept(PartyGift.DEFAULT_TABLE, table(DEFAULT)));
		gift(pvd, "hakurei_reimu", reimu());
		gift(pvd, "cirno", cirno());
		gift(pvd, "kirisame_marisa", marisa());
		gift(pvd, "izayoi_sakuya", sakuya());
		gift(pvd, "alice_margatroid", alice());
		gift(pvd, "morichika_rinnosuke", morichika());
		gift(pvd, "ibuki_suika", suika());
	}

	private static List<Tier> reimu() {
		return List.of(
				tier(0, 50, gift(GLNaturalBlocks.STAR_FLOWER, 12)),
				tier(50, 100, gift(GLItems.GHOST_SAKE, 20),
						gift(GLItems.TENGU_SAKE, 20),
						gift(GLDecoBlocks.STURDY_TEDDY_BEAR, 40)),
				tier(100, 200, gift(GLTalismans.SPEED_TALISMAN, 10),
						gift(GLTalismans.HYDROPHOBIC_TALISMAN, 10),
						gift(GLTalismans.LAVA_TALISMAN, 10)),
				tier(200, 0, gift(GLTalismans.HEAL_TALISMAN, 20),
						gift(GLTalismans.SHELTER_TALISMAN, 20),
						gift(GLTalismans.LAVA_TALISMAN, 10)));
	}

	private static List<Tier> cirno() {
		return List.of(
				tier(0, 20, gift(GLNaturalBlocks.STAR_FLOWER, 20),
						gift(GLItems.FAIRY_CAKE, 20)),
				tier(20, 40, gift(GLItems.FAIRY_CAKE, 20),
						gift(GLDecoBlocks.STURDY_TEDDY_BEAR, 20),
						gift(GLItems.FROZEN_FROG_COLD, 10),
						gift(GLItems.FROZEN_FROG_WARM, 10),
						gift(GLItems.FROZEN_FROG_TEMPERATE, 10)),
				tier(40, 0, gift(GLItems.FAIRY_ICE_CRYSTAL, 20),
						gift(GLItems.FROZEN_FROG_COLD, 5),
						gift(GLItems.FROZEN_FROG_WARM, 5),
						gift(GLItems.FROZEN_FROG_TEMPERATE, 5))
		);
	}

	private static List<Tier> marisa() {
		return List.of(
				tier(0, 50, gift(GLNaturalBlocks.STAR_FLOWER, 20)),
				tier(50, 100, gift(GLItems.TENGU_SAKE, 10),
						gift(GLItems.GHOST_SAKE, 10),
						gift(GLItems.MAGIC_BOOK, 20)),
				tier(100, 200, gift(HexBrew.EXPLOSIVE_HEXBREW.bottle, 10),
						gift(HexBrew.MIASMA_HEXBREW.bottle, 10),
						gift(HexBrew.MUNDANE_HEXBREW.bottle, 10),
						gift(HexBrew.HYPHAE_HEXBREW.bottle, 10)),
				tier(200, 0, gift(HexBrew.HEXBREW_ELIXIR.bottle, 10),
						gift(HexBrew.STARLIGHT_HEXBREW.bottle, 10),
						gift(HexBrew.SHIELD_HEXBREW.bottle, 10)));
	}

	private static List<Tier> sakuya() {
		return List.of(
				tier(0, 50, gift(GLNaturalBlocks.STAR_FLOWER, 20)),
				tier(50, 100, gift(GLItems.TENGU_SAKE, 10),
						gift(GLItems.GHOST_SAKE, 10)),
				tier(100, 400, gift(GLItems.IRON_DAGGER, 10)),
				tier(400, 0, gift(GLItems.DAGGER_GLOVE, 10)));
	}

	private static List<Tier> alice() {
		return List.of(
				tier(0, 50, gift(GLNaturalBlocks.STAR_FLOWER, 20)),
				tier(50, 100, gift(GLDecoBlocks.STURDY_TEDDY_BEAR, 20)),
				tier(100, 400, gift(GLItems.DOLL_LANCE, 10)),
				tier(400, 0, gift(GLItems.DOLL, 10)));
	}

	private static List<Tier> morichika() {
		return List.of(
				tier(0, 50, gift(GLNaturalBlocks.STAR_FLOWER, 20)),
				tier(50, 100, gift(GLItems.TENGU_SAKE, 10),
						gift(GLItems.GHOST_SAKE, 10),
						gift(GLDecoBlocks.STURDY_TEDDY_BEAR, 20)),
				tier(100, 200, gift(GLItems.STRANGE_GLASSES, 10),
						gift(GLItems.CAT_BELL, 10),
						gift(GLItems.MERMAID_PEARL, 10),
						gift(GLItems.STRAW_HAT, 10)),
				tier(200, 0, gift(GLItems.MINI_HAKKERO_PROTOTYPE, 10),
						gift(GLItems.CENTIPICKAXE, 10),
						gift(GLItems.DOWSER, 10)));
	}

	/**
	 * An oni leaves you with drink, and only drink, and she is not fussy about which
	 * kind: her ladder is short and the top rung is the expensive bottle rather than
	 * something of her own, because she has nothing of her own worth giving.
	 */
	private static List<Tier> suika() {
		return List.of(
				tier(0, 50, gift(GLNaturalBlocks.STAR_FLOWER, 20)),
				tier(50, 0, gift(GLItems.TENGU_SAKE, 20),
						gift(GLItems.GHOST_SAKE, 20)));
	}

	/**
	 * Her rungs.
	 */
	private static void gift(RegistrateLootTableProvider pvd, String character, List<Tier> tiers) {
		pvd.addLootAction(SET, c -> c.accept(key(character), table(tiers)));
	}

	private static ResourceKey<LootTable> key(String character) {
		return ResourceKey.create(Registries.LOOT_TABLE,
				ResourceLocation.fromNamespaceAndPath(GensokyoLegacy.MODID, PartyGift.TABLE_DIR + "/" + character));
	}

	/**
	 * One pool per rung, each answering only for its own band of appeal, so exactly one of
	 * them ever runs its roll. A guest served past the top rung stays there and keeps
	 * drawing from the top pool rather than falling off the end of the ladder — which is
	 * what the open-ended {@code max} of {@code 0} buys.
	 */
	private static LootTable.Builder table(List<Tier> ladder) {
		var table = LootTable.lootTable();
		for (Tier tier : ladder) {
			table.withPool(pool(tier));
		}
		return table;
	}

	/**
	 * One pool, one roll: a gift is always exactly one item, and how good an item she can
	 * reach is what the appeal buys. The condition is on the pool rather than on its
	 * entries, so a rung that fails costs one test instead of one per entry.
	 */
	private static LootPool.Builder pool(Tier tier) {
		var pool = LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1f))
				.setBonusRolls(ConstantValue.exactly(0f))
				.when(() -> new AppealConsumedCondition(tier.min, tier.max));
		for (var entry : tier.gifts()) {
			pool.add(LootItem.lootTableItem(entry.item()).setWeight(entry.weight()));
		}
		return pool;
	}

	/**
	 * One rung of a character's ladder: the band of appeal it answers for, and what she is
	 * willing to hand over while standing in it.
	 *
	 * @param min appeal that opens the rung, inclusive
	 * @param max appeal that closes it, exclusive; {@code 0} or less means it never closes
	 */
	private record Tier(List<Entry> gifts, int min, int max) {
	}

	/**
	 * One gift: what it is and how likely she is to pick it.
	 */
	private record Entry(Item item, int weight) {
	}

	/**
	 * A rung of some width, holding what she will hand over over that whole band.
	 */
	private static Tier tier(int min, int max, Entry... gifts) {
		return new Tier(List.of(gifts), min, max);
	}

	private static Entry gift(ItemLike item, int weight) {
		return new Entry(item.asItem(), weight);
	}

}