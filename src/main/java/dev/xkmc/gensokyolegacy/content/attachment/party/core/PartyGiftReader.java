package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * What every guest can leave behind, read off her table rather than rolled, so a chance is the
 * exact share the roll would have picked and not merely an observation. Loot tables are never
 * synced, so this is what a client is told and all a page about a gift can know.
 *
 * @see "doc/design/party/send.md"
 */
public class PartyGiftReader {

	private PartyGiftReader() {
	}

	/**
	 * One ladder per party-eligible character, keyed by her entity type: a character with no table
	 * of her own still has one, holding the {@link PartyGift#DEFAULT_TABLE} she falls back to.
	 * Only those — this is all a client is told, and a page is the only thing needing them.
	 */
	public static LinkedHashMap<ResourceLocation, ArrayList<GiftTier>> all(MinecraftServer server) {
		var lookup = server.reloadableRegistries().get().lookup(Registries.LOOT_TABLE).orElse(null);
		LinkedHashMap<ResourceLocation, ArrayList<GiftTier>> ans = new LinkedHashMap<>();
		if (lookup == null) return ans;
		for (var pair : GLMeta.PARTY_TASTE.getAll(server.registryAccess()).toList()) {
			var type = pair.getFirst().value();
			var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			if (id == null) continue;
			LootTable table = lookup.get(PartyGift.keyOf(type)).or(() -> lookup.get(PartyGift.DEFAULT_TABLE))
					.map(Holder::value).orElse(LootTable.EMPTY);
			ans.put(id, of(table));
		}
		return ans;
	}

	/**
	 * The ladder in {@code table}: one tier per pool, in the order the pools are written.
	 */
	public static ArrayList<GiftTier> of(LootTable table) {
		ArrayList<GiftTier> ans = new ArrayList<>();
		for (LootPool pool : table.pools) ans.add(tier(pool));
		return ans;
	}

	/**
	 * One pool as one rung. Only a pool gated outright by one appeal band says which appeal
	 * reaches it; anything else answers at whatever appeal the roll finds, which as far as a page
	 * goes is all of them.
	 */
	private static GiftTier tier(LootPool pool) {
		ArrayList<Weighted> entries = new ArrayList<>();
		boolean said = flatten(pool.entries, entries);
		int total = 0;
		for (var entry : entries) total += entry.weight();
		var gifts = new ArrayList<GiftEntry>(entries.size());
		for (var entry : entries)
			gifts.add(new GiftEntry(entry.stack(), said && total > 0 ? (float) entry.weight() / total : null));
		int min = 0;
		int max = 0;
		for (var condition : pool.conditions)
			if (condition instanceof AppealConsumedCondition appeal && !appeal.invert()) {
				min = appeal.min();
				max = appeal.max();
			}
		return new GiftTier(min, max, gifts);
	}

	/**
	 * The item entries of a pool, a group flattened into its children: a gift is one item drawn
	 * from the rung, so a group inside one is the rung told twice rather than an alternative.
	 * False if some entry names no item outright, whose share of the draw no page can divide up.
	 */
	private static boolean flatten(List<LootPoolEntryContainer> entries, List<Weighted> ans) {
		boolean said = true;
		for (LootPoolEntryContainer entry : entries) {
			if (entry instanceof LootItem item)
				// luck is never set on a gift roll, so an entry weighs exactly its own weight
				ans.add(new Weighted(new ItemStack(item.item), Math.max(item.weight, 0)));
			else if (entry instanceof CompositeEntryBase composite)
				said &= flatten(composite.children, ans);
			else
				said = false;
		}
		return said;
	}

	/** One entry of a rung: the item, and the mass it carries in the draw over the others. */
	private record Weighted(ItemStack stack, int weight) {
	}

}