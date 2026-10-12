package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
 * What a guest leaves behind: one roll of her own loot table at
 * {@code <namespace>:party_gift/<entity type path>}, falling back to {@link #DEFAULT_TABLE}
 * so a guest with no table of her own still gives something.
 *
 * @see doc/design/party/send.md
 */
public class PartyGift {

	/** Folder the gift tables live in, under their own character type's namespace. */
	public static final String TABLE_DIR = "party_gift";

	/** Used for a guest who has no table of her own. */
	public static final ResourceKey<LootTable> DEFAULT_TABLE =
			ResourceKey.create(Registries.LOOT_TABLE, GensokyoLegacy.loc(TABLE_DIR + "/default"));

	/**
	 * Drop a gift on the guest's table, once per visit — the once-only flag lives on her
	 * module. Generosity is reach into her pool, not a bigger stack: a gift is always
	 * exactly one item.
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
		// a generated table is one pool of one roll; a datapack that adds a second pool
		// buries both here, and that is its mistake
		return stacks.isEmpty() ? null : stacks.getFirst();
	}

	/**
	 * Her own table, or {@link #DEFAULT_TABLE} when she has not got one. Asks the lookup
	 * directly: the reloadable registries answer {@code LootTable.EMPTY} for both "no
	 * table" and "a table that gives nothing", and only the first wants the fallback.
	 */
	@Nullable
	private static LootTable tableOf(ServerLevel level, YoukaiEntity guest) {
		var lookup = level.getServer().reloadableRegistries().get().lookup(Registries.LOOT_TABLE).orElse(null);
		if (lookup == null) return null;
		var id = BuiltInRegistries.ENTITY_TYPE.getKey(guest.getType());
		if (id == null) return null;
		var own = ResourceKey.create(Registries.LOOT_TABLE,
				ResourceLocation.fromNamespaceAndPath(id.getNamespace(), TABLE_DIR + "/" + id.getPath()));
		return lookup.get(own).or(() -> lookup.get(DEFAULT_TABLE)).map(Holder::value).orElse(null);
	}

	private PartyGift() {
	}

}