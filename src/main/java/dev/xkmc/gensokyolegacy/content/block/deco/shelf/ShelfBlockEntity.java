package dev.xkmc.gensokyolegacy.content.block.deco.shelf;

import dev.xkmc.gensokyolegacy.content.attachment.storage.PendingItemStorage;
import dev.xkmc.l2core.base.tile.BaseBlockEntity;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

@SerialClass
public class ShelfBlockEntity extends BaseBlockEntity {

	@SerialField
	public UUID owner = Util.NIL_UUID;
	@SerialField
	public ItemStack stack = ItemStack.EMPTY;
	@SerialField
	public int cost, stock, earning;

	/**
	 * Player who initiated the current break, recorded by
	 * {@code ShelfBlock.playerWillDestroy} before removal. Transient: never
	 * serialized, consumed by the universal removal flush below.
	 */
	public UUID lastBreaker = null;


	public ShelfBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public boolean set(Level level, Player player, ItemStack held, boolean isCreative) {
		if (!stack.isEmpty()) {
			if (held.isEmpty()) {
				if (!level.isClientSide()) {
					Block.popResource(level, getBlockPos(), stack.copyWithCount(stock));
					stack = ItemStack.EMPTY;
					stock = 0;
					withdraw(player);
					notifyTile();
				}
				return true;
			}
			if (held.is(Items.EMERALD)) {
				if (!level.isClientSide()) {
					cost = held.getCount();
					notifyTile();
				}
				return true;
			}
			if (held.is(Items.EMERALD_BLOCK)) {
				if (!level.isClientSide()) {
					cost = held.getCount() * 9;
					notifyTile();
				}
				return true;
			}
		} else if (held.isEmpty()) {
			if (earning <= 0) return false;
			if (!level.isClientSide()) {
				withdraw(player);
				notifyTile();
			}
			return true;
		}
		if (!level.isClientSide()) {
			if (!ItemStack.isSameItemSameComponents(stack, held)) {
				Block.popResource(level, getBlockPos(), stack.copyWithCount(stock));
				stack = held.copyWithCount(1);
				stock = held.getCount();
			} else stock += held.getCount();
			held.shrink(stock);
			cost = 1;
			notifyTile();
		}
		return true;
	}

	public void notifyTile() {
		sync();
		setChanged();
	}

	/**
	 * Pay all accumulated emerald earnings to the player, as emerald blocks first.
	 * Must be called on server side. Returns true if anything was paid out.
	 */
	public boolean withdraw(Player player) {
		if (earning <= 0) return false;
		int blocks = earning / 9;
		int rest = earning % 9;
		earning = 0;
		while (blocks > 0) {
			int n = Math.min(blocks, 64);
			blocks -= n;
			player.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD_BLOCK, n));
		}
		if (rest > 0)
			player.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, rest));
		return true;
	}

	/**
	 * Fill this shelf with a new offer. Called on server side by shopkeeper restock behavior.
	 */
	public void restock(ItemStack display, int stock, int cost) {
		this.stack = display.copyWithCount(1);
		this.stock = stock;
		this.cost = cost;
		notifyTile();
	}

	/**
	 * Universal removal flush, called from {@code onReplaced} for every removal
	 * cause (player break in any mode, explosion, commands, ...). The block
	 * entity is still present at this point. Nothing is ever voided:
	 * the recorded breaker breaking their own shelf drops its stock and
	 * earnings on the ground; a shelf removed by anyone or anything else sends
	 * its contents to {@link PendingItemStorage}, to be handed back by the
	 * periodic delivery once the owner is online with room. Unowned shop
	 * shelves drop in place.
	 */
	public void flushDrops(ServerLevel level, BlockPos pos) {
		var breaker = lastBreaker;
		lastBreaker = null;
		boolean hasStock = !stack.isEmpty() && stock > 0;
		boolean hasEarning = earning > 0;
		if (!hasStock && !hasEarning) return;
		if (!owner.equals(Util.NIL_UUID) && owner.equals(breaker)) {
			if (hasStock) dropStock(level, pos);
			if (hasEarning) dropEarnings(level, pos);
		} else if (!owner.equals(Util.NIL_UUID)) {
			PendingItemStorage.get(level).stash(owner, stack, stock, earning);
		} else {
			if (hasStock) dropStock(level, pos);
			if (hasEarning) dropEarnings(level, pos);
		}
	}

	private void dropStock(ServerLevel level, BlockPos pos) {
		int left = stock;
		while (left > 0) {
			int n = Math.min(left, stack.getMaxStackSize());
			left -= n;
			Block.popResource(level, pos, stack.copyWithCount(n));
		}
	}

	private void dropEarnings(ServerLevel level, BlockPos pos) {
		int blocks = earning / 9;
		int rest = earning % 9;
		while (blocks > 0) {
			int n = Math.min(blocks, 64);
			blocks -= n;
			Block.popResource(level, pos, new ItemStack(Items.EMERALD_BLOCK, n));
		}
		if (rest > 0)
			Block.popResource(level, pos, new ItemStack(Items.EMERALD, rest));
	}

	public Component getTitle() {
		return Component.literal("¥" + cost + " (" + stock + ")");
	}

	public boolean buy(Level level, ItemStack held, Player player) {
		if (stack.isEmpty() || stock <= 0) return false;
		int emerald = 0, blocks = 0;
		for (var e : player.getInventory().items) {
			if (e.is(Items.EMERALD)) emerald += e.getCount();
			if (e.is(Items.EMERALD_BLOCK)) blocks += e.getCount();
		}
		if (emerald + blocks * 9 < cost) return false;
		if (!level.isClientSide()) {
			stock--;
			earning += cost;

			int emeraldCost = Math.min(emerald, cost);
			int blockCost = Math.min(blocks, ((cost - emeraldCost) + 8) / 9);
			emeraldCost = cost - blockCost * 9;
			for (var e : player.getInventory().items) {
				if (e.is(Items.EMERALD_BLOCK) && blockCost > 0) {
					int remove = Math.min(blockCost, e.getCount());
					e.shrink(remove);
					blockCost -= remove * 9;
				}
			}
			if (emeraldCost < 0) {
				player.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, -emeraldCost));
			} else if (emeraldCost > 0) {
				for (var e : player.getInventory().items) {
					if (e.is(Items.EMERALD) && emeraldCost > 0) {
						int remove = Math.min(emeraldCost, e.getCount());
						e.shrink(remove);
						emeraldCost -= remove;
					}
				}
			}
			player.getInventory().placeItemBackInInventory(stack.copyWithCount(1));
			notifyTile();
		}
		return true;
	}

}
