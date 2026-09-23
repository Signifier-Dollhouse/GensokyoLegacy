package dev.xkmc.gensokyolegacy.content.attachment.storage;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.l2core.capability.level.BaseSavedData;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * Temporary server-side storage for item payouts that could not be handed
 * over directly (e.g. shop shelf contents lost to a break the owner did not
 * perform). Stock and earnings are stashed under the owner UUID and handed
 * back by periodic delivery once the owner is online with room. Entries never
 * expire on their own; they are removed once fully delivered.
 */
@SerialClass
public class PendingItemStorage extends BaseSavedData<PendingItemStorage> {

	public static final Component RETURN_MSG = GLLang.Misc.MSG_SHELF_RETURNED.get();

	private static final String ID = GensokyoLegacy.MODID + "_pending_items";
	private static final Factory<PendingItemStorage> FACTORY = new Factory<>(PendingItemStorage::new, PendingItemStorage::new);

	public static PendingItemStorage get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
	}

	@SerialField
	private final LinkedHashMap<UUID, ArrayList<PendingItemData>> pending = new LinkedHashMap<>();

	public PendingItemStorage() {
		super(PendingItemStorage.class);
	}

	private PendingItemStorage(CompoundTag tag, HolderLookup.Provider pvd) {
		super(PendingItemStorage.class);
		new TagCodec(pvd).fromTag(tag, PendingItemStorage.class, this);
	}

	/**
	 * Stash one payout of stock and earnings under the owner.
	 * Entries for the same item are merged; different items queue separately.
	 * Earnings with no stock merge into an earning-only entry when present.
	 */
	public void stash(UUID owner, ItemStack stack, int stock, int earning) {
		if (owner.equals(Util.NIL_UUID)) return;
		boolean hasStock = !stack.isEmpty() && stock > 0;
		if (!hasStock && earning <= 0) return;
		var list = pending.computeIfAbsent(owner, k -> new ArrayList<>());
		for (var e : list) {
			if (hasStock ? ItemStack.isSameItemSameComponents(e.stack, stack) :
					e.stock <= 0 && e.earning > 0) {
				if (hasStock) e.stock += stock;
				e.earning += earning;
				return;
			}
		}
		list.add(new PendingItemData(hasStock ? stack.copyWithCount(1) : ItemStack.EMPTY, hasStock ? stock : 0, earning));
	}

	public boolean hasPending(UUID owner) {
		var list = pending.get(owner);
		if (list == null) return false;
		for (var e : list) {
			if (!e.isEmpty()) return true;
		}
		return false;
	}

	/**
	 * Hand back everything stashed for the player. Items that do not fit stay
	 * stashed for the next delivery. Returns true if anything was handed over.
	 */
	public boolean deliver(ServerPlayer player) {
		var list = pending.get(player.getUUID());
		if (list == null || list.isEmpty()) return false;
		boolean moved = false;
		var rest = new ArrayList<PendingItemData>();
		for (var e : list) {
			int stockLeft = takeStock(player, e);
			int earnLeft = takeEarnings(player, e.earning);
			moved |= stockLeft < e.stock || earnLeft < e.earning;
			if (stockLeft > 0 || earnLeft > 0)
				rest.add(new PendingItemData(e.stack, stockLeft, earnLeft));
		}
		if (rest.isEmpty()) pending.remove(player.getUUID());
		else pending.put(player.getUUID(), rest);
		return moved;
	}

	private static int takeStock(ServerPlayer player, PendingItemData data) {
		int left = data.stock;
		while (left > 0 && !data.stack.isEmpty()) {
			int n = Math.min(left, data.stack.getMaxStackSize());
			var give = data.stack.copyWithCount(n);
			player.getInventory().add(give);
			int delta = n - give.getCount();
			if (delta <= 0) break;
			left -= delta;
		}
		return left;
	}

	private static int takeEarnings(ServerPlayer player, int earning) {
		int left = earning;
		int blocks = left / 9;
		while (blocks > 0) {
			int n = Math.min(blocks, 64);
			var give = new ItemStack(Items.EMERALD_BLOCK, n);
			player.getInventory().add(give);
			int delta = n - give.getCount();
			if (delta <= 0) break;
			left -= delta * 9;
			blocks -= n;
		}
		if (blocks <= 0 && left % 9 > 0) {
			var give = new ItemStack(Items.EMERALD, left % 9);
			player.getInventory().add(give);
			left -= left % 9 - give.getCount();
		}
		return left;
	}

}
