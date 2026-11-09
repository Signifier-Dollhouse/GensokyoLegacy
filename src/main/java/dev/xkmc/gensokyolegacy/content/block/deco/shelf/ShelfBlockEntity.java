package dev.xkmc.gensokyolegacy.content.block.deco.shelf;

import dev.xkmc.gensokyolegacy.content.attachment.storage.PendingItemStorage;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2library.base.tile.BaseBlockEntity;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
	 * Player who initiated the current break, recorded by {@code ShelfBlock.playerWillDestroy} before
	 * removal. Transient: never serialized, consumed by the universal removal flush below.
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
			if (held.is(GLItems.COIN.get())) {
				if (!level.isClientSide()) {
					cost = held.getCount();
					notifyTile();
				}
				return true;
			}
			if (held.is(GLItems.COIN_STACK.get())) {
				if (!level.isClientSide()) {
					cost = held.getCount() * GLItems.COIN_STACK_VALUE;
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
			if (!ItemStack.isSameItemSameTags(stack, held)) {
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
	 * Pay all accumulated coin earnings to the player, as coin stacks first.
	 * Must be called on server side. Returns true if anything was paid out.
	 */
	public boolean withdraw(Player player) {
		if (earning <= 0) return false;
		int stacks = earning / GLItems.COIN_STACK_VALUE;
		int rest = earning % GLItems.COIN_STACK_VALUE;
		earning = 0;
		while (stacks > 0) {
			int n = Math.min(stacks, 64);
			stacks -= n;
			player.getInventory().placeItemBackInInventory(new ItemStack(GLItems.COIN_STACK.get(), n));
		}
		if (rest > 0)
			player.getInventory().placeItemBackInInventory(new ItemStack(GLItems.COIN.get(), rest));
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
	 * Universal removal flush, called from {@code onReplaced} for every removal cause, while the block
	 * entity is still present. Nothing is ever voided: the recorded breaker drops the shelf's stock and
	 * earnings on the ground, anyone else sends them to {@link PendingItemStorage} for periodic delivery
	 * once the owner is online with room, and an unowned shelf drops in place.
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
		int stacks = earning / GLItems.COIN_STACK_VALUE;
		int rest = earning % GLItems.COIN_STACK_VALUE;
		while (stacks > 0) {
			int n = Math.min(stacks, 64);
			stacks -= n;
			Block.popResource(level, pos, new ItemStack(GLItems.COIN_STACK.get(), n));
		}
		if (rest > 0)
			Block.popResource(level, pos, new ItemStack(GLItems.COIN.get(), rest));
	}

	public Component getTitle() {
		return Component.literal("¥" + cost + " (" + stock + ")");
	}

	public boolean buy(Level level, ItemStack held, Player player) {
		if (stack.isEmpty() || stock <= 0) return false;
		int coins = 0, stacks = 0;
		for (var e : player.getInventory().items) {
			if (e.is(GLItems.COIN.get())) coins += e.getCount();
			else if (e.is(GLItems.COIN_STACK.get())) stacks += e.getCount();
		}
		if (coins + stacks * GLItems.COIN_STACK_VALUE < cost) return false;
		if (!level.isClientSide()) {
			stock--;
			earning += cost;

			// coins first, then whole stacks; the last stack may overpay, so its change is paid back in coins
			int coinCost = Math.min(coins, cost);
			int stackCost = Math.min(stacks,
					(cost - coinCost + GLItems.COIN_STACK_VALUE - 1) / GLItems.COIN_STACK_VALUE);
			int change = coinCost + stackCost * GLItems.COIN_STACK_VALUE - cost;
			for (var e : player.getInventory().items) {
				if (e.is(GLItems.COIN_STACK.get()) && stackCost > 0) {
					int remove = Math.min(stackCost, e.getCount());
					e.shrink(remove);
					stackCost -= remove;
				}
			}
			for (var e : player.getInventory().items) {
				if (e.is(GLItems.COIN.get()) && coinCost > 0) {
					int remove = Math.min(coinCost, e.getCount());
					e.shrink(remove);
					coinCost -= remove;
				}
			}
			if (change > 0)
				player.getInventory().placeItemBackInInventory(new ItemStack(GLItems.COIN.get(), change));
			player.getInventory().placeItemBackInInventory(stack.copyWithCount(1));
			notifyTile();
		}
		return true;
	}

}
