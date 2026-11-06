package dev.xkmc.gensokyolegacy.content.item.spiritual;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Universal spiritual power stored on an item stack. The cap is a component too, so every item
 * declares its own intrinsic capacity; Yuyuko's fan is the first carrier.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public final class SpiritualPower {

	/**
	 * The pool bar's colour, shared by every carrier so one item's bar cannot drift from another's.
	 */
	public static final int BAR_COLOR = 0x55FFFF;

	private SpiritualPower() {
	}

	public static int get(ItemStack stack) {
		return GLItems.SPIRITUAL_POWER.getOrDefault(stack, 0);
	}

	public static int max(ItemStack stack) {
		return Math.max(0, GLItems.SPIRITUAL_POWER_MAX.getOrDefault(stack, 0));
	}

	public static void set(ItemStack stack, int power) {
		GLItems.SPIRITUAL_POWER.set(stack, Mth.clamp(power, 0, max(stack)));
	}

	/**
	 * Adds {@code delta} clamped to the pool and returns the amount actually accepted.
	 */
	public static int add(ItemStack stack, int delta) {
		int old = get(stack);
		int next = Mth.clamp(old + delta, 0, max(stack));
		set(stack, next);
		return next - old;
	}

	public static void appendHoverText(ItemStack stack, List<Component> list) {
		list.add(GLLang.Spiritual.BAR.get(get(stack), max(stack)));
	}

	public static boolean barVisible(ItemStack stack) {
		if (stack.has(GLItems.ICON.get()))
			return false;
		return max(stack) > 0;
	}

	public static int barWidth(ItemStack stack) {
		int max = max(stack);
		if (max <= 0) return 0;
		return Mth.clamp(Math.round(13.0F * get(stack) / max), 0, 13);
	}

	/**
	 * Feeds {@code power} into the spiritual items the player carries, as evenly as the priority
	 * order allows: main hand, off hand, equipment, hotbar, then the rest of the inventory.
	 *
	 * @return the power actually stored
	 */
	public static int restore(Player player, int power) {
		if (power <= 0) return 0;
		List<ItemStack> targets = carriers(player);
		if (targets.isEmpty()) return 0;
		int n = targets.size();
		int share = power / n;
		if (share == 0) {
			int given = add(targets.getFirst(), 1);
			List<ItemStack> rest = new ArrayList<>(targets.subList(1, n));
			var random = player.getRandom();
			for (int i = 0; i < power - 1 && !rest.isEmpty(); i++) {
				given += add(rest.remove(random.nextInt(rest.size())), 1);
			}
			return given;
		}
		int given = 0;
		for (ItemStack stack : targets) {
			given += add(stack, share);
		}
		if (given < power) {
			given += add(targets.getFirst(), power - given);
			for (int i = 1; i < n && given < power; i++) {
				given += add(targets.get(i), power - given);
			}
		}
		return given;
	}

	/**
	 * The carrier stacks with room left, in the order they are served: hands, equipment, hotbar,
	 * then inventory.
	 */
	private static List<ItemStack> carriers(Player player) {
		List<ItemStack> ans = new ArrayList<>();
		collect(ans, player.getMainHandItem());
		collect(ans, player.getOffhandItem());
		for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
				EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			collect(ans, player.getItemBySlot(slot));
		}
		for (ItemStack stack : player.getInventory().items) {
			collect(ans, stack);
		}
		return ans;
	}

	private static void collect(List<ItemStack> list, ItemStack stack) {
		if (max(stack) <= 0 || get(stack) >= max(stack)) return;
		for (ItemStack other : list) {
			if (other == stack) return;
		}
		list.add(stack);
	}

}
