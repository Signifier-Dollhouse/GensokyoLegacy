package dev.xkmc.gensokyolegacy.content.item.currency;

import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Coin arithmetic shared by the shelf and the trade menu: a coin stack is worth
 * {@link GLItems#COIN_STACK_VALUE} coins and is split only when loose coins cannot cover a cost.
 */
public final class CoinPurse {

	private CoinPurse() {
	}

	/** Coin value the player can pay with: loose coins plus coin stacks at their coin value. */
	public static int coins(Player player) {
		int ans = 0;
		for (var stack : player.getInventory().items) {
			if (stack.is(GLItems.COIN.get())) ans += stack.getCount();
			else if (stack.is(GLItems.COIN_STACK.get())) ans += stack.getCount() * GLItems.COIN_STACK_VALUE;
		}
		return ans;
	}

	/** Currency the player carries for display; every tagged stack counts 1 except a coin stack. */
	public static int money(Player player) {
		int ans = 0;
		for (var stack : player.getInventory().items) {
			if (stack.is(GLItems.COIN_STACK.get())) ans += stack.getCount() * GLItems.COIN_STACK_VALUE;
			else if (stack.is(GLTagGen.CURRENCY)) ans += stack.getCount();
		}
		return ans;
	}

	/** A read-only view of the main inventory with every coin stack replaced by its coin value. */
	public static List<ItemStack> asCoins(Player player) {
		List<ItemStack> ans = new ArrayList<>();
		for (var stack : player.getInventory().items) {
			if (stack.is(GLItems.COIN_STACK.get()))
				ans.add(new ItemStack(GLItems.COIN.get(), stack.getCount() * GLItems.COIN_STACK_VALUE));
			else ans.add(stack);
		}
		return ans;
	}

	/** Splits whole coin stacks into loose coins until the loose coins cover {@code cost}. */
	public static void split(Player player, int cost) {
		if (cost <= 0) return;
		int loose = 0;
		for (var stack : player.getInventory().items)
			if (stack.is(GLItems.COIN.get())) loose += stack.getCount();
		if (loose >= cost) return;
		int take = (cost - loose + GLItems.COIN_STACK_VALUE - 1) / GLItems.COIN_STACK_VALUE;
		int taken = 0;
		for (var stack : player.getInventory().items) {
			if (take <= 0) break;
			if (stack.is(GLItems.COIN_STACK.get())) {
				int remove = Math.min(take, stack.getCount());
				stack.shrink(remove);
				take -= remove;
				taken += remove;
			}
		}
		if (taken > 0)
			player.getInventory().placeItemBackInInventory(new ItemStack(GLItems.COIN.get(), taken * GLItems.COIN_STACK_VALUE));
	}

	/** Pays {@code cost} coins, splitting stacks as needed; the overpaid change stays as coins. */
	public static boolean pay(Player player, int cost) {
		if (coins(player) < cost) return false;
		split(player, cost);
		int left = cost;
		for (var stack : player.getInventory().items) {
			if (left <= 0) break;
			if (stack.is(GLItems.COIN.get())) {
				int remove = Math.min(left, stack.getCount());
				stack.shrink(remove);
				left -= remove;
			}
		}
		return true;
	}

}
