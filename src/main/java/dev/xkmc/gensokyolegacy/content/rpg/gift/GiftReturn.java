package dev.xkmc.gensokyolegacy.content.rpg.gift;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.core.GiftDialogData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/**
 * The gift a character hands back, rolled from the {@code gift_return} table her {@code gift_dialog}
 * datamap entry names. The table is copied at datagen from the second rung of her party gift ladder,
 * so what she returns is drawn from the same stock she would leave behind as a guest — one rung down,
 * to price a gift that took many gifts to earn rather than one that took a whole visit.
 *
 * @see "doc/design/gift.md"
 */
public class GiftReturn {

	/** Folder the return tables live in, under their own character type's namespace. */
	public static final String TABLE_DIR = "gift_return";

	/**
	 * One item out of her return table, or empty. The character goes in as
	 * {@link LootContextParams#THIS_ENTITY} under the same param set {@code PartyGift} uses, so a
	 * datapack that carries over an appeal gate still reads correctly.
	 */
	public static ItemStack roll(ServerPlayer sp, YoukaiEntity ch, GiftDialogData config) {
		if (config.returnTable() == null) return ItemStack.EMPTY;
		var lookup = sp.serverLevel().getServer().reloadableRegistries().get().lookup(Registries.LOOT_TABLE).orElse(null);
		if (lookup == null) return ItemStack.EMPTY;
		var key = ResourceKey.create(Registries.LOOT_TABLE, config.returnTable());
		var loot = lookup.get(key).map(holder -> holder.value()).orElse(null);
		if (loot == null || loot == LootTable.EMPTY) return ItemStack.EMPTY;
		var stacks = new ArrayList<ItemStack>();
		loot.getRandomItems(params(sp, ch), stacks::add);
		return stacks.isEmpty() ? ItemStack.EMPTY : stacks.getFirst();
	}

	private static LootParams params(ServerPlayer sp, YoukaiEntity ch) {
		var level = (ServerLevel) sp.level();
		return new LootParams.Builder(level)
				.withParameter(LootContextParams.THIS_ENTITY, ch)
				.withParameter(LootContextParams.ORIGIN, sp.position())
				.create(LootContextParamSets.ADVANCEMENT_REWARD);
	}

	private GiftReturn() {
	}

}