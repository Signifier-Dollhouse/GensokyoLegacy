package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.entity.behavior.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * What a guest leaves behind: one roll of her own loot table at {@code <namespace>:party_gift/<entity type path>},
 * falling back to {@link #DEFAULT_TABLE} so a guest with no table of her own still gives something.
 *
 * @see "doc/design/party/send.md"
 */
public class PartyGift {

	/**
	 * Folder the gift tables live in, under their own character type's namespace.
	 */
	public static final String TABLE_DIR = "party_gift";

	/**
	 * Used for a guest who has no table of her own.
	 */
	public static final ResourceKey<LootTable> DEFAULT_TABLE =
			ResourceKey.create(Registries.LOOT_TABLE, GensokyoLegacy.loc(TABLE_DIR + "/default"));

	/**
	 * Drop a gift on the guest's table, once per visit — the once-only flag lives on her module.
	 * Generosity is how far up her table's ladder the appeal she ate reached, not a bigger stack: a
	 * gift is always exactly one item, and only that appeal band answers.
	 *
	 * @return whether a gift was left
	 */
	public static boolean give(ServerLevel level, YoukaiEntity guest, PartyModule party) {
		if (party.gifted()) return false;
		var table = party.table();
		if (table == null) return false;
		var gift = roll(level, guest, table);
		if (gift == null) return false;
		party.markGifted();
		Block.popResource(level, table.above(), gift);
		return true;
	}

	/**
	 * One gift out of her table, or null. The guest goes in as {@link LootContextParams#THIS_ENTITY},
	 * which is what {@link AppealConsumedCondition} reads — hence
	 * {@link LootContextParamSets#ADVANCEMENT_REWARD}, the only stock param set carrying both.
	 */
	@Nullable
	private static ItemStack roll(ServerLevel level, YoukaiEntity guest, BlockPos table) {
		var loot = tableOf(level, guest);
		if (loot == null) return null;
		var params = new LootParams.Builder(level)
				.withParameter(LootContextParams.THIS_ENTITY, guest)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(table.above()))
				.create(LootContextParamSets.ADVANCEMENT_REWARD);
		var stacks = new ArrayList<ItemStack>();
		loot.getRandomItems(params, stacks::add);
		// a generated table is a ladder of pools whose bands partition the appeal range, so
		// exactly one answers; a datapack that ungates two buries both, and that is its mistake
		return stacks.isEmpty() ? null : stacks.getFirst();
	}

	/**
	 * Her own table, or {@link #DEFAULT_TABLE} when she has not got one. Asks the lookup directly:
	 * the reloadable registries answer {@code LootTable.EMPTY} for both "no table" and "a table
	 * that gives nothing"; only the first wants the fallback.
	 */
	@Nullable
	private static LootTable tableOf(ServerLevel level, YoukaiEntity guest) {
		var lookup = level.getServer().reloadableRegistries().get().lookup(Registries.LOOT_TABLE).orElse(null);
		if (lookup == null) return null;
		return lookup.get(keyOf(guest.getType())).or(() -> lookup.get(DEFAULT_TABLE))
				.map(Holder::value).orElse(null);
	}

	/**
	 * Her own gift table, which nothing answers to for a guest with no file of her own — that is
	 * {@link #DEFAULT_TABLE}'s whole job, and {@link #tableOf} is where the two are joined.
	 */
	public static ResourceKey<LootTable> keyOf(EntityType<?> type) {
		var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		if (id == null) return DEFAULT_TABLE;
		return ResourceKey.create(Registries.LOOT_TABLE,
				ResourceLocation.fromNamespaceAndPath(id.getNamespace(), TABLE_DIR + "/" + id.getPath()));
	}

	private PartyGift() {
	}

}